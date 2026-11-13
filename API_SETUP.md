# Hướng dẫn cấu hình API cho SmartExpense

## 1. Firebase (bắt buộc — để đăng nhập/đăng kí hoạt động)

### Bước 1: Tạo project
1. Mở https://console.firebase.google.com → **Add project** → đặt tên (vd: `smartexpense`) → Create.
2. (Không cần Google Analytics — có thể tắt.)

### Bước 2: Thêm app Android
1. Trong project → biểu tượng **Android** → điền đúng:
   - **Package name**: `com.example.smartexpense`
   - App nickname: `SmartExpense`
   - SHA-1: bỏ trống (Email/Password không cần)
2. **Register app** → tải file **`google-services.json`** xuống.

### Bước 3: Thay file placeholder
- Copy file vừa tải vào **thay đè**file: `app/google-services.json`
- (File hiện tại trong project là file GIẢ LẬP để build được — đăng nhập sẽ lỗi cho tới khi thay.)

### Bước 4: Bật Email/Password login
1. Menu **Build → Authentication → Get started**.
2. Tab **Sign-in method** → chọn **Email/Password** → Enable → Save.

### Bước 5: Chạy lại
- Sync lại project (nếu Android Studio đang mở) rồi **Run**.
- Đăng kí 1 tài khoản mới → app tự ghi vào `accounts.csv` và Room.

## 2. Tiền tệ (mặc định VND)

Từ bản **1.2 (3)**, app bỏ thẻ tỷ giá động và không cho đổi cặp tiền tệ nữa:
mọi khoản thu/chi hiển thị **VND (₫)** định dạng `1.234.567 ₫`
(xem `utils/CurrencyUtils.kt`).

> API Frankfurter trước đây không hỗ trợ VND, nên tính năng tỷ giá đã được
> gỡ bỏ cùng với `ExchangeRateRepository`, `FrankfurterService`,
> `ExchangeRateResponse` và hộp thoại `dialog_currency`.

### 2.1. Tỷ giá USD→VND (bản 1.4 — phân hệ JSON online)

Từ bản **1.4**, tỷ giá quay lại dưới dạng **phân hệ đọc JSON online** (dùng cho
yêu cầu kỹ thuật Retrofit + Room + 3 trạng thái Resource), không ảnh hưởng cách
định dạng tiền trong app:

| Thành phần | File | Vai trò |
|---|---|---|
| API | `https://open.er-api.com/v6/latest/USD` | Miễn phí, **không cần key**. JSON: `{"result":"success","base_code":"USD","rates":{"VND":25540.0,...}}` |
| Retrofit service | `data/remote/exchange/ExchangeRateApi.kt` | `@GET("v6/latest/{base}") suspend fun latest(...)` — coroutine trên `Dispatchers.IO` |
| DTO | `model/ExchangeRateResponse.kt` | `result`, `base_code`, `rates: Map<String, Double>` |
| Entity/DAO | `model/ExchangeRateEntity.kt` + `data/local/dao/ExchangeRateDao.kt` | Bảng `exchange_rates` (Room v2, có `MIGRATION_1_2`); DAO dùng `@Upsert` suspend + `Flow` khi đọc |
| Repository | `repository/ExchangeRateRepository.kt` | Gọi API → `upsertAll` cache Room; lỗi mạng → đọc cache (Success dữ liệu cũ) hoặc `Resource.Error` |
| ViewModel | `viewmodel/RatesViewModel.kt` | `StateFlow<Resource<ExchangeRateEntity>>`: **Loading** (ProgressBar) → **Success** (nạp dữ liệu) → **Error** (thông báo + thử lại) |
| UI | **Cài đặt → Tỷ giá USD→VND** | Bấm dòng để tải lại; ngắt mạng vẫn xem được tỷ giá lần cuối từ Room |

Test offline: bật **Chế độ Máy bay** trên AVD rồi mở lại màn Cài đặt — dòng tỷ giá
hiển thị giá trị đã lưu kèm ghi chú *ngoại tuyến*; nếu Room chưa có cache thì hiện
*Lỗi mạng – bấm để thử lại*.

