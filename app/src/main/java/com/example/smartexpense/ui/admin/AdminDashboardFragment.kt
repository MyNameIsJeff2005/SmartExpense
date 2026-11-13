package com.example.smartexpense.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.R
import com.example.smartexpense.databinding.FragmentAdminDashboardBinding
import com.example.smartexpense.ui.common.BarChartView
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tab Tổng quan (mục 6.2): lưới 2x2 số liệu THẬT từ Room
 * + biểu đồ cột "Người dùng mới 7 ngày" (cột cuối đậm, 6 cột nhạt) + pill tăng trưởng.
 */
class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refreshDashboard()
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboard.collect { data ->
                    binding.tvUsers.text = CurrencyUtils.formatCount(data.userCount)
                    binding.tvTransactions.text = CurrencyUtils.formatCount(data.txCount)
                    binding.tvActive.text = CurrencyUtils.formatCount(data.activeToday)
                    binding.tvLocked.text = CurrencyUtils.formatCount(data.lockedCount)

                    renderBarChart(data)
                }
            }
        }
    }

    private fun renderBarChart(data: AdminViewModel.DashboardData) {
        if (data.weekCounts.size != 7) return
        val today = LocalDate.now()
        val labels = (6 downTo 0).map { offset ->
            weekdayShort(today.minusDays(offset.toLong()).dayOfWeek)
        }
        val bars = data.weekCounts.mapIndexed { index, count ->
            BarChartView.Bar(
                value = count.toFloat().coerceAtLeast(0f),
                color = if (index == 6) {
                    requireContext().getColor(R.color.blue_primary)
                } else {
                    requireContext().getColor(R.color.blue_chart_muted)
                }
            )
        }
        binding.barChart.setData(bars, labels)

        // Pill tăng trưởng: % so với tuần trước, âm -> đỏ (6.2)
        val growth = data.growthPercent
        if (growth == null) {
            binding.tvGrowth.visibility = View.GONE
        } else {
            binding.tvGrowth.visibility = View.VISIBLE
            binding.tvGrowth.text = (if (growth >= 0) "+" else "") + growth + "%"
            if (growth >= 0) {
                binding.tvGrowth.setBackgroundResource(R.drawable.bg_pill_green)
                binding.tvGrowth.setTextColor(requireContext().getColor(R.color.green))
            } else {
                binding.tvGrowth.setBackgroundResource(R.drawable.bg_pill_red)
                binding.tvGrowth.setTextColor(requireContext().getColor(R.color.red))
            }
        }
    }

    private fun weekdayShort(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "T2"
        DayOfWeek.TUESDAY -> "T3"
        DayOfWeek.WEDNESDAY -> "T4"
        DayOfWeek.THURSDAY -> "T5"
        DayOfWeek.FRIDAY -> "T6"
        DayOfWeek.SATURDAY -> "T7"
        DayOfWeek.SUNDAY -> "CN"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
