package com.example.smartexpense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Hồ sơ tài khoản lưu cục bộ (bản cache của tài khoản Firebase) - mục 3.2 */
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val createdAt: Long,     // epoch millis
    val role: String,        // "USER" | "ADMIN"
    val status: String,      // "ACTIVE" | "LOCKED"
    val lastLoginAt: Long
) {
    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ADMIN = "ADMIN"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_LOCKED = "LOCKED"
    }
}
