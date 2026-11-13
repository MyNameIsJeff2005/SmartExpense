package com.example.smartexpense.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.smartexpense.R

/**
 * Quản lý ngôn ngữ hiển thị (theo từng app) cho Android 7+ (API 24):
 *
 *  - Lựa chọn KHÔNG lưu trong AppPrefs mà dùng cơ chế lưu tự động của AppCompat
 *    (AppLocalesMetadataHolderService khai báo trong AndroidManifest).
 *  - Áp dụng bằng AppCompatDelegate.setApplicationLocales(...): Activity sẽ được
 *    tạo lại với ngôn ngữ mới; chọn "Theo hệ thống" thì về rỗng (dùng locale máy).
 *  - Chỉ hỗ trợ tiếng Việt (vi) và tiếng Anh (en) - khớp res/xml/locales_config.xml.
 */
object LanguageManager {

    const val LANG_SYSTEM = "system"
    const val LANG_VI = "vi"
    const val LANG_EN = "en"

    /** Thứ tự lựa chọn trong hộp thoại (dùng chung cho Profile và Admin). */
    val LANGS = arrayOf(LANG_SYSTEM, LANG_VI, LANG_EN)

    /** Đổi ngôn ngữ: lưu tự động + Activity tự tạo lại với ngôn ngữ mới. */
    fun apply(mode: String) {
        val locales = when (mode) {
            LANG_VI -> LocaleListCompat.forLanguageTags("vi")
            LANG_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList() // theo hệ thống
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** Lựa chọn hiện tại: "system" | "vi" | "en" (đọc từ AppCompat). */
    fun current(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val first = if (locales.isEmpty) null else locales[0]
        return when (first?.language) {
            "vi" -> LANG_VI
            "en" -> LANG_EN
            else -> LANG_SYSTEM
        }
    }

    /** Nhãn hiển thị của một lựa chọn, luôn dùng TÊN RIÊNG của ngôn ngữ đó. */
    fun label(context: Context, mode: String): String = when (mode) {
        LANG_VI -> "Tiếng Việt"
        LANG_EN -> "English"
        else -> context.getString(R.string.lang_system)
    }

    /** Nhãn của lựa chọn hiện tại - hiển thị ở dòng cài đặt. */
    fun currentLabel(context: Context): String = label(context, current())

    /** Danh sách nhãn theo đúng thứ tự [LANGS]. */
    fun labels(context: Context): Array<String> =
        LANGS.map { label(context, it) }.toTypedArray()

    /** Vị trí của lựa chọn trong [LANGS]. */
    fun indexOf(mode: String): Int = LANGS.indexOf(mode).let { if (it < 0) 0 else it }
}
