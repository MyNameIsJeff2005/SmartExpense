package com.example.smartexpense.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemCategoryGridBinding
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.utils.UiUtils

/**
 * Ô danh mục trong lưới 4 cột của màn Thêm/Sửa giao dịch (mục 5.6).
 * Ô đang chọn đậm màu + chữ chính; ô còn lại mờ hơn (chữ phụ).
 */
class CategoryGridAdapter(
    private val onCategoryClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoryGridAdapter.Holder>() {

    private var categories: List<CategoryEntity> = emptyList()
    private var selectedId: Long = -1L

    /** Cập nhật danh sách + danh mục đang chọn. */
    fun submit(newCategories: List<CategoryEntity>, newSelectedId: Long) {
        categories = newCategories
        selectedId = newSelectedId
        notifyDataSetChanged()
    }

    /** Chọn lại danh mục khi nạp dữ liệu sửa giao dịch. */
    fun setSelectedId(id: Long) {
        if (selectedId != id) {
            selectedId = id
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCategoryGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val category = categories[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.badgeCircle.background?.let { drawable ->
            val wrapped = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(wrapped, UiUtils.parseColor(category.colorHex))
        }
        binding.ivIcon.setImageResource(UiUtils.categoryIconId(context, category.iconKey))
        binding.tvName.text = category.name

        val selected = category.id == selectedId
        binding.tvName.setTextColor(
            context.getColor(
                if (selected) R.color.text_primary else R.color.text_secondary
            )
        )
        binding.tvName.setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        binding.root.setOnClickListener { onCategoryClick(category) }
    }

    class Holder(val binding: ItemCategoryGridBinding) : RecyclerView.ViewHolder(binding.root)
}
