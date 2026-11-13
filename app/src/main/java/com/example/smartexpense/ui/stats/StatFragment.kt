package com.example.smartexpense.ui.stats
import com.example.smartexpense.adapter.PieLegendAdapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.R
import com.example.smartexpense.databinding.FragmentStatBinding
import com.example.smartexpense.ui.common.PieChartView
import com.example.smartexpense.ui.common.StackedBarChartView
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.viewmodel.StatViewModel
import kotlinx.coroutines.launch

/**
 * Thống kê (mục 5.5): bộ chọn tháng, tổng thu/chi/số dư tháng,
 * biểu đồ CHI TIÊU PHÂN THEO DANH MỤC + chú giải %.
 * Nút cạnh tiêu đề cho phép chuyển giữa biểu đồ tròn (lấp đầy 360°)
 * và biểu đồ cột dọc xếp chồng; lựa chọn được lưu trong [AppPrefs].
 */
class StatFragment : Fragment() {

    private var _binding: FragmentStatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatViewModel by viewModels()

    private val legendAdapter = PieLegendAdapter()
    private val barLegendAdapter = PieLegendAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLegend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLegend.adapter = legendAdapter
        binding.rvBarLegend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBarLegend.adapter = barLegendAdapter

        binding.monthSelector.setMonth(viewModel.selectedMonth.value)
        binding.monthSelector.onMonthChanged = { month ->
            viewModel.selectedMonth.value = month
        }

        applyChartType(AppPrefs.get(requireContext()).statChartType)
        binding.btnChartPie.setOnClickListener { applyChartType(AppPrefs.CHART_PIE) }
        binding.btnChartBar.setOnClickListener { applyChartType(AppPrefs.CHART_BAR) }
    }

    /**
     * Hiện đúng khối biểu đồ + chú giải, ghi nhớ lựa chọn và tô nút pill:
     * nút đang chọn nền xanh chữ trắng, nút còn lại trong suốt.
     */
    private fun applyChartType(type: String) {
        val isBar = type == AppPrefs.CHART_BAR
        binding.sectionPie.visibility = if (isBar) View.GONE else View.VISIBLE
        binding.sectionBar.visibility = if (isBar) View.VISIBLE else View.GONE
        AppPrefs.get(requireContext()).statChartType = type

        binding.btnChartPie.setBackgroundResource(
            if (isBar) android.R.color.transparent else R.drawable.bg_chip_selected
        )
        (binding.btnChartPie.getChildAt(0) as ImageView).setColorFilter(
            requireContext().getColor(if (isBar) R.color.text_secondary else R.color.white)
        )

        binding.btnChartBar.setBackgroundResource(
            if (isBar) R.drawable.bg_chip_selected else android.R.color.transparent
        )
        (binding.btnChartBar.getChildAt(0) as ImageView).setColorFilter(
            requireContext().getColor(if (isBar) R.color.white else R.color.text_secondary)
        )
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statState.collect { state ->
                    binding.tvIncome.text = "+" + CurrencyUtils.formatVnd(state.income)
                    binding.tvExpense.text = "-" + CurrencyUtils.formatVnd(state.expense)
                    binding.tvBalance.text = CurrencyUtils.formatVnd(state.balance)

                    // Trống: tháng không có giao dịch nào (5.5)
                    val emptyMonth = state.income == 0L && state.expense == 0L
                    binding.tvEmpty.visibility = if (emptyMonth) View.VISIBLE else View.GONE
                    binding.tvNoExpense.visibility =
                        if (!emptyMonth && state.expense == 0L) View.VISIBLE else View.GONE

                    // Biểu đồ tròn + cột dọc dùng chung dữ liệu chi tiêu theo danh mục
                    val slices = state.pieSlices.map { slice ->
                        PieChartView.Slice(slice.color, slice.weight)
                    }
                    binding.pieChart.setData(slices)
                    binding.barChart.setData(
                        slices.map { StackedBarChartView.Segment(it.color, it.weight) }
                    )
                    legendAdapter.submit(state.expenseByCategory)
                    barLegendAdapter.submit(state.expenseByCategory)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
