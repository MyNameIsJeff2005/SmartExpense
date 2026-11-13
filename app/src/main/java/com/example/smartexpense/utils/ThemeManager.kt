package com.example.smartexpense.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.smartexpense.R

/**
 * Quản lý chế độ sáng / tối (mục 3.10 - cài đặt giao diện).
 *
 * Cách hoạt động:
 *  - Lựa chọn lưu trong AppPrefs (EncryptedSharedPreferences) dưới khóa "theme_mode".
 *  - Áp dụng bằng AppCompatDelegate.setDefaultNightMode(...) ngay khi mở app
 *    (SmartExpenseApp) và ngay khi người dùng đổi trong Cài đặt.
 *  - Toàn bộ màu nền/chữ nằm trong res/values/colors.xml và res/values-night/colors.xml
 *    nên khi đổi chế độ, Activity tự tạo lại với bảng màu tương ứng.
 */
object ThemeManager {

    const val MODE_SYSTEM = "system"
    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"

    /** Thứ tự lựa chọn hiển thị trong hộp thoại (dùng chung cho Profile và Admin). */
    val MODES = arrayOf(MODE_SYSTEM, MODE_LIGHT, MODE_DARK)

    /** Áp dụng chế độ đã lưu (hoặc chế độ truyền vào) cho toàn app. */
    fun apply(context: Context, mode: String = AppPrefs.get(context).themeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** Đổi chế độ: lưu lại rồi áp dụng ngay (Activity sẽ được tạo lại). */
    fun setMode(context: Context, mode: String) {
        AppPrefs.get(context).themeMode = mode
        apply(context, mode)
    }

    /** Nhãn tiếng Việt của một chế độ. */
    fun label(context: Context, mode: String): String = when (mode) {
        MODE_LIGHT -> context.getString(R.string.theme_light)
        MODE_DARK -> context.getString(R.string.theme_dark)
        else -> context.getString(R.string.theme_system)
    }

    /** Danh sách nhãn theo đúng thứ tự [MODES] - dùng cho AlertDialog.setSingleChoiceItems. */
    fun labels(context: Context): Array<String> = MODES.map { label(context, it) }.toTypedArray()

    /** Vị trí của một chế độ trong [MODES]. */
    fun indexOf(mode: String): Int = MODES.indexOf(mode).let { if (it < 0) 0 else it }
}
