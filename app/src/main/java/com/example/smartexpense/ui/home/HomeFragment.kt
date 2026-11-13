package com.example.smartexpense.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.R
import com.example.smartexpense.databinding.FragmentHomeBinding
import com.example.smartexpense.adapter.TransactionRowAdapter
import com.example.smartexpense.ui.transactions.DetailActivity
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.viewmodel.HomeViewModel
import com.example.smartexpense.viewmodel.UiState
import kotlinx.coroutines.launch

/**
 * Trang chủ (mục 5.3): ngày hôm nay, chọn tháng, thẻ số dư trên nền xanh,
 * 2 ô nhỏ Thu/Chi, cảnh báo vượt hạn mức, các khoản thu/chi giao dịch gần đây.
 * Tiền tệ hiển thị cố định VND (₫) - không dùng tỷ giá động.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var recentAdapter: TransactionRowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ngày hôm nay đầu trang (mục 5.3)
        binding.tvToday.text = DateTimeUtils.formatLongDate(DateTimeUtils.today())

        recentAdapter = TransactionRowAdapter { row ->
            startActivity(
                android.content.Intent(requireContext(), DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_TX_ID, row.id)
            )
        }
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = recentAdapter

        // Bộ chọn tháng ◀ ▶ (mục 3.11)
        binding.monthSelector.setMonth(viewModel.selectedMonth.value)
        binding.monthSelector.onMonthChanged = { month ->
            viewModel.selectedMonth.value = month
        }
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.monthData.collect { renderMonth(it) } }
                launch { viewModel.budget.collect { renderBudget(it) } }
            }
        }
    }

    private fun renderMonth(data: HomeViewModel.MonthData) {
        binding.tvBalance.text = CurrencyUtils.formatVnd(data.balance)
        binding.tvIncome.text = "+" + CurrencyUtils.formatVnd(data.income)
        binding.tvExpense.text = "-" + CurrencyUtils.formatVnd(data.expense)
        recentAdapter.submitList(data.recent)
        renderBudget(viewModel.budget.value)
    }

    private fun renderBudget(budget: Long) {
        val expense = viewModel.monthData.value.expense
        if (budget <= 0) {
            binding.budgetArea.visibility = View.GONE
            binding.tvBudgetOver.visibility = View.GONE
            return
        }
        binding.budgetArea.visibility = View.VISIBLE
        val percent = ((expense * 100) / budget).toInt()
        binding.budgetProgress.progress = percent.coerceIn(0, 100)
        binding.tvBudget.text = getString(
            R.string.budget_text,
            CurrencyUtils.formatVnd(budget),
            percent.coerceIn(0, 100)
        )
        binding.tvBudgetOver.visibility =
            if (expense > budget) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
