# SmartExpense

**SmartExpense — Personal Finance Manager**, app Android quản lý chi tiêu cá nhân, hoạt động offline-first. Ghi thu/chi theo danh mục, xem số dư từng tháng, biểu đồ thống kê, hạn mức, sao lưu/khôi phục file CSV, kèm màn hình Quản trị cho admin và bảng điều khiển server Firebase. Toàn bộ dữ liệu giao dịch nằm trong Room ngay trên máy; hồ sơ tài khoản đồng bộ lên Firestore khi có mạng.

## Important

🤖 **Dự án được viết với sự hỗ trợ của AI**

Phần lớn code, bản dịch chuỗi và các bản sửa lỗi được viết cùng AI (OpenCode). Chủ repo xem lại từng thay đổi và tự thử trong app trên một emulator Pixel 7 Pro (Android 16). AI cũng tự kiểm tra công việc của mình bằng cách điều khiển trực tiếp emulator đó: dump cây giao diện (`uiautomator`), đọc logcat và chạy các phiên thử thật — kể cả bật **Chế độ máy bay** để kiểm tra chế độ offline. Code có thể đọc như được một máy viết rất nhanh — vì thực tế là như vậy. Báo lỗi vẫn luôn được chào đón.

## Status

App **hoạt động đầy đủ** cho một người dùng: đăng ký/đăng nhập/quên mật khẩu qua Firebase, CRUD giao dịch, danh mục, hạn mức, thống kê, tỷ giá USD→VND, sao lưu/khôi phục và toàn bộ phần Quản trị đã được xác nhận chạy trong app, hỗ trợ **Tiếng Việt + English** (đổi ngay trong app) và **chế độ sáng/tối**. Bản hiện tại: **1.4 (versionCode 10)**, `minSdk 24`, `targetSdk 36`.

## Quick start (Android Studio hoặc thiết bị thật)

Mọi thứ chạy trên chính app — không cần server riêng ngoài Firebase. Trên thiết bị thật hoặc emulator đều được (app chạy tốt trên API 24+; đã thử nghiệm trên emulator Pixel 7 Pro).

1. Cần **Android Studio** (kèm SDK 37) và một **project Firebase** — xem hướng dẫn từng bước trong [`API_SETUP.md`](API_SETUP.md) mục 1.
2. Clone repo này, mở bằng Android Studio và chờ Gradle sync (danh sách dependency nằm trong `gradle/libs.versions.toml`).
3. Tải **`google-services.json`** từ Firebase Console và chép đè lên `app/google-services.json` (file trong repo là file giả lập để build được — đăng nhập sẽ lỗi cho tới khi thay).
4. *(Tùy chọn, để có tính năng admin)* Mở `local.properties` và thêm:
   ```
   admin.emails=email-cua-ban@gmail.com
   ```
   (nhiều email phân tách bằng dấu phẩy; email này phải trùng email đăng ký trong app).
5. Bấm **Run**. Ở màn Đăng nhập, bấm **Sign up** để tạo tài khoản mới, hoặc **Run DEMO (built-in sample data)** để xem nhanh với dữ liệu mẫu nội bộ mà không cần tài khoản.
6. Bắt đầu ghi giao dịch: nút **+** ở tab Transactions.

Không cần quyền đặc biệt: quyền lưu trữ đi qua **Storage Access Framework** (trình chọn file/thư mục của Android) khi sao lưu/khôi phục, app không đòi quyền bộ nhớ toàn cục.

## Các cách chạy khác

Quick start ở trên là setup 1 và đủ để dùng app. Các chế độ khác:

| Setup | Backend | Dành cho |
|---|---|---|
| 1. Tài khoản Firebase | Room + Firestore | dùng thật, hồ sơ theo dõi thiết bị (mặc định) |
| 2. DEMO mode | Room, dữ liệu mẫu nội bộ | xem nhanh app không cần đăng ký |
| 3. Tài khoản admin | Room + Firestore + CSV | khóa/mở khóa tài khoản khách hàng, nhật ký, quản lý CSV |

Setup 3 chỉ cần thêm `admin.emails` vào `local.properties` rồi cài lại app (xem bước 4 ở trên).

## Features

Tất cả các mục dưới đây chạy ngay trong app; cách dùng chi tiết nằm trong [`API_SETUP.md`](API_SETUP.md).

