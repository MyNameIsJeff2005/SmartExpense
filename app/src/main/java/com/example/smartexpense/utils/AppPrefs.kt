package com.example.smartexpense.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * EncryptedSharedPreferences cho cài đặt KHÔNG phải credential (mục 3.10):
 * hạn mức tháng, cờ seed danh mục, công tắc auto_recreate_csv.
 * Không bao giờ lưu mật khẩu/token. Nếu khởi tạo encryption thất bại (máy lạ),
 * fallback sang SharedPreferences thường để app vẫn chạy được.
 */
class AppPrefs private constructor(context: Context) {

    private val prefs: SharedPreferences

    init {
        val appContext = context.applicationContext
        prefs = runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            appContext.getSharedPreferences("${PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    var monthlyBudget: Long
        get() = prefs.getLong(KEY_BUDGET, 0L)
        set(value) = prefs.edit().putLong(KEY_BUDGET, value).apply()

    var categoriesSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    /** Công tắc "Tự động tạo lại CSV khi file bị xóa" - mặc định BẬT (mục 3.2c) */
    var autoRecreateCsv: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CSV, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CSV, value).apply()

    /** Chế độ giao diện: "system" (theo máy) | "light" (sáng) | "dark" (tối) */
    var themeMode: String
        get() = prefs.getString(KEY_THEME, ThemeManager.MODE_SYSTEM).orEmpty()
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    /** Lần đồng bộ hồ sơ (Firestore) gần nhất (epoch millis, 0 = chưa từng đồng bộ) */
    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    /** Giới tính hiển thị ở màn Account: "male" | "female" */
    var accountGender: String
        get() = prefs.getString(KEY_GENDER, GENDER_MALE).orEmpty()
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    /** Tuổi người dùng (0 = chưa nhập) */
    var accountAge: Int
        get() = prefs.getInt(KEY_AGE, 0)
        set(value) = prefs.edit().putInt(KEY_AGE, value).apply()

    /** Đường dẫn tuyệt đối tới ảnh đại diện đã sao chép vào bộ nhớ app (rỗng = dùng chữ cái đầu) */
    var avatarPath: String
        get() = prefs.getString(KEY_AVATAR, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_AVATAR, value).apply()

    /** Thư mục sao lưu người dùng chọn (SAF tree URI; rỗng = thư mục mặc định của app) */
    var backupTreeUri: String
        get() = prefs.getString(KEY_BACKUP_TREE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BACKUP_TREE, value).apply()

    /** Công tắc "Tự động sao lưu khi có phiên bản mới" - mặc định BẬT giống bản thiết kế */
    var autoBackup: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP, value).apply()

    /** versionCode của lần tự động sao lưu gần nhất (0 = chưa từng sao lưu) */
    var lastBackupVersionCode: Int
        get() = prefs.getInt(KEY_LAST_BACKUP_VERSION, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_BACKUP_VERSION, value).apply()

    /** Lần sao lưu gần nhất (epoch millis, 0 = chưa từng sao lưu) */
    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_AT, value).apply()

    /** Kiểu biểu đồ ở tab Thống kê: "pie" (mặc định) | "bar" (cột dọc) */
    var statChartType: String
        get() = prefs.getString(KEY_CHART_TYPE, CHART_PIE).orEmpty()
        set(value) = prefs.edit().putString(KEY_CHART_TYPE, value).apply()

    companion object {
        private const val PREFS_NAME = "smartexpense_prefs"
        private const val KEY_BUDGET = "monthly_budget"
        private const val KEY_SEEDED = "categories_seeded"
        private const val KEY_AUTO_CSV = "auto_recreate_csv"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LAST_SYNC = "last_sync_at"
        private const val KEY_GENDER = "account_gender"
        private const val KEY_AGE = "account_age"
        private const val KEY_AVATAR = "account_avatar_path"
        private const val KEY_BACKUP_TREE = "backup_tree_uri"
        private const val KEY_AUTO_BACKUP = "auto_backup"
        private const val KEY_LAST_BACKUP_VERSION = "last_backup_version_code"
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
        private const val KEY_CHART_TYPE = "stat_chart_type"

        const val GENDER_MALE = "male"
        const val GENDER_FEMALE = "female"
        const val CHART_PIE = "pie"
        const val CHART_BAR = "bar"

        @Volatile
        private var instance: AppPrefs? = null

        fun get(context: Context): AppPrefs =
            instance ?: synchronized(this) {
                instance ?: AppPrefs(context).also { instance = it }
            }
    }
}
