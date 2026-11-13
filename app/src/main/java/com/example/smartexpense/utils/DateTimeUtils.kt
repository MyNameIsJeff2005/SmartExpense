package com.example.smartexpense.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Mọi hàm ngày giờ gom vào đây (mục 3.11): java.time + desugaring,
 * ranh giới ngày/tháng theo múi giờ thiết bị; tuần bắt đầu Thứ Hai.
 */
object DateTimeUtils {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /**
     * Ngôn ngữ dùng để hiển thị thứ/tháng - lấy theo ngôn ngữ mà Android đang dùng cho app
     * (values = tiếng Việt, values-en = tiếng Anh), nên ngày tháng đổi theo máy người dùng.
     */
    private var appLocale: Locale = Locale.getDefault()

    /** Gọi từ Application để đồng bộ ngôn ngữ hiện tại của app. */
    fun syncLocale(context: android.content.Context) {
        appLocale = context.resources.configuration.locales[0]
    }

    /** Tên thứ đầy đủ theo ngôn ngữ app: "Thứ Tư" / "Wednesday". */
    fun weekdayName(dow: DayOfWeek): String =
        dow.getDisplayName(TextStyle.FULL, appLocale).replaceFirstChar { it.titlecase(appLocale) }

    /** "Thứ Tư, 28/10/2026" hoặc "Wednesday, 28/10/2026" */
    fun formatLongDate(date: LocalDate): String =
        "${weekdayName(date.dayOfWeek)}, ${date.dayOfMonth}/${date.monthValue}/${date.year}"

    fun formatLongDate(millis: Long): String = formatLongDate(toLocalDate(millis))

    /** "Tháng 10/2026" (vi) hoặc "October 2026" (en) */
    fun formatMonth(month: YearMonth): String =
        if (appLocale.language == "vi") {
            "Tháng ${month.monthValue}/${month.year}"
        } else {
            month.month.getDisplayName(TextStyle.FULL, appLocale)
                .replaceFirstChar { it.titlecase(appLocale) } + " " + month.year
        }

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    var dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatDate(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        return pad2(dt.dayOfMonth) + "/" + pad2(dt.monthValue) + "/" + dt.year
    }

    fun formatTime(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        return pad2(dt.hour) + ":" + pad2(dt.minute)
    }

    /** "28/10/2026 · 14:05" */
    fun formatDateTime(millis: Long): String = "${formatDate(millis)} · ${formatTime(millis)}"

    /** ISO-8601 có múi giờ cho cột created_at trong CSV (mục 3.2) */
    fun toIsoOffsetString(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    // ---------- Ranh giới thời gian ----------

    fun startOfMonth(month: YearMonth): Long =
        month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfMonth(month: YearMonth): Long =
        month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(date: LocalDate): Long =
        date.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    fun today(): LocalDate = LocalDate.now(zone)

    private fun pad2(n: Int): String = if (n < 10) "0$n" else n.toString()
}
