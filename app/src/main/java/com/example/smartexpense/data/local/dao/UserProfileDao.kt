package com.example.smartexpense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartexpense.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** TẤT CẢ thao tác hồ sơ đều hoạt động trên bảng user_profiles */
@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): UserProfile?

    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<UserProfile>

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun countProfiles(): Int

    @Query("SELECT COUNT(*) FROM user_profiles WHERE status = 'LOCKED'")
    suspend fun countLocked(): Int

    @Query("SELECT COUNT(*) FROM user_profiles WHERE lastLoginAt BETWEEN :start AND :end")
    suspend fun countActiveBetween(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM user_profiles WHERE createdAt BETWEEN :start AND :end")
    suspend fun countCreatedBetween(start: Long, end: Long): Int

    @Query("UPDATE user_profiles SET status = 'LOCKED' WHERE uid = :uid")
    suspend fun lockUser(uid: String)

    @Query("UPDATE user_profiles SET status = 'ACTIVE' WHERE uid = :uid")
    suspend fun unlockUser(uid: String)

    @Query("UPDATE user_profiles SET lastLoginAt = :time WHERE uid = :uid")
    suspend fun updateLastLogin(uid: String, time: Long)

    /** Xóa hẳn hồ sơ một tài khoản khỏi bảng (dùng khi xóa tài khoản - 1.3.1) */
    @Query("DELETE FROM user_profiles WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}
