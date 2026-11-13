package com.example.smartexpense.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemDayHeaderBinding
import com.example.smartexpense.databinding.ItemTransactionRowBinding
import com.example.smartexpense.ui.common.GroupedRow
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.UiUtils

/**
 * Danh sách giao dịch nhóm theo ngày (mục 5.4):
 * - DayHeader: tiêu đề ngày + tổng thu/chi của ngày
 * - ItemRow: dòng giao dịch (dùng chung item_transaction_row)
 * Click vào dòng giao dịch -> callback với [GroupedRow.ItemRow].
 */
class GroupedTransactionAdapter(
    private val onItemClick: (GroupedRow.ItemRow) -> Unit
) : ListAdapter<GroupedRow, RecyclerView.ViewHolder>(DIFF) {

    /** Tên ngắn đồng bộ với các adapter khác. */
    fun submit(list: List<GroupedRow>) = submitList(list)

    override fun getItemViewType(position: Int): Int =
        if (currentList[position] is GroupedRow.DayHeader) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemDayHeaderBinding.inflate(inflater, parent, false))
        } else {
            ItemHolder(ItemTransactionRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = currentList[position]) {
            is GroupedRow.DayHeader -> bindHeader(holder as HeaderHolder, row)
            is GroupedRow.ItemRow -> bindItem(holder as ItemHolder, row)
        }
    }

    private fun bindHeader(holder: HeaderHolder, header: GroupedRow.DayHeader) {
        val binding = holder.binding
        binding.tvDay.text = header.label

        if (header.dayIncome > 0) {
            binding.tvDayIncome.visibility = View.VISIBLE
            binding.tvDayIncome.text = "+" + CurrencyUtils.formatVnd(header.dayIncome)
        } else {
            binding.tvDayIncome.visibility = View.GONE
        }

        if (header.dayExpense > 0) {
            binding.tvDayExpense.visibility = View.VISIBLE
            binding.tvDayExpense.text = "-" + CurrencyUtils.formatVnd(header.dayExpense)
        } else {
            binding.tvDayExpense.visibility = View.GONE
        }
    }

    private fun bindItem(holder: ItemHolder, row: GroupedRow.ItemRow) {
        val tx = row.txRow
        val binding = holder.binding
        val context = binding.root.context

        binding.ivIcon.setImageResource(UiUtils.categoryIconId(context, tx.iconKey))
        val color = UiUtils.parseColor(tx.colorHex)
        binding.badgeCircle.background?.let { drawable ->
            val wrapped = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(wrapped, color)
        }

        binding.tvTitle.text = tx.title
        binding.tvSubtitle.text = tx.subtitle
        val sign = if (tx.income) "+" else "-"
        binding.tvAmount.text = sign + CurrencyUtils.formatVnd(tx.amount)
        binding.tvAmount.setTextColor(
            context.getColor(if (tx.income) R.color.green else R.color.text_primary)
        )

        binding.root.setOnClickListener { onItemClick(row) }
    }

    class HeaderHolder(val binding: ItemDayHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class ItemHolder(val binding: ItemTransactionRowBinding) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1

        val DIFF = object : DiffUtil.ItemCallback<GroupedRow>() {
            override fun areItemsTheSame(oldItem: GroupedRow, newItem: GroupedRow): Boolean =
                when {
                    oldItem is GroupedRow.DayHeader && newItem is GroupedRow.DayHeader ->
                        oldItem.label == newItem.label
                    oldItem is GroupedRow.ItemRow && newItem is GroupedRow.ItemRow ->
                        oldItem.txId == newItem.txId
                    else -> false
                }

            override fun areContentsTheSame(oldItem: GroupedRow, newItem: GroupedRow) =
                oldItem == newItem
        }
    }
}
