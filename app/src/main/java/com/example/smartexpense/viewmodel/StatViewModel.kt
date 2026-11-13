package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.data.local.dao.CategoryTotal
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CategoryRepository
import com.example.smartexpense.repository.TransactionRepository
import com.example.smartexpense.ui.common.PieChartView
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.UiUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

/** Thống kê: tổng thu/chi theo tháng + biểu đồ tròn chi tiêu theo danh mục (mục 3.5, 5.5) */
class StatViewModel(app: Application) : AndroidViewModel(app) {

    private val txRepo = TransactionRepository(app)
    private val categoryRepo = CategoryRepository(app)
    private val uid: String = AuthRepository(app).currentUserUid() ?: ""

    val selectedMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val incomeSum: StateFlow<Long> = selectedMonth.flatMapLatest { m ->
        txRepo.observeIncomeSum(uid, DateTimeUtils.startOfMonth(m), DateTimeUtils.endOfMonth(m))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val expenseSum: StateFlow<Long> = selectedMonth.flatMapLatest { m ->
        txRepo.observeExpenseSum(uid, DateTimeUtils.startOfMonth(m), DateTimeUtils.endOfMonth(m))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val totalsByCategory: StateFlow<List<CategoryTotal>> = selectedMonth.flatMapLatest { m ->
        txRepo.observeExpenseTotalsByCategory(uid, DateTimeUtils.startOfMonth(m), DateTimeUtils.endOfMonth(m))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class StatState(
        val income: Long = 0,
        val expense: Long = 0,
        val expenseByCategory: List<Pair<CategoryEntity, Long>> = emptyList()
    ) {
        val balance: Long get() = income - expense

        /** Dữ liệu cho PieChartView: mỗi danh mục chi tiêu một màu (mục 5.5) */
        val pieSlices: List<PieChartView.Slice>
            get() {
                val grandTotal = expenseByCategory.sumOf { it.second }
                if (grandTotal <= 0L) return emptyList()
                return expenseByCategory.map { (category, total) ->
                    PieChartView.Slice(UiUtils.parseColor(category.colorHex), total.toFloat() / grandTotal)
                }
            }
    }

    val statState: StateFlow<StatState> = combine(
        incomeSum, expenseSum, totalsByCategory, categoryRepo.observeAll()
    ) { income, expense, totals, categories ->
        val byId = categories.associateBy { it.id }
        StatState(
            income = income,
            expense = expense,
            expenseByCategory = totals.mapNotNull { t ->
                byId[t.categoryId]?.let { it to t.total }
            }.sortedByDescending { it.second }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatState())
}
