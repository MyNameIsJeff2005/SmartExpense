package com.example.smartexpense.ui.common

/** Dòng dữ liệu giao diện giao dịch dùng chung (Home / List / Admin) */
data class TxRow(
    val id: Long = 0,
    val title: String,
    val subtitle: String,
    val amount: Long,
    val income: Boolean,
    val colorHex: String? = null,
    val iconKey: String? = null,
    val flagged: Boolean = false
)

/** Danh sách nhóm theo ngày: tiêu đề ngày + các dòng giao dịch */
sealed interface GroupedRow {
    data class DayHeader(val label: String, val dayIncome: Long, val dayExpense: Long) : GroupedRow
    data class ItemRow(val txId: Long, val txRow: TxRow) : GroupedRow
}
