package com.example.smartexpense.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Giao dịch thu/chi. `dateTime` là epoch millis UTC gồm cả giờ phút (mục 3.1.4).
 * Index (uid, dateTime) phục vụ lọc tháng/nhóm ngày nhanh.
 */
@Entity(
    tableName = "transactions",
    indices = [Index("uid", "dateTime")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,          // Firebase UID chủ sở hữu
    val type: String,         // TYPE_INCOME | TYPE_EXPENSE
    val amount: Long,         // > 0
    val categoryId: Long,
    val dateTime: Long,       // epoch millis UTC
    val note: String = "",
    val createdAt: Long
) {
    companion object {
        const val TYPE_INCOME = "INCOME"
        const val TYPE_EXPENSE = "EXPENSE"
    }
}
