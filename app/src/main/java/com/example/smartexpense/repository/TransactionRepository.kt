package com.example.smartexpense.repository

import android.content.Context
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.data.local.dao.CategoryTotal
import com.example.smartexpense.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Repository cho CRUD giao dịch (mọi query đều theo uid) */
class TransactionRepository(private val context: Context) {

    private val dao get() = AppDatabase.get(context).transactionDao()

    suspend fun insert(t: TransactionEntity): Long = dao.insert(t)

    suspend fun update(t: TransactionEntity) = dao.update(t)

    suspend fun delete(id: Long, uid: String) = dao.delete(id, uid)

    suspend fun getById(id: Long): TransactionEntity? = dao.getById(id)

    suspend fun countAll(): Int = dao.countAll()

    suspend fun countByUid(uid: String): Int = dao.countByUid(uid)

    fun observeRange(uid: String, start: Long, end: Long): Flow<List<TransactionEntity>> =
        dao.observeRange(uid, start, end)

    fun observeIncomeSum(uid: String, start: Long, end: Long): Flow<Long> =
        dao.observeIncomeSum(uid, start, end)

    fun observeExpenseSum(uid: String, start: Long, end: Long): Flow<Long> =
        dao.observeExpenseSum(uid, start, end)

    fun observeExpenseTotalsByCategory(uid: String, start: Long, end: Long): Flow<List<CategoryTotal>> =
        dao.observeExpenseTotalsByCategory(uid, start, end)
}