**Quản lý giao dịch** *(mục 5.4–5.6 tài liệu kỹ thuật)*

- Thêm/sửa/xóa thu chi: số tiền có định dạng khi gõ, 8 danh mục mặc định (icon + màu riêng), ghi chú, ngày giờ, có thể gắn cờ cảnh báo.
- Danh sách nhóm theo ngày: tiêu đề ngày + tổng thu/chi từng ngày; lưới lịch 7 cột đánh dấu ngày có thu (chấm xanh) / có chi (chấm đỏ), chọn ngày để lọc.
- **Hạn mức chi tiêu tháng**: đặt hạn mức, app cảnh báo khi vượt.

**Trang chủ & Thống kê**

- Số dư, tổng thu, tổng chi của tháng + giao dịch gần đây; bộ chọn tháng ◀ ▶.
- Biểu đồ tròn **hoặc** cột dọc xếp chồng theo danh mục, chú giải kèm phần trăm; chuyển kiểu biểu đồ ngay tại tab và app nhớ lựa chọn.

**Tỷ giá USD→VND** *(phân hệ JSON online — bản 1.4)*

- Kéo JSON từ `open.er-api.com/v6/latest/USD` bằng **Retrofit + Coroutines** (`Dispatchers.IO`), hiển thị ở tab Cài đặt.
- Quản lý 3 trạng thái UI bằng sealed class **`Resource`**: **Loading** (ProgressBar) → **Success** (giá trị + thời điểm cập nhật) → **Error** (thông báo lỗi + nút thử lại).
- Cache vào **Room**: bật **Chế độ máy bay** vẫn xem được tỷ giá lần gần nhất; bấm dòng để tải lại.

**Cài đặt & cá nhân hóa**

- **Ngôn ngữ**: Theo hệ thống / Tiếng Việt / English — đổi ngay trong app, tự tạo lại Activity (AppCompat per-app locale).
- **Chế độ tối**: công tắc bật/tắt nhanh, hộp thoại chọn "Theo hệ thống / Sáng / Tối", nhớ lựa chọn.
- **Nhật ký thay đổi**: màn dòng thời gian các phiên bản (chip `SmartExpense • vX`, nhãn "Đã cài", thẻ New/Fix/Removes, nút "Hiển thị tất cả (N)").

**Dữ liệu** *(bản 1.2.2+)*

- **Offline-first**: Room là nguồn chính, mọi thao tác không cần mạng; Firestore chỉ đồng bộ hồ sơ.
- **Đồng bộ hồ sơ với Firebase**: đẩy/kéo hồ sơ giữa Room ↔ Firestore, hiển thị "lần đồng bộ gần nhất".
- **Sao lưu / Khôi phục**: ghi file CSV qua SAF (không cần quyền bộ nhớ), khôi phục gộp vào Room; **tự động sao lưu** mỗi khi phát hiện phiên bản mới; **đường dẫn sao lưu** tùy chọn.

**Quản trị** *(chỉ admin — mục 6)*

- Dashboard số liệu, danh sách người dùng (tìm theo tên/email), Chi tiết người dùng: **khóa / mở khóa / đặt lại mật khẩu**.
- Giao dịch minh họa với bộ lọc Tất cả / Thu / Chi / Cảnh báo; **Nhật ký hoạt động** ghi lại mọi hành động admin.
- **Quản lý file CSV** (xuất/nhập/dựng lại/xóa) và **Quản lý danh mục** (thêm/sửa, không có xóa).

**Kiến trúc** *(đạt yêu cầu kỹ thuật v1.4)*

- **View Binding 100%** — không còn bất kỳ `findViewById` nào trong Activity/Fragment/ViewHolder.
- **MVVM**: mọi logic nằm ở ViewModel (`viewmodel/`), dữ liệu bọc trong `StateFlow`/`LiveData` nên không mất khi xoay màn hình.
- **Cấu trúc package chuẩn**: `ui/`, `viewmodel/`, `model/`, `repository/`, `adapter/` (+ `data/local` Room, `data/remote` Retrofit).
- **Room Singleton** (`@Volatile` + `synchronized`), DAO viết `suspend fun` cho ghi và `Flow<List<T>>` cho đọc; schema v2 có migration tự động.
- Tách chuỗi song ngữ hoàn toàn qua `values/` + `values-en/`.

## Đã xác nhận hoạt động

