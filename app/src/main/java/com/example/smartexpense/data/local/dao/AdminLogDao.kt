package com.example.smartexpense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smartexpense.model.AdminLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminLogDao {

    @Insert
    suspend fun insert(log: AdminLogEntity)

    @Query("SELECT * FROM admin_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AdminLogEntity>>
}
