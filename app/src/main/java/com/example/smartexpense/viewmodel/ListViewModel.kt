package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.R
import com.example.smartexpense.model.TransactionEntity
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CategoryRepository
import com.example.smartexpense.repository.TransactionRepository
import com.example.smartexpense.ui.common.GroupedRow
import com.example.smartexpense.ui.common.TxRow
import com.example.smartexpense.utils.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

enum class TxFilter { ALL, INCOME, EXPENSE }

/**
 * Dữ liệu tab Giao dịch: lọc loại/tháng, nhóm theo ngày, chế độ lịch (mục 3.1.5, 3.11).
 * Mọi query giao dịch đều theo UID hiện tại (mục 3.1.6).
 */
class ListViewModel(app: Application) : AndroidViewModel(app) {

    private val txRepo = TransactionRepository(app)
    private val categoryRepo = CategoryRepository(app)
    private val uid: String = AuthRepository(app).currentUserUid() ?: ""

    val selectedMonth = MutableStateFlow(YearMonth.now())
    val filter = MutableStateFlow(TxFilter.ALL)
    val selectedDay = MutableStateFlow<LocalDate?>(null)

    /** Toàn bộ giao dịch tháng (không lọc loại) — dùng cho nhóm ngày & lịch */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthAll: StateFlow<List<TransactionEntity>> = selectedMonth.flatMapLatest { m ->
        txRepo.observeRange(uid, DateTimeUtils.startOfMonth(m), DateTimeUtils.endOfMonth(m))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Giao dịch đã áp lọc Thu/Chi */
    val items: StateFlow<List<TransactionEntity>> = combine(monthAll, filter) { list, f ->
        when (f) {
            TxFilter.ALL -> list
            TxFilter.INCOME -> list.filter { it.type == TransactionEntity.TYPE_INCOME }
            TxFilter.EXPENSE -> list.filter { it.type == TransactionEntity.TYPE_EXPENSE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Danh sách nhóm theo ngày: header (tổng thu/chi ngày) + các dòng giao dịch */
    val groupedRows: StateFlow<List<GroupedRow>> = combine(
        items, monthAll, categoryRepo.observeAll()
    ) { filtered, all, categories ->
        val byId = categories.associateBy { it.id }
        val rows = mutableListOf<GroupedRow>()

        // Tổng thu/chi từng ngày tính từ danh sách KHÔNG lọc (mục 3.1.5)
        val headerSums = mutableMapOf<LocalDate, Pair<Long, Long>>()
        all.forEach { t ->
            val day = DateTimeUtils.toLocalDate(t.dateTime)
            val acc = headerSums[day] ?: (0L to 0L)
            headerSums[day] = if (t.type == TransactionEntity.TYPE_INCOME) {
                Pair(acc.first + t.amount, acc.second)
            } else {
                Pair(acc.first, acc.second + t.amount)
            }
        }

        filtered.groupBy { DateTimeUtils.toLocalDate(it.dateTime) }
            .entries
            .sortedByDescending { it.key }
            .forEach { (day, txs) ->
                val sums = headerSums[day] ?: (0L to 0L)
                rows.add(GroupedRow.DayHeader(DateTimeUtils.formatLongDate(day), sums.first, sums.second))
                txs.forEach { t ->
                    val category = byId[t.categoryId]
                    rows.add(
                        GroupedRow.ItemRow(
                            txId = t.id,
                            txRow = TxRow(
                                id = t.id,
                                title = t.note.ifBlank { category?.name ?: getApplication<Application>().getString(R.string.tx_fallback_title) },
                                subtitle = (category?.name ?: getApplication<Application>().getString(R.string.tx_fallback_title)) +
                                    " · " + DateTimeUtils.formatDate(t.dateTime),
                                amount = t.amount,
                                income = t.type == TransactionEntity.TYPE_INCOME,
                                colorHex = category?.colorHex,
                                iconKey = category?.iconKey
                            )
                        )
                    )
                }
            }
        rows
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Giao dịch của ngày đang chọn trong chế độ Lịch (không theo lọc loại - mục 3.11) */
    val dayTransactions: StateFlow<List<TransactionEntity>> = combine(monthAll, selectedDay) { list, day ->
        list.filter { t -> day != null && DateTimeUtils.toLocalDate(t.dateTime) == day }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