Đã thử trong app: đăng ký/đăng nhập/quên mật khẩu, sửa tên & ảnh đại diện, xóa tài khoản (Room + Firestore + Firebase Auth), CRUD giao dịch, hạn mức, biểu đồ tròn/cột, lịch và lọc theo ngày, tỷ giá online + offline cache, đổi ngôn ngữ, chế độ tối, nhật ký thay đổi, sao lưu/khôi phục/tự động sao lưu, đồng bộ Firestore, xuất/nhập CSV và toàn bộ tab Quản trị. App chạy đúng trên Android 16 (emulator Pixel 7 Pro) và được thiết kế cho API 24 trở lên.

## Chưa hoạt động / giới hạn

- **Dùng chung nhiều người thật**: app chỉ quản lý chi tiêu cá nhân; admin xem được danh sách tài khoản nhưng không có tương tác thời gian thực giữa các người dùng.
- **Đổi cặp tiền tệ**: mọi số tiền hiển thị **VND (₫)**; tỷ giá USD→VND chỉ là tham khảo, không quy đổi dữ liệu.
- **Xóa danh mục**: quản lý danh mục chỉ thêm/sửa (cố ý — tránh rớt dữ liệu giao dịch cũ).
- **Đăng ký/đăng nhập lần đầu cần mạng** (Firebase Auth); sau đó app dùng offline trọn vẹn.
- **Danh mục đã seed không tự dịch** khi đổi ngôn ngữ (dữ liệu người dùng) — xóa dữ liệu app để seed lại.

## FAQ

**App có cần internet không?** Chỉ lúc đăng ký/đăng nhập, đồng bộ hồ sơ và tải tỷ giá. Giao dịch và mọi tính năng khác dùng offline.

**Dữ liệu của tôi nằm ở đâu?** Giao dịch trong Room (`smartexpense.db`) trên máy. Hồ sơ tài khoản được cache trong Room và đồng bộ lên Firestore (collection `users`). Sao lưu CSV nằm ở thư mục bạn chọn qua SAF, mặc định `Android/data/com.example.smartexpense/files/backups/`.

**Tỷ giá không tải được?** Kiểm tra mạng rồi bấm chính dòng *Tỷ giá USD→VND* để thử lại. Nếu Room đã có cache, app vẫn hiển thị giá trị cũ kèm thời điểm cập nhật.

**Làm sao tôi thành admin?** Thêm email của bạn vào `admin.emails` trong `local.properties` trước khi build, rồi đăng ký đúng email đó trong app.

**Quên mật khẩu?** Dùng *Forgot password?* ở màn Đăng nhập — Firebase gửi email đặt lại.

**Đổi ngôn ngữ / chế độ tối ở đâu?** Tab **Cài đặt** (mọi người dùng) hoặc **Quản trị → Cài đặt** (admin). Cả hai tự nhớ lựa chọn.

**Đăng nhập xong thấy màn hình trống?** Chưa có giao dịch nào trong tháng đang xem. Thử bấm **▶** để sang tháng trước (nếu có dữ liệu demo) hoặc thêm giao dịch mới bằng nút **+**.

## More

- [`API_SETUP.md`](API_SETUP.md): cấu hình Firebase từng bước, Firestore + Rules, tỷ giá USD→VND (Retrofit + Room + 3 trạng thái), CSV, admin, đa ngôn ngữ và icon ứng dụng.
- Nhật ký thay đổi đầy đủ **ngay trong app**: Cài đặt → *Xem nhật ký thay đổi* (nội dung từ `ui/changelog/ChangelogData.kt`).
- Bản dựng kiểm thử: `gradlew assembleDebug` — APK ở `app/build/outputs/apk/debug/`.

## Special thanks

- **Firebase** (Auth + Firestore) — xác thực và đồng bộ hồ sơ.
- **AndroidX / Material** (Room, ViewModel, Navigation, Material Design) — nền tảng của toàn bộ app.
- **Retrofit & Gson** — đọc JSON tỷ giá.
- **open.er-api.com** — API tỷ giá miễn phí, không cần key.
- **Flaticon & các bộ icon vector** — icon danh mục và giao diện.

## License

Đồ án môn học **Lập trình ứng dụng di động**. Mã nguồn chia sẻ cho mục đích học tập và tham khảo; không phát hành thương mại. Nếu bạn tái sử dụng một phần code, ghi rõ nguồn repo này.
