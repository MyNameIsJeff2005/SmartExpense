package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.R
import com.example.smartexpense.model.TransactionEntity
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CategoryRepository
import com.example.smartexpense.repository.TransactionRepository
import com.example.smartexpense.ui.common.TxRow
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/** Tổng hợp dữ liệu Trang chủ: số dư tháng, thu/chi, giao dịch gần đây, hạn mức */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository(app)
    private val txRepo = TransactionRepository(app)
    private val categoryRepo = CategoryRepository(app)
    private val prefs = AppPrefs.get(app)
    private val uid: String = auth.currentUserUid() ?: ""

    val selectedMonth = MutableStateFlow(YearMonth.now())

    data class MonthData(
        val income: Long = 0,
        val expense: Long = 0,
        val recent: List<TxRow> = emptyList()
    ) {
        val balance: Long get() = income - expense
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthData: StateFlow<MonthData> = selectedMonth.flatMapLatest { m ->
        val start = DateTimeUtils.startOfMonth(m)
        val end = DateTimeUtils.endOfMonth(m)
        combine(
            txRepo.observeIncomeSum(uid, start, end),
            txRepo.observeExpenseSum(uid, start, end),
            txRepo.observeRange(uid, start, end),
            categoryRepo.observeAll()
        ) { income, expense, txs, categories ->
            val byId = categories.associateBy { it.id }
            MonthData(
                income = income,
                expense = expense,
                recent = txs.take(5).map { t ->
                    val category = byId[t.categoryId]
                    TxRow(
                        id = t.id,
                        title = t.note.ifBlank { category?.name ?: getApplication<Application>().getString(R.string.tx_fallback_title) },
                        subtitle = (category?.name ?: getApplication<Application>().getString(R.string.tx_fallback_title)) +
                            " · " + DateTimeUtils.formatDate(t.dateTime),
                        amount = t.amount,
                        income = t.type == TransactionEntity.TYPE_INCOME,
                        colorHex = category?.colorHex,
                        iconKey = category?.iconKey
                    )
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthData())

    // ---------- Hạn mức chi tiêu (mục 3.7, 3.10) ----------

    val budget = MutableStateFlow(prefs.monthlyBudget)

    fun saveBudget(amount: Long) {
        prefs.monthlyBudget = amount
        budget.value = amount
    }

}
