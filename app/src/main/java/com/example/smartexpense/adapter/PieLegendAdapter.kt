package com.example.smartexpense.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.databinding.ItemPieLegendBinding
import com.example.smartexpense.model.CategoryEntity

/**
 * Chú giải biểu đồ tròn / cột dọc (mục 5.5): chấm màu + tên danh mục + phần trăm.
 * Dữ liệu vào: cặp [CategoryEntity, tổng chi] — phần trăm tự tính theo tổng toàn bộ.
 */
class PieLegendAdapter :
    ListAdapter<Pair<CategoryEntity, Long>, PieLegendAdapter.Holder>(DIFF) {

    /** Tên ngắn đồng bộ với các adapter khác. */
    fun submit(list: List<Pair<CategoryEntity, Long>>) = submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPieLegendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (category, total) = currentList[position]
        val grandTotal = currentList.sumOf { it.second }
        val binding = holder.binding

        // Chấm màu danh mục; hex không hợp lệ thì bỏ màu (giữ màu badge mặc định)
        runCatching {
            val color = Color.parseColor(category.colorHex)
            binding.dot.background?.let { drawable ->
                val wrapped = DrawableCompat.wrap(drawable.mutate())
                DrawableCompat.setTint(wrapped, color)
            }
        }

        binding.tvName.text = category.name
        binding.tvPercent.text =
            if (grandTotal > 0) "${total * 100 / grandTotal}%" else "0%"
    }

    class Holder(val binding: ItemPieLegendBinding) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Pair<CategoryEntity, Long>>() {
            override fun areItemsTheSame(
                oldItem: Pair<CategoryEntity, Long>,
                newItem: Pair<CategoryEntity, Long>
            ) = oldItem.first.id == newItem.first.id

            override fun areContentsTheSame(
                oldItem: Pair<CategoryEntity, Long>,
                newItem: Pair<CategoryEntity, Long>
            ) = oldItem == newItem
        }
    }
}
