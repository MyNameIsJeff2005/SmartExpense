package com.example.smartexpense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Danh mục thu/chi dùng chung cho mọi tài khoản (mục 3.3) */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconUrl: String,      // URL ảnh (raw.githubusercontent) - nếu rỗng dùng iconKey
    val iconKey: String,      // tên drawable cục bộ làm fallback
    val colorHex: String,     // vd "#F97316"
    val type: String          // "EXPENSE" | "INCOME"
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
    }
}