## 3. Tài khoản Admin (tùy chọn)

Mở file `local.properties`, điền email (phải trùng email bạn đăng kí trong app):

```
admin.emails=email-cua-ban@gmail.com
```

Nhiều email phân tách bằng dấu phẩy. Sau đó Sync + cài lại app.
Vào phần Quản trị từ tab **Cài đặt** (dòng **Quản trị** chỉ hiện với tài khoản admin).

---

## 4. Cơ sở dữ liệu hồ sơ trên Firebase (bản 1.3.0)

App dùng **offline-first**: Room là nguồn dữ liệu chính, Firebase chỉ đồng bộ thêm.
Từ bản **1.3.0**, hồ sơ người dùng nằm trong **Firebase Firestore** (collection
`users`) thay cho server REST MockAPI của các bản 1.0–1.2, nên đăng nhập/đăng ký
hoạt động ngay sau khi có `app/google-services.json` — không cần server riêng.

### 4.1. Firebase Auth — đăng nhập / đăng ký (3.1)

- App gọi `createUserWithEmailAndPassword` / `signInWithEmailAndPassword`
  (`data/repository/AuthRepository.kt`), mật khẩu do Firebase quản lý, app không lưu.
- **Firebase Console** (project hiện tại: `smartexpense-58538`) →
  *Authentication → Sign-in method → bật **Email/Password***.
- Lỗi được chuyển sang tiếng Việt: `AUTH/…` (`EMAIL_EXISTS`, `WEAK_PASSWORD`,
  `WRONG_CREDENTIALS`) — xem `Messages` và `mapFirebaseError()`.
- *Quên mật khẩu*: `sendPasswordResetEmail` gửi email đặt lại qua Firebase.

### 4.2. Firestore — cơ sở dữ liệu hồ sơ (3.2)

Định dạng document (collection `users`, id chính là UID của Firebase Auth):

```json
{
  "uid": "firebase-uid",
  "displayName": "Nguyen Van A",
  "email": "a@example.com",
  "createdAt": 1761739200000,
  "role": "USER",
  "status": "ACTIVE",
  "lastLoginAt": 1761739200000
}
```

> Không có mật khẩu/token trong dữ liệu này. Firebase Auth vẫn là nơi xác thực.

Từ bản 1.3.0, mọi luồng hồ sơ đều ghi Firestore (`data/remote/FirestoreUserRepository.kt`):

| Sự kiện | Hành động trên Firestore |
|---|---|
| **Đăng ký** | `users/{uid}` tạo mới |
| **Đăng nhập mới trên máy** | cập nhật `lastLoginAt`; nếu Room trống thì cũng khôi phục hồ sơ từ Firestore |
| **Đổi tên ở màn Tài khoản** | cập nhật `displayName` |
| **Thủ công** (Cài đặt → *Đồng bộ hồ sơ với Firebase*) | đẩy/kéo hồ sơ giữa Room ↔ Firestore |

- Sau mỗi lần đồng bộ, `lastSyncAt` được lưu để hiển thị "Lần đồng bộ gần nhất" trong
  **Cài đặt** và **Quản trị → Cài đặt**.
- **Quản trị → Cài đặt → Đồng bộ dữ liệu**: chọn *Tải hồ sơ từ Firestore về máy* hoặc
  *Đẩy dữ liệu máy lên Firestore*; dòng *Cơ sở dữ liệu đám mây* cho biết project đang
  dùng (vd `Firestore · smartexpense-58538`).
- Quyền/khóa của máy (role, status) được giữ khi gộp — Firestore không tự nâng admin.

