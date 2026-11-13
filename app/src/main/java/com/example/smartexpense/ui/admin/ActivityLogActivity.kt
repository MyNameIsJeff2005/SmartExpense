package com.example.smartexpense.ui.admin
import com.example.smartexpense.adapter.ActivityLogAdapter

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.databinding.ActivityActivityLogBinding
import com.example.smartexpense.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

/**
 * Nhật ký hoạt động (mục 6.7): đọc AdminLog từ Room (mới nhất trên cùng).
 */
class ActivityLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivityLogBinding
    private val viewModel: AdminViewModel by viewModels()

    private val adapter = ActivityLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logs.collect { logs ->
                    adapter.submit(logs)
                    binding.tvEmpty.visibility =
                        if (logs.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
