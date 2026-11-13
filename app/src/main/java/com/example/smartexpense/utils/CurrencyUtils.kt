package com.example.smartexpense.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs

/** Định dạng tiền/đếm: 1.234.567 ₫ theo mục 4.2 (thu "+", chi "-" do UI xử lý) */
object CurrencyUtils {

    private val groupingFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,###", symbols)
    }

    fun formatVnd(amount: Long): String =
        groupingFormat.format(amount) + " ₫"

    /** Đếm tổng hợp kiểu 1.248 cho giao diện admin (mục 6) */
    fun formatCount(count: Int): String = groupingFormat.format(count.toLong())

    /** Dùng ô nhập số tiền: chỉ số, không ký hiệu */
    fun formatPlain(amount: Long): String = groupingFormat.format(amount)
}
