package com.example.smartexpense.ui.changelog
import com.example.smartexpense.adapter.ChangelogAdapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.databinding.ActivityChangelogBinding
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.applySystemBarInsets

/**
 * Nhật ký thay đổi (mục 5.8 - nhóm Giới thiệu).
 *
 * Hiển thị lịch sử cập nhật từ phiên bản 1.0.0 đến phiên bản hiện tại
 * (lấy từ [ChangelogData]); nội dung tự đổi ngôn ngữ theo cài đặt app.
 */
class ChangelogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangelogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangelogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)
        DateTimeUtils.syncLocale(this)

        binding.btnBack.setOnClickListener { finish() }

        val adapter = ChangelogAdapter(ChangelogData.versions)
        binding.rvVersions.layoutManager = LinearLayoutManager(this)
        binding.rvVersions.adapter = adapter
        binding.rvVersions.addItemDecoration(
            ChangelogAdapter.TimelineDecoration(this, adapter)
        )
    }
}
