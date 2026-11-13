package com.example.smartexpense.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemCategoryManageBinding
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.utils.UiUtils

/**
 * Dòng danh mục trong Quản lý danh mục (mục 6.6): badge màu + icon + tên +
 * nhãn Thu/Chi, bấm dòng mở hộp thoại sửa danh mục.
 */
class CategoryManageAdapter(
    private val onCategoryClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<CategoryManageAdapter.Holder>() {

    private var categories: List<CategoryEntity> = emptyList()

    fun submit(newCategories: List<CategoryEntity>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCategoryManageBinding.inflate(
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

        val isIncome = category.type == CategoryEntity.TYPE_INCOME
        binding.tvType.setText(
            if (isIncome) R.string.category_type_income else R.string.category_type_expense
        )

        binding.root.setOnClickListener { onCategoryClick(category) }
    }

    class Holder(val binding: ItemCategoryManageBinding) : RecyclerView.ViewHolder(binding.root)
}
