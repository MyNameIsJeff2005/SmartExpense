"""Sinh icon launcher tu icon.json (trich tu icon/icon_launcher.fig).

Chay: python tools/gen_launcher_icon.py
- Doc icon.json (cung thu muc voi script, sinh boi scripts/build_icon.mjs)
- Raster hoa toan bo shape (nonzero winding, aa supersampling) -> PNG mipmap-*dpi
- Nen icon ugula calisiyor: ic_launcher.png = bo icon full (bo icon goc ma vien cong thanh trong)
  ic_launcher_round.png = cat theo duong tron
"""
import json
import math
import os
import struct

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
ICON_JSON = os.path.join(ROOT, "tools", "icon.json")

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Ma mau gradient nen (da phen 0.2 moi nen trang) - doc tu fig: dua theo trục Y
GRAD_STOPS = [
    (0.000000, (0xCC, 0xDC, 0xFF)),
    (0.254808, (0xCC, 0xDC, 0xFF)),
    (0.456731, (0xD2, 0xD2, 0xF8)),
    (0.629808, (0xD6, 0xDF, 0xF3)),
    (0.870192, (0xCE, 0xE3, 0xFD)),
    (1.000000, (0xCE, 0xE3, 0xFD)),
]


def grad_color(t):
    """Truyen thang mau gradient theo t trong [0,1]."""
    t = min(1.0, max(0.0, t))
    for i in range(len(GRAD_STOPS) - 1):
        p0, c0 = GRAD_STOPS[i]
        p1, c1 = GRAD_STOPS[i + 1]
        if p0 <= t <= p1:
            f = 0.0 if p1 <= p0 else (t - p0) / (p1 - p0)
            return tuple(int(round(c0[k] + (c1[k] - c0[k]) * f)) for k in range(3))
    return GRAD_STOPS[-1][1]


# ---------- parser path SVG (M/L/C) ----------
NUM = "0123456789-."


def parse_commands(d):
    """Doc chuoi path: khong dau cach dieu khien, moi lenh M/L/C/Z doc du so."""
    cmds = []
    i = 0
    n = len(d)

    def read_num():
        nonlocal i
        while i < n and d[i] in " ,":
            i += 1
        start = i
        while i < n and d[i] in NUM:
            i += 1
        if i == start:
            raise ValueError("khong doc duoc so tai %d sau lenh: %s" % (start, d[:20]))
        return float(d[start:i])

    while i < n:
        c = d[i]
        i += 1
        if c == "Z":
            cmds.append((c, []))
        elif c in "ML":
            cmds.append((c, [(read_num(), read_num())]))
        elif c == "C":
            cmds.append((c, [(read_num(), read_num()) for _ in range(3)]))
        # ky tu khac (khoang trang) -> bo qua
    return cmds


def cubic(p0, p1, p2, p3, steps=24):
    out = []
    for k in range(1, steps + 1):
        t = k / steps
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        out.append((x, y))
    return out


def to_segments(path_d):
    """chuyen path thanh danh sach canh mach (a,b) de scanline."""
    cmds = parse_commands(path_d)
    cur = (0.0, 0.0)
    start = (0.0, 0.0)
    edges = []

    def line_to(p):
        nonlocal cur
        if cur[1] != p[1]:
            edges.append((cur[0], cur[1], p[0], p[1]))
        cur = p

    for c, pts in cmds:
        if c == "M":
            p = pts[0]
            cur = p
            start = p
        elif c == "L":
            p = pts[0]
            line_to(p)
        elif c == "C":
            p1, p2, p3 = pts
            for p in cubic(cur, p1, p2, p3):
                line_to(p)
        elif c == "Z":
            if cur != start:
                line_to(start)
            cur = start
    return edges


# ---------- raster nonzero ----------
def raster(edges, w, h):
    """Tra ve mask L (0/255) theo quy tac nonzero winding."""
    mask = bytearray(w * h)
    for y in range(h):
        yc = y + 0.5
        xs = []
        for ax, ay, bx, by in edges:
            if (ay <= yc < by) or (by <= yc < ay):
                t = (yc - ay) / (by - ay)
                xs.append((ax + t * (bx - ax), 1 if by > ay else -1))
        if not xs:
            continue
        xs.sort()
        wind = 0
        row = y * w
        for k in range(len(xs) - 1):
            wind += xs[k][1]
            if wind != 0:
                xa = max(0, int(math.ceil(xs[k][0] - 0.5)))
                xb = min(w - 1, int(math.floor(xs[k + 1][0] - 0.5)))
                if xb >= xa:
                    mask[row + xa : row + xb + 1] = b"\xff" * (xb - xa + 1)
    return mask


def circle_mask(w, h):
    m = bytearray(w * h)
    cx = cy = w / 2
    r = w / 2
    for y in range(h):
        for x in range(w):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            if dx * dx + dy * dy <= r * r:
                m[y * w + x] = 255
    return m


def hex_color(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


class Shape:
    pass


def path_with(path_d, key):
    return path_d  # toa do da la gia tri tuyet doi trong icon.json


def render_icon(size, ss=4, round_icon=False):
    """RGBA kich thuoc size; full thiet ke included squircle nen."""
    data = json.load(open(ICON_JSON, encoding="utf-8"))
    W = size * ss
    canvas = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    scale = W / data["size"]

    for sh in data["shapes"]:
        edges = [(x * scale, y * scale, x2 * scale, y2 * scale)
                 for (x, y, x2, y2) in to_segments(sh["paths"][0]["d"])]
        mask = raster(edges, W, W)
        fills = sh["fills"]
        grad = next((f for f in fills if f["kind"] == "grad"), None)
        solid = next((f for f in fills if f["kind"] == "solid"), None)

        layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        px = layer.load()
        if grad is not None:
            base = grad["opacity"]
            for y in range(W):
                c = grad_color(y / (W - 1))
                row = y * W
                for x in range(W):
                    if mask[row + x]:
                        # phen mau gradient tren nen trang truoc phen Figma
                        # (thiet ke goc: la mau lai ma moi nen trang can lai)
                        r, g, b = (c[k] * base + 255 * (1 - base) for k in range(3))
                        px[x, y] = (int(r), int(g), int(b), 255)
        elif solid is not None:
            color = (int(solid["color"]["r"] * 255), int(solid["color"]["g"] * 255), int(solid["color"]["b"] * 255))
            layer = Image.new("RGBA", (W, W), color + (0,))
            alpha = Image.frombytes("L", (W, W), bytes(mask))
            layer.putalpha(alpha)

        canvas.alpha_composite(layer)

    if round_icon:
        m = circle_mask(W, W)
        alpha = Image.frombytes("L", (W, W), bytes(m))
        out = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        canvas_im = canvas.copy()
        canvas_im.putalpha(alpha)
        out.alpha_composite(canvas_im)
        canvas = out

    return canvas.resize((size, size), Image.LANCZOS)


def main():
    data = json.load(open(ICON_JSON, encoding="utf-8"))
    print("shapes:", [s["name"] for s in data["shapes"]])

    for dpi, size in DENSITIES.items():
        out_dir = os.path.join(RES, "mipmap-" + dpi)
        os.makedirs(out_dir, exist_ok=True)
        full = render_icon(size)
        full.save(os.path.join(out_dir, "ic_launcher.png"), "PNG", optimize=True)
        rounded = render_icon(size, round_icon=True)
        rounded.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG", optimize=True)
        print("wrote", dpi, size)

    preview = render_icon(512)
    out = r"C:\Users\Thanh\AppData\Local\Temp\icon_new_preview.png"
    preview.save(out)
    print("preview:", out)


if __name__ == "__main__":
    main()
