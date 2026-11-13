package com.example.smartexpense.ui.admin

import androidx.annotation.StringRes
import com.example.smartexpense.R

/**
 * Dữ liệu MINH HỌA cho tab Giao dịch của admin (mục 6.5):
 * admin KHÔNG được xem nội dung giao dịch thật của người dùng (mục 3.1.6),
 * tab này chỉ hiển thị tập giao dịch hợp lệ để minh họa theo đặc tả.
 *
 * Tiêu đề và tên chủ giao dịch để dạng ID tài nguyên (sample_tx_*, sample_owner_*)
 * để hiển thị đúng theo ngôn ngữ của máy.
 */
object SampleData {

    data class SampleTx(
        @StringRes val titleRes: Int,
        @StringRes val ownerRes: Int,
        val dateShort: String,
        val amount: Long,
        val income: Boolean,
        val flagged: Boolean = false,
        val colorHex: String = "#2563EB"
    )

    val all: List<SampleTx> = listOf(
        SampleTx(
            titleRes = R.string.sample_tx_1,
            ownerRes = R.string.sample_owner_1,
            dateShort = "26/10",
            amount = 90_000_000L,
            income = false,
            flagged = true,
            colorHex = "#DC2626"
        ),
        SampleTx(R.string.sample_tx_2, R.string.sample_owner_2, "25/10", 12_000_000L, true, colorHex = "#16A34A"),
        SampleTx(R.string.sample_tx_3, R.string.sample_owner_2, "24/10", 120_000L, false, colorHex = "#F97316"),
        SampleTx(R.string.sample_tx_4, R.string.sample_owner_3, "24/10", 3_500_000L, true, colorHex = "#16A34A"),
        SampleTx(R.string.sample_tx_5, R.string.sample_owner_3, "23/10", 350_000L, false, colorHex = "#0EA5E9"),
        SampleTx(R.string.sample_tx_6, R.string.sample_owner_1, "22/10", 260_000L, false, colorHex = "#8B5CF6"),
        SampleTx(R.string.sample_tx_7, R.string.sample_owner_2, "21/10", 2_000_000L, true, colorHex = "#2563EB"),
        SampleTx(R.string.sample_tx_8, R.string.sample_owner_4, "20/10", 180_000L, false, colorHex = "#FACC15"),
        SampleTx(R.string.sample_tx_9, R.string.sample_owner_3, "18/10", 890_000L, false, colorHex = "#10B981"),
        SampleTx(R.string.sample_tx_10, R.string.sample_owner_4, "16/10", 1_500_000L, true, colorHex = "#16A34A")
    )
}
