package com.example.smartexpense.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemAdminTransactionBinding
import com.example.smartexpense.ui.common.TxRow
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.UiUtils

/**
 * Dòng giao dịch minh họa tab admin (mục 6.5): cờ đỏ nếu giao dịch bị gắn cờ.
 * Dữ liệu vào: [AdminTxRow] = dòng giao dịch + tên người dùng + ngày rút gọn.
 * KHÔNG mở chi tiết người dùng khi click (mục 3.1.6).
 */
class AdminTransactionAdapter :
    RecyclerView.Adapter<AdminTransactionAdapter.Holder>() {

    data class AdminTxRow(
        val txRow: TxRow,
        val owner: String,
        val dateShort: String
    )

    private var rows: List<AdminTxRow> = emptyList()

    fun submit(newRows: List<AdminTxRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAdminTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = rows[position]
        val tx = item.txRow
        val binding = holder.binding
        val context = binding.root.context

        binding.badgeCircle.background?.let { drawable ->
            val wrapped = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(wrapped, UiUtils.parseColor(tx.colorHex))
        }
        binding.ivIcon.setImageResource(
            UiUtils.categoryIconId(context, tx.iconKey)
        )

        binding.tvTitle.text = tx.title
        binding.ivFlag.visibility = if (tx.flagged) View.VISIBLE else View.GONE

        val ownerLine = item.owner + (if (item.dateShort.isBlank()) "" else " · ${item.dateShort}")
        binding.tvSubtitle.text = ownerLine

        val sign = if (tx.income) "+" else "-"
        binding.tvAmount.text = sign + CurrencyUtils.formatVnd(tx.amount)
        binding.tvAmount.setTextColor(
            context.getColor(if (tx.income) R.color.green else R.color.red)
        )
    }

    class Holder(val binding: ItemAdminTransactionBinding) : RecyclerView.ViewHolder(binding.root)
}