### 4.3. Rules Firestore tối thiểu (Firebase Console → Firestore → Rules)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null
        && request.auth.uid == uid
        && !request.resource.data.diff(resource.data).affectedKeys()
             .hasAny(['role', 'status']);
    }
  }
}
```

> Rules trên là mức "đủ dùng cho đồ án": người dùng chỉ ghi được hồ sơ của chính mình
> và không tự sửa `role`/`status` từ client. Bảo mật thật cần backend/Admin SDK.

### 4.4. Google Play services trên trình giả lập

- AVD phải có Google Play (hình ảnh *Google APIs / Google Play*), thiết bị thật cài
  Google Play Services là chạy được ngay — SDK Firestore tự cấu hình nhờ
  `app/google-services.json` (plugin `com.google.gms.google-services`).

---

## 5. Xuất / nhập CSV tài khoản khách hàng (admin)

Vào **Quản trị → Cài đặt → Quản lý file CSV**:

- **Xuất CSV ra file…**: Android mở trình chọn nơi lưu của Android (Drive, Downloads, USB…),
  ghi toàn bộ hồ sơ đang có trong Room ra file `.csv`. Không cần quyền bộ nhớ.
- **Nhập CSV từ file…**: chọn file `.csv` bất kỳ. App sẽ:
  - đọc đúng chuẩn CSV (có hỗ trợ ô chứa dấu phẩy, ngoặc kép, xuống dòng);
  - `uid` đã có → cập nhật tên/email/ngày tạo;
  - `uid` mới → thêm hồ sơ (role USER, trạng thái ACTIVE);
  - dòng thiếu `uid` → bỏ qua;
  - sau đó dựng lại `accounts.csv` và ghi nhật ký `IMPORT_CSV` / `EXPORT_CSV`.

**Thông báo cấp quyền dữ liệu (mục 3.2c):** trước khi app ghi/đọc tệp, app hiển thị
hộp thoại *Cấp quyền dữ liệu* giải thích cơ chế Storage Access Framework: app làm
việc với **My Files** của điện thoại thông qua trình chọn tệp/thư mục, chỉ đọc/ghi
đúng nơi bạn chọn và **không cần** quyền bộ nhớ toàn cục. Bấm *Tiếp tục* mới mở
trình chọn. Từ bản **1.2.2 (5)**, thông báo này xuất hiện trước **Sao lưu** và
**Khôi phục** trong *Cài đặt → Dữ liệu*.

Định dạng chuẩn (xem thêm `app/src/main/assets/accounts_template.csv`):

```
uid,display_name,email,created_at
abc123,Nguyen Van A,a@example.com,2026-10-29T17:00:00+07:00
```

---

## 6. Giao diện sáng/tối và phiên bản app

- **Cài đặt → Chế độ tối**: bật/tắt nhanh bằng công tắc; bấm cả dòng để chọn
  *Theo hệ thống / Sáng / Tối* (admin cũng có trong **Quản trị → Cài đặt**).
  Lựa chọn được lưu vào EncryptedSharedPreferences và áp dụng ngay bằng
  `AppCompatDelegate.setDefaultNightMode`.
- **Phiên bản ứng dụng** hiển thị `versionName (versionCode)` lấy trực tiếp từ `BuildConfig`;
  bản hiện tại là **1.4 (10)**, tăng `versionCode`/`versionName` trong `app/build.gradle.kts` là UI tự đổi.

### 6.1. Tab Cài đặt & màn Tài khoản (bản 1.2.1)

Tab thứ 4 của Bottom Navigation đổi từ *Profile* thành **Cài đặt / Settings**,
gồm 4 nhóm:

| Nhóm | Dòng | Hành vi |
|---|---|---|
| Tài khoản | Tên + *Thông tin cá nhân* | Mở màn **Account** để xem/sửa |
| Cài đặt | Ngôn ngữ, Thông báo, Hạn mức, Chế độ tối, Trợ giúp | Đổi ngôn ngữ, mở cài đặt thông báo hệ thống, đặt hạn mức, công tắc tối, hộp thoại trợ giúp |
| Dữ liệu | **Đồng bộ hồ sơ với Firebase**, **Sao lưu**, **Khôi phục**, **Tự động sao lưu**, **Đường dẫn sao lưu** | Đẩy hồ sơ mình lên Firestore, ghi bản sao CSV, khôi phục từ CSV (admin), bật/tắt tự động sao lưu, chọn thư mục lưu |
| Giới thiệu | SmartExpense + phiên bản, **Xem nhật ký thay đổi**, Github | Hộp thoại giới thiệu, mở màn Changelog, mở link Github |

Màn **Account** (ảnh 3.jpg) cho phép:
- chọn ảnh đại diện bằng **photo picker** (không cần quyền bộ nhớ), ảnh được thu nhỏ
  tối đa 512px và lưu trong `filesDir/avatars/`;
- sửa **tên** (ghi Room `user_profiles` + Firebase Auth, sau đó dựng lại `accounts.csv`),
  **giới tính** và **tuổi** (lưu trong AppPrefs, không đổi schema Room);
- **email** chỉ đọc; nút ✓ góc phải lưu thay đổi.

Từ bản **1.3.1**, màn Account mở ở **trạng thái chỉ đọc**: các trường (tên, tuổi,
giới tính, ảnh) khóa cho đến khi bấm **Edit account** — sau đó hiện nút ✓ để lưu.
Nút **Delete account** bên cạnh:
- hỏi *"Are you sure you're going to delete this account?"* với nút **Yes**
  bị khóa **5 giây** (chống bấm nhầm) rồi mới cho phép bấm;
- sau khi xác nhận: xóa sạch **giao dịch + hồ sơ** của uid trong Room, xóa
  document `users/{uid}` trên **Firestore**, thử xóa hẳn user trên **Firebase
  Auth** * (cần phiên đăng nhập gần đây — nếu Firebase từ chối thì chỉ đăng
  xuất; doc đám mây vẫn đã xóa), xóa ảnh đại diện và các tùy chọn riêng;
- kết thúc: đăng xuất hoàn toàn và quay về **màn Đăng nhập**.
Logic nằm trong `AccountViewModel.deleteAccount()`.

Từ bản **1.2.1**, toàn bộ Activity tự xử lý **window insets** (`utils/WindowInsets.kt`)
để nội dung không bị đè lên thanh trạng thái trên Android 15+ (lỗi thấy ở ảnh 4.jpg).
Trang chủ có thêm logo + tên **SmartExpense** phía trên dòng ngày.

### 6.2. Sao lưu / Khôi phục (bản 1.2.2)

Nhóm **Dữ liệu** thay hai dòng *Xuất CSV / Nhập CSV* bằng **Sao lưu / Khôi phục**
kèm hai cài đặt mới (ảnh 6.jpg), logic nằm trong `data/repository/BackupRepository.kt`:

| Dòng | Hành vi |
|---|---|
| **Sao lưu** | Dựng lại `accounts.csv` từ Room rồi ghi một tệp mới `smartexpense_backup_yyyyMMdd_HHmmss.csv` vào thư mục sao lưu. Snackbar hiện đường dẫn vừa ghi. Hiển thị hộp thoại *Cấp quyền dữ liệu* trước khi chạy. |
| **Khôi phục** | Chỉ admin. Chọn tệp `.csv` bằng SAF → gộp vào Room (uid trùng: cập nhật, uid mới: thêm) → dựng lại `accounts.csv`. Người dùng thường thấy thông báo *Chỉ tài khoản admin…*. |
| **Tự động sao lưu** | Công tắc (mặc định BẬT). Khi mở app, nếu `BuildConfig.VERSION_CODE` khác lần sao lưu trước → tự tạo một bản sao lưu rồi ghi nhớ `versionCode` (lưu trong AppPrefs). |
| **Đường dẫn sao lưu** | Hiển thị thư mục hiện tại. Bấm vào để **chọn thư mục khác** bằng SAF (quyền được giữ lại qua `takePersistableUriPermission`) hoặc **dùng mặc định**. |

- **Mặc định**: `Android/data/com.example.smartexpense/files/backups/` — thư mục riêng
  của app trên bộ nhớ ngoài, ghi được **không cần** quyền bộ nhớ.
- Bản sao lưu chứa đúng nội dung `accounts.csv` (hồ sơ tài khoản), **không** gồm
  mật khẩu và không gồm dữ liệu giao dịch trong Room.
- Xóa app sẽ xóa luôn thư mục mặc định; muốn giữ bản sao lâu dài hãy chọn một
  thư mục khác (ví dụ `Documents/SmartExpense`).
- Từ bản **1.2.3**, tệp trong thư mục tự chọn được tạo qua document URI của tree
  (`DocumentsContract.buildDocumentUriUsingTree` + `getTreeDocumentId`) nên ghi
  đúng vào thư mục người dùng đã chọn trên Android 11+.

### 6.3. Nhật ký thay đổi (bản 1.3.1)

Dòng **Xem nhật ký thay đổi / View changelogs** nằm trong nhóm *Giới thiệu*, giữa
dòng phiên bản SmartExpense và Github; ghi chú là *Có gì mới trong phiên bản này /
What's new in this version*.

Màn `ui/changelog/ChangelogActivity.kt` dùng giao diện **dòng thời gian**
(ảnh `Changelog_layout.jpg`):

- Đầu màn: icon app + tiêu đề **View changelogs** + chip `SmartExpense • vX`.
- Mỗi phiên bản là một mốc trên dòng kẻ dọc: phiên bản đang cài có chấm viền xanh
  và nhãn **Đã cài / Installed**, các bản cũ có chấm xám.
- Tóm tắt dưới tên phiên bản: *thời gian · N features · N fixes · N removes*,
  dùng `<plurals>` nên tự đổi **feature/features**, **fix/fixes**, **remove/removes**.
  Thời gian là **Hôm nay / Today**, **Hôm qua / Yesterday**, **n ngày trước** hoặc
  ngày đã định dạng theo ngôn ngữ app.
- Bấm vào dòng (hoặc mũi tên) để mở/đóng chi tiết; mũi tên xoay 180°.
- Chi tiết gồm các thẻ **New(s)**, **Fix(es)**, **Remove(s)** (ảnh `Changelog_info.jpg`),
  mỗi thẻ có icon màu, số lượng, danh sách gạch đầu dòng và nút **Hiện tất cả (N) /
  Show all (N)** khi có hơn 5 mục.
- Dòng kẻ dọc và chấm mốc do `ChangelogAdapter.TimelineDecoration` vẽ nên liền mạch
  khi cuộn; nội dung nằm trong `ui/changelog/ChangelogData.kt`.
- Toàn bộ nội dung là string/plurals resource nên tự đổi giữa Tiếng Việt và English.
- Khi phát hành phiên bản mới: tăng `versionName`/`versionCode` trong
  `app/build.gradle.kts`, thêm một `VersionEntry` vào `ChangelogData.versions`
  và bổ sung các chuỗi `cl_*` cho cả hai ngôn ngữ.

### 6.4. Chuyển kiểu biểu đồ Thống kê (bản 1.2.4)

Tab **Thống kê** có cụm nút nhỏ cạnh tiêu đề *Chi tiêu theo danh mục* để chuyển
giữa hai kiểu biểu đồ (mặc định là biểu đồ tròn):

- **Biểu đồ tròn**: `PieChartView` vẽ các cung nối tiếp nhau, tổng đúng 360° nên
  lấp đầy vòng tròn, không còn khoảng trống giữa các mảnh.
- **Biểu đồ cột dọc**: `StackedBarChartView` vẽ một thanh bo tròn hai đầu, các phần
  xếp chồng theo tỉ lệ chi tiêu từng danh mục; chú giải % nằm bên phải.
- Lựa chọn lưu trong `AppPrefs.statChartType` (`pie` | `bar`) nên giữ nguyên khi
  mở lại app.

---

## 7. Đa ngôn ngữ: Tiếng Việt & English

App có **2 ngôn ngữ**, mặc định theo ngôn ngữ của điện thoại, và từ bản **1.2 (3)**
có **nút đổi ngôn ngữ ngay trong app**:

- **Cài đặt → Ngôn ngữ** (mọi người dùng)
- **Quản trị → Cài đặt → Ngôn ngữ**

Cả hai mở cùng một hộp thoại: *Theo hệ thống / Tiếng Việt / English*. Lựa chọn
được lưu tự động bởi AppCompat (`AppLocalesMetadataHolderService` trong
manifest) và Activity sẽ tự tạo lại với ngôn ngữ mới.

| Thư mục | Ngôn ngữ | Vai trò |
|---|---|---|
| `app/src/main/res/values/strings.xml` | Tiếng Việt | Ngôn ngữ mặc định (mọi ngôn ngữ khác đều rơi về đây) |
| `app/src/main/res/values-en/strings.xml` | English | Dùng khi máy để English (en, en-US, en-GB…) |

**Đã bản địa hoá toàn bộ:**
- Mọi chuỗi trong layout, dialog, Snackbar/Toast và thông báo lỗi
  (`utils/Messages.kt` giờ chỉ giữ **ID tài nguyên** `@StringRes`, không còn chuỗi cứng).
- Tên thứ & tháng: `utils/DateTimeUtils.kt` dùng `Locale` của app → "Thứ Tư, 28/10/2026"
  hoặc "Wednesday, 28/10/2026"; tháng "Tháng 10/2026" hoặc "October 2026".
- 8 danh mục mặc định (seed lần chạy đầu) và ghi chú giao dịch demo.
- Dữ liệu minh hoạ ở tab Giao dịch của admin (`sample_tx_*`, `sample_owner_*`).
- Nhật ký hoạt động (BẬT/TẮT, khóa/mở khóa…).

**Đổi ngôn ngữ để thử:**
1. Cài đặt máy → Ngôn ngữ → English (hoặc Tiếng Việt).
2. Hoặc trên Android 13+: Cài đặt → Ứng dụng → SmartExpense → **Ngôn ngữ**
   (nhờ `res/xml/locales_config.xml` + `android:localeConfig` trong manifest).

**Thêm/sửa chuỗi:** sửa **cả hai** file `values/strings.xml` và `values-en/strings.xml`
với cùng `name`; nếu chỉ sửa một file thì ngôn ngữ kia sẽ hiển thị tiếng Việt.
Có thể kiểm tra nhanh bằng script đối chiếu key (mọi key phải khớp nhau).

> Lưu ý: danh mục được seed **một lần** khi chạy app lần đầu, nên nếu đổi ngôn ngữ sau đó
> thì tên danh mục đã lưu trong Room vẫn giữ ngôn ngữ cũ (dữ liệu người dùng, không tự dịch).
> Xóa dữ liệu app để seed lại theo ngôn ngữ mới.

---

## 8. Icon ứng dụng (bản 1.2 — từ Figma)

Icon launcher được thiết kế trong **`icon/icon_launcher.fig`** (Figma) — hình
**ví xanh `#2E5DC5`** với **túi `#F1F9E9`** chứa 3 cột biểu đồ, **chấm xanh lá
`#0C5143`** và **clasp** bên phải, đặt trên nền **gradient xanh nhạt bo cong**.

