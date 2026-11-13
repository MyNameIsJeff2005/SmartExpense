package com.example.smartexpense.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.Locale
import kotlin.math.min as kotlinMin

/**
 * Thời lượng chuẩn cho animation biểu đồ (bản 1.3.1)
 */
private const val CHART_ANIM_MS = 600L

/**
 * Biểu đồ tròn phẳng (mục 5.5): vẽ bằng drawArc trong onDraw, KHÔNG lỗ giữa,
 * các cung nối tiếp nhau lấp đầy 360° (không còn khoảng trống giữa các mảnh).
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Slice(val color: Int, val weight: Float) // weight: 0..1

    private var slices: List<Slice> = emptyList()

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.GRAY
    }

    /** Tiến độ vẽ 0..1: các cung quét dần từ 12 giờ (animation 1.3.1) */
    private var progress = 1f

    private var animator: ValueAnimator? = null

    fun setData(data: List<Slice>) {
        slices = data
        animateProgress()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = slices.sumOf { it.weight.toDouble() }
        if (total <= 0.0) return

        val size = kotlinMin(width, height).toFloat() * 0.96f
        val cx = width / 2f
        val cy = height / 2f
        val radius = size / 2f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        var startAngle = -90f // bắt đầu từ 12h
        slices.forEach { slice ->
            val sweep = (slice.weight / total * 360.0).toFloat() * progress
            paint.color = slice.color
            canvas.drawArc(rect, startAngle, sweep, true, paint)
            startAngle += (slice.weight / total * 360.0).toFloat()
        }
    }

    /** Chạy lại hiệu ứng quét 360° mỗi khi dữ liệu thay đổi. */
    private fun animateProgress() {
        animator?.cancel()
        progress = if (isInEditMode || slices.isEmpty()) 1f else 0f
        if (progress == 1f) {
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CHART_ANIM_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}

/**
 * Biểu đồ cột phẳng "Người dùng mới 7 ngày" (mục 6.2): cột bo góc trên,
 * 6 cột xanh nhạt + cột cuối xanh đậm, nhãn ngày 11sp.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Bar(val value: Float, val color: Int)

    private var bars: List<Bar> = emptyList()
    private var labels: List<String> = emptyList()

    private val barPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#6B7280")
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }

    fun setData(data: List<Bar>, dayLabels: List<String>) {
        bars = data
        labels = dayLabels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return
        val labelHeight = if (labels.isNotEmpty()) sp(16f) else 0f
        val chartBottom = height - labelHeight - dp(4f)
        val chartTop = dp(8f)
        val chartHeight = chartBottom - chartTop
        val maxValue = bars.maxOf { it.value }.coerceAtLeast(1f)

        val gap = dp(8f)
        val barWidth = ((width - gap * (bars.size + 1)) / bars.size)

        bars.forEachIndexed { index, bar ->
            val left = gap + index * (barWidth + gap)
            val valueHeight = (bar.value / maxValue * chartHeight).coerceAtLeast(dp(2f))
            val top = chartBottom - valueHeight
            barPaint.color = bar.color
            canvas.drawPath(roundedTopRect(left, top, barWidth, valueHeight), barPaint)

            if (labels.getOrNull(index) != null && labelHeight > 0f) {
                val label = labels[index]
                canvas.drawText(label, left + barWidth / 2f, height - dp(2f), textPaint)
            }
        }
    }

    private fun roundedTopRect(left: Float, top: Float, width: Float, height: Float) =
        android.graphics.Path().apply {
            val r = (width / 3f).coerceAtMost(height)
            moveTo(left, top + height)
            lineTo(left, top + r)
            arcTo(RectF(left, top, left + 2 * r, top + 2 * r), 180f, 90f)
            lineTo(left + width - r, top)
            arcTo(RectF(left + width - 2 * r, top, left + width, top + 2 * r), 270f, 90f)
            lineTo(left + width, top + height)
            close()
        }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}

/**
 * Biểu đồ cột dọc xếp chồng (mục 5.5): một thanh bo tròn hai đầu, các phần
 * xếp từ trên xuống theo tỉ lệ chi tiêu từng danh mục. Dùng khi người dùng
 * chuyển từ biểu đồ tròn sang biểu đồ cột dọc ở tab Thống kê.
 */
class StackedBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Segment(val color: Int, val weight: Float) // weight: 0..1

    private var segments: List<Segment> = emptyList()

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    /** Tiến độ đổ đầy 0..1: các phần trải dần từ trên xuống (animation 1.3.1) */
    private var progress = 1f

    private var animator: ValueAnimator? = null

    fun setData(data: List<Segment>) {
        segments = data
        animateProgress()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = segments.sumOf { it.weight.toDouble() }
        if (total <= 0.0) return

        val top = dp(4f)
        val bottom = height - dp(4f)
        val fullHeight = bottom - top
        if (fullHeight <= 0f) return

        // Đổ đầy lấp dần tới `fillBottom` theo tiến độ animation
        val fillBottom = top + fullHeight * progress

        val barWidth = kotlinMin(width * 0.55f, dp(64f))
        val left = (width - barWidth) / 2f
        val radius = barWidth / 2f
        val clip = Path().apply {
            addRoundRect(
                RectF(left, top, left + barWidth, fillBottom),
                radius.coerceAtMost((fillBottom - top) / 2f),
                radius.coerceAtMost((fillBottom - top) / 2f),
                Path.Direction.CW
            )
        }

        canvas.save()
        canvas.clipPath(clip)
        var y = top
        segments.forEachIndexed { index, segment ->
            if (y >= fillBottom) return@forEachIndexed
            // Phần cuối luôn kéo tới mốc đổ đầy để thanh không bị hụt do làm tròn số.
            val segmentHeight = if (index == segments.lastIndex) {
                fillBottom - y
            } else {
                (segment.weight / total * (fillBottom - top)).toFloat()
                    .coerceAtMost(fillBottom - y)
            }
            paint.color = segment.color
            canvas.drawRect(left, y, left + barWidth, y + segmentHeight, paint)
            y += segmentHeight
        }
        canvas.restore()
    }

    /** Chạy lại hiệu ứng đổ đầy từ trên xuống mỗi khi dữ liệu thay đổi. */
    private fun animateProgress() {
        animator?.cancel()
        progress = if (isInEditMode || segments.isEmpty()) 1f else 0f
        if (progress == 1f) {
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = CHART_ANIM_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
