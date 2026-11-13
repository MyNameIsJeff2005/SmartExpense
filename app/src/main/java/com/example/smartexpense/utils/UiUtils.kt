package com.example.smartexpense.utils

import android.content.Context
import android.graphics.Color
import com.example.smartexpense.R

/** Hỗ trợ icon và màu danh mục */
object UiUtils {

    /** Tìm drawable theo tên iconKey của danh mục; không có thì dùng icon chung */
    fun categoryIconId(context: Context, iconKey: String?): Int {
        if (iconKey.isNullOrBlank()) return R.drawable.ic_category_generic
        val id = context.resources.getIdentifier(iconKey, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.ic_category_generic
    }

    /** Đọc mã màu danh mục; giá trị không hợp lệ -> xanh chủ đạo */
    fun parseColor(hex: String?): Int {
        if (hex.isNullOrBlank()) return Color.parseColor("#2563EB")
        return runCatching { Color.parseColor(hex) }.getOrDefault(Color.parseColor("#2563EB"))
    }

    /** Chữ cái đầu làm avatar (ký tự đầu của tên sau khi bỏ khoảng trắng) */
    fun initial(name: String?): String {
        val trimmed = name?.trim().orEmpty()
        return if (trimmed.isEmpty()) "?" else trimmed.substring(0, 1).uppercase()
    }
}
