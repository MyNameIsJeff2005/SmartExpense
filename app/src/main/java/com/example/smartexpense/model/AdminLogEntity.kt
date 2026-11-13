package com.example.smartexpense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Nhật ký hành động của admin (tùy chọn, mục 6.7) */
@Entity(tableName = "admin_logs")
data class AdminLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,       // "LOCK", "UNLOCK", "RESET_PW", "REBUILD_CSV", "DELETE_CSV", "TOGGLE_CSV"
    val targetUid: String,
    val timestamp: Long
)
