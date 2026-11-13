package com.example.smartexpense.ui.admin
import com.example.smartexpense.adapter.AdminTransactionAdapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.R
import com.example.smartexpense.databinding.FragmentAdminTransactionBinding
import com.example.smartexpense.ui.common.TxRow
import kotlinx.coroutines.launch

/**
 * Tab Giao dịch của admin (mục 6.5): dữ liệu MINH HỌA;
 * chip lọc Tất cả / Thu / Chi / Cảnh báo (gắn cờ). KHÔNG mở chi tiết người dùng (mục 3.1.6).
 */
class AdminTransactionFragment : Fragment() {

    private var _binding: FragmentAdminTransactionBinding? = null
    private val binding get() = _binding!!

    private val adapter = AdminTransactionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAdminTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminTransactions.adapter = adapter

        renderSample(Filter.ALL)
        binding.chipAll.setOnClickListener { renderSample(Filter.ALL) }
        binding.chipIncome.setOnClickListener { renderSample(Filter.INCOME) }
        binding.chipExpense.setOnClickListener { renderSample(Filter.EXPENSE) }
        binding.chipFlagged.setOnClickListener { renderSample(Filter.FLAGGED) }
    }

    private enum class Filter { ALL, INCOME, EXPENSE, FLAGGED }

    private fun renderSample(filter: Filter) {
        val rows = SampleData.all
            .filter { tx ->
                when (filter) {
                    Filter.ALL -> true
                    Filter.INCOME -> tx.income
                    Filter.EXPENSE -> !tx.income
                    Filter.FLAGGED -> tx.flagged
                }
            }
            .map { tx ->
                AdminTransactionAdapter.AdminTxRow(
                    txRow = TxRow(
                        title = getString(tx.titleRes),
                        subtitle = "",
                        amount = tx.amount,
                        income = tx.income,
                        colorHex = tx.colorHex,
                        flagged = tx.flagged
                    ),
                    owner = getString(tx.ownerRes),
                    dateShort = tx.dateShort
                )
            }
        adapter.submit(rows)

        setChipState(Filter.ALL, filter == Filter.ALL)
        setChipState(Filter.INCOME, filter == Filter.INCOME)
        setChipState(Filter.EXPENSE, filter == Filter.EXPENSE)
        setChipState(Filter.FLAGGED, filter == Filter.FLAGGED)
    }

    private fun setChipState(channel: Filter, selected: Boolean) {
        val chip = when (channel) {
            Filter.ALL -> binding.chipAll
            Filter.INCOME -> binding.chipIncome
            Filter.EXPENSE -> binding.chipExpense
            Filter.FLAGGED -> binding.chipFlagged
        }
        chip.setBackgroundResource(
            if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        chip.setTextColor(
            requireContext().getColor(if (selected) R.color.white else R.color.text_secondary)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
