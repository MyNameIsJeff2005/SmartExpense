package com.example.smartexpense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartexpense.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * MỌI truy vấn đọc nội dung giao dịch phải truyền `uid` (mục 3.1.6).
 * Ngoại lệ duy nhất: countAll() chỉ đếm tổng số, không lộ nội dung, dành cho admin.
 */
@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(t: TransactionEntity): Long

    @Update
    suspend fun update(t: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id AND uid = :uid")
    suspend fun delete(id: Long, uid: String)

    /** Xóa sạch giao dịch của một tài khoản (dùng khi xóa tài khoản - 1.3.1) */
    @Query("DELETE FROM transactions WHERE uid = :uid")
    suspend fun deleteAllByUid(uid: String)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE uid = :uid AND dateTime BETWEEN :start AND :end ORDER BY dateTime DESC")
    fun observeRange(uid: String, start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE uid = :uid AND type = 'INCOME' AND dateTime BETWEEN :start AND :end")
    fun observeIncomeSum(uid: String, start: Long, end: Long): Flow<Long>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE uid = :uid AND type = 'EXPENSE' AND dateTime BETWEEN :start AND :end")
    fun observeExpenseSum(uid: String, start: Long, end: Long): Flow<Long>

    @Query("SELECT categoryId AS categoryId, IFNULL(SUM(amount), 0) AS total FROM transactions WHERE uid = :uid AND type = 'EXPENSE' AND dateTime BETWEEN :start AND :end GROUP BY categoryId")
    fun observeExpenseTotalsByCategory(uid: String, start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE uid = :uid")
    suspend fun countByUid(uid: String): Int
}

/** Kết quả tổng hợp chi tiêu theo danh mục (không phải bảng) */
data class CategoryTotal(val categoryId: Long, val total: Long)
