package com.example.smartexpense.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemCalendarDayBinding
import java.time.LocalDate

/**
 * Ô ngày của lưới lịch 7 cột (mục 3.11): chấm xanh lá nếu ngày có thu,
 * chấm đỏ nếu có chi; ngày đang chọn bo pill xanh; ngày hôm nay viền nhẹ.
 * Số ngày dùng màu @color/text_primary để tự đổi theo chế độ sáng/tối (1.3.1).
 */
class CalendarAdapter(
    private val onDayClick: (LocalDate?) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.Holder>() {

    data class Cell(
        val date: LocalDate?,   // null = ô trống đầu tháng
        val hasIncome: Boolean,
        val hasExpense: Boolean
    )

    private var cells: List<Cell> = emptyList()
    private var selectedDay: LocalDate? = null
    private var today: LocalDate = LocalDate.now()

    /** Cập nhật toàn bộ lưới + ngày đang chọn (fragment tự gọi lại khi đổi ngày). */
    fun submit(newCells: List<Cell>, newSelected: LocalDate?) {
        cells = newCells
        selectedDay = newSelected
        today = LocalDate.now()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = cells.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val cell = cells[position]
        val binding = holder.binding
        val context = binding.root.context

        if (cell.date == null) {
            // Ô trống đầu tháng: số rỗng, không chấm, không nhận touch
            binding.tvDayNumber.text = ""
            binding.dotIncome.visibility = android.view.View.GONE
            binding.dotExpense.visibility = android.view.View.GONE
            binding.dayRoot.setBackgroundResource(android.R.color.transparent)
            binding.dayRoot.setOnClickListener(null)
            return
        }

        binding.tvDayNumber.text = cell.date.dayOfMonth.toString()
        binding.tvDayNumber.setTextColor(
            ContextCompat.getColor(context, R.color.text_primary)
        )

        binding.dotIncome.visibility =
            if (cell.hasIncome) android.view.View.VISIBLE else android.view.View.GONE
        binding.dotExpense.visibility =
            if (cell.hasExpense) android.view.View.VISIBLE else android.view.View.GONE

        // Nền: ngày chọn -> pill xanh; hôm nay (chưa chọn) -> viền nhạt; còn lại trong suốt
        binding.dayRoot.setBackgroundResource(
            when {
                cell.date == selectedDay -> R.drawable.bg_day_selected
                cell.date == today -> R.drawable.bg_day_today
                else -> android.R.color.transparent
            }
        )

        binding.dayRoot.setOnClickListener { onDayClick(cell.date) }
    }

    class Holder(val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root)
}
