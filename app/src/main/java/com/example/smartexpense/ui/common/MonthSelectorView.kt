package com.example.smartexpense.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.example.smartexpense.databinding.MonthSelectorBinding
import com.example.smartexpense.utils.DateTimeUtils
import java.time.YearMonth

/**
 * Bộ chọn tháng ◀ ▶ (mục 3.11): tự đúng ranh giới tháng (xử lý tháng 2, năm nhuận
 * được YearMonth đảm nhiệm). Đặt trong layout bất kỳ và lắng nghe onMonthChanged.
 */
class MonthSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = MonthSelectorBinding.inflate(LayoutInflater.from(context), this, true)

    var current: YearMonth = YearMonth.now()
        private set

    var onMonthChanged: ((YearMonth) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        @SuppressLint("CustomViewStyleable")
        binding.monthLabel.text = DateTimeUtils.formatMonth(current)
        binding.btnPrev.setOnClickListener { move(-1) }
        binding.btnNext.setOnClickListener { move(1) }
    }

    /** Đặt lại tháng từ bên ngoài (không phát sự kiện) */
    fun setMonth(month: YearMonth) {
        current = month
        binding.monthLabel.text = DateTimeUtils.formatMonth(month)
    }

    private fun move(delta: Int) {
        current = current.plusMonths(delta.toLong())
        binding.monthLabel.text = DateTimeUtils.formatMonth(current)
        onMonthChanged?.invoke(current)
    }
}