| File | Dùng cho |
|---|---|
| `app/src/main/res/drawable/ic_launcher_background.xml` | Lớp nền gradient full-bleed của adaptive icon |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Lớp foreground (hình ví) của adaptive icon |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | Themed icon một màu (Android 13+) |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | Khai báo adaptive icon |
| `app/src/main/res/mipmap-*dpi/ic_launcher*.png` | Icon cho Android 7.x và các launcher cũ |
| `tools/icon.json` | Dữ liệu hình học trích từ file `.fig` (path + màu) |

**Cách sinh lại từ file Figma:**
1. Decode `icon/icon_launcher.fig` (ZIP chứa `canvas.fig` — binary Kiwi của
   Figma) bằng script Node trong `$TEMP/fig_work` (fig-kiwi + kiwi-schema +
   fzstd): `node build_icon.mjs <canvas.fig> <icon.json> <out_xml>` —
   trích `fillGeometry` thành path SVG, bake translation và ghi ra 3 XML
   gradient/foreground/monochrome.
2. `python tools/gen_launcher_icon.py` đọc `tools/icon.json`, raster hoá
   (nonzero winding + supersampling) và viết lại toàn bộ PNG cho 5 mật độ
   (`ic_launcher.png` + `ic_launcher_round.png`), kèm preview 512px.
3. Build lại app.
