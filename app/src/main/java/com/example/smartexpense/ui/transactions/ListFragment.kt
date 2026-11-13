package com.example.smartexpense.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.R
import com.example.smartexpense.databinding.FragmentListBinding
import com.example.smartexpense.adapter.TransactionRowAdapter
import com.example.smartexpense.adapter.CalendarAdapter
import com.example.smartexpense.adapter.GroupedTransactionAdapter
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.viewmodel.ListViewModel
import com.example.smartexpense.viewmodel.TxFilter
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Tab Giao dịch (mục 5.4, 3.11):
 * - Danh sách: nhóm theo ngày (tiêu đề + tổng), click một giao dịch -> Chi tiết
 * - Lịch: lưới 7 cột bắt đầu thứ Hai; chấm xanh lá/đỏ; chạm 1 ngày xem giao dịch trong ngày
 * - Lọc Thu/Chi áp cho dạng Danh sách; Lịch luôn tổng hợp toàn bộ (mục 3.11)
 */
class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ListViewModel by viewModels()

    private lateinit var groupedAdapter: GroupedTransactionAdapter
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var dayAdapter: TransactionRowAdapter

    private var calendarMode = false
    private var monthCells: List<CalendarAdapter.Cell> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupedAdapter = GroupedTransactionAdapter { itemRow ->
            startActivity(
                android.content.Intent(requireContext(), DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_TX_ID, itemRow.txId)
            )
        }
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = groupedAdapter

        calendarAdapter = CalendarAdapter { day ->
            viewModel.selectedDay.value = if (viewModel.selectedDay.value == day) null else day
        }
        binding.rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendar.adapter = calendarAdapter

        dayAdapter = TransactionRowAdapter { row ->
            startActivity(
                android.content.Intent(requireContext(), DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_TX_ID, row.id)
            )
        }
        binding.rvDayList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDayList.adapter = dayAdapter

        // Bộ chọn tháng
        binding.monthSelector.setMonth(viewModel.selectedMonth.value)
        binding.monthSelector.onMonthChanged = { month ->
            viewModel.selectedMonth.value = month
            viewModel.selectedDay.value = null
        }

        // Chuyển Danh sách / Lịch (mục 3.11)
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            calendarMode = checkedId == binding.btnModeCalendar.id
            applyMode()
        }

        // Chip lọc Thu/Chi (mục 3.1.5) - chỉ áp cho dạng Danh sách
        binding.chipAll.setOnClickListener { applyFilter(TxFilter.ALL) }
        binding.chipIncome.setOnClickListener { applyFilter(TxFilter.INCOME) }
        binding.chipExpense.setOnClickListener { applyFilter(TxFilter.EXPENSE) }
        applyFilter(TxFilter.ALL)
    }

    private fun applyFilter(filter: TxFilter) {
        viewModel.filter.value = filter
        binding.chipAll.setBackgroundResource(
            if (filter == TxFilter.ALL) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        binding.chipAll.setTextColor(
            requireContext().getColor(
                if (filter == TxFilter.ALL) R.color.white else R.color.text_secondary
            )
        )
        binding.chipIncome.setBackgroundResource(
            if (filter == TxFilter.INCOME) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        binding.chipIncome.setTextColor(
            requireContext().getColor(
                if (filter == TxFilter.INCOME) R.color.white else R.color.text_secondary
            )
        )
        binding.chipExpense.setBackgroundResource(
            if (filter == TxFilter.EXPENSE) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        binding.chipExpense.setTextColor(
            requireContext().getColor(
                if (filter == TxFilter.EXPENSE) R.color.white else R.color.text_secondary
            )
        )
    }

    private fun applyMode() {
        binding.calendarArea.visibility = if (calendarMode) View.VISIBLE else View.GONE
        binding.rvList.visibility = if (calendarMode) View.GONE else View.VISIBLE
        if (calendarMode) {
            rebuildCalendar()
        } else {
            // trở về danh sách: hiện/ẩn empty theo độ dài nhánh hiện tại
            binding.tvEmpty.visibility =
                if (groupedAdapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.groupedRows.collect { rows ->
                        groupedAdapter.submit(rows)
                        if (!calendarMode) {
                            binding.tvEmpty.visibility =
                                if (rows.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.monthAll.collect { all ->
                        // Tính các ô lưới lịch từ danh sách KHÔNG lọc (mục 3.11)
                        val month = viewModel.selectedMonth.value
                        val byDay = all.groupBy { DateTimeUtils.toLocalDate(it.dateTime) }
                        val incomeDays = byDay.filterValues { list ->
                            list.any { it.type == com.example.smartexpense.model.TransactionEntity.TYPE_INCOME }
                        }.keys
                        val expenseDays = byDay.filterValues { list ->
                            list.any { it.type == com.example.smartexpense.model.TransactionEntity.TYPE_EXPENSE }
                        }.keys

                        val first = month.atDay(1)
                        val leading = first.dayOfWeek.value - 1 // Thứ Hai là ngày đầu tuần
                        val cells = mutableListOf<CalendarAdapter.Cell>()
                        repeat(leading) { cells.add(CalendarAdapter.Cell(null, false, false)) }
                        for (d in 1..month.lengthOfMonth()) {
                            val date = month.atDay(d)
                            cells.add(
                                CalendarAdapter.Cell(
                                    date,
                                    date in incomeDays,
                                    date in expenseDays
                                )
                            )
                        }
                        monthCells = cells
                        if (calendarMode) rebuildCalendar()
                    }
                }
                launch {
                    viewModel.selectedDay.collect { day ->
                        rebuildCalendar()
                    }
                }
                launch {
                    viewModel.dayTransactions.collect { list ->
                        val day = viewModel.selectedDay.value
                        val income = list.filter { it.type == com.example.smartexpense.model.TransactionEntity.TYPE_INCOME }.sumOf { it.amount }
                        val expense = list.filter { it.type == com.example.smartexpense.model.TransactionEntity.TYPE_EXPENSE }.sumOf { it.amount }
                        if (day == null) {
                            binding.tvDayTitle.text = getString(R.string.list_mode_calendar)
                        } else {
                            binding.tvDayTitle.text = DateTimeUtils.formatLongDate(day) +
                                " · +" + CurrencyUtils.formatVnd(income) + " / -" + CurrencyUtils.formatVnd(expense)
                        }
                        dayAdapter.submitList(emptyList()) // reset refs trước khi submit mới
                        dayAdapter.submitList(mapRows(list))
                    }
                }
            }
        }
    }

    /** Đổ danh sách ngày đang chọn từ TransactionEntity + danh mục */
    private suspend fun mapRows(list: List<com.example.smartexpense.model.TransactionEntity>) =
        com.example.smartexpense.data.local.AppDatabase.get(requireContext())
            .categoryDao().getAllOnce()
            .let { categories ->
                val byId = categories.associateBy { it.id }
                list.map { t ->
                    val category = byId[t.categoryId]
                    com.example.smartexpense.ui.common.TxRow(
                        id = t.id,
                        title = t.note.ifBlank { category?.name ?: requireContext().getString(R.string.tx_fallback_title) },
                        subtitle = (category?.name ?: requireContext().getString(R.string.tx_fallback_title)) + " · " +
                            DateTimeUtils.formatTime(t.dateTime),
                        amount = t.amount,
                        income = t.type == com.example.smartexpense.model.TransactionEntity.TYPE_INCOME,
                        colorHex = category?.colorHex,
                        iconKey = category?.iconKey
                    )
                }
            }

    private fun rebuildCalendar() {
        calendarAdapter.submit(monthCells, viewModel.selectedDay.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
