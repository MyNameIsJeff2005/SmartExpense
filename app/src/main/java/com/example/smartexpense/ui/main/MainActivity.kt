package com.example.smartexpense.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartexpense.R
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.databinding.ActivityMainBinding
import com.example.smartexpense.ui.auth.AuthActivity
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.ui.home.HomeFragment
import com.example.smartexpense.ui.profile.ProfileFragment
import com.example.smartexpense.ui.stats.StatFragment
import com.example.smartexpense.ui.transactions.AddEditTransactionActivity
import com.example.smartexpense.ui.transactions.ListFragment

/**
 * Màn hình chính: Bottom Navigation 4 tab (mục 2, 5.2).
 * Mở app mà chưa có phiên đăng nhập thì chuyển ngay về [AuthActivity].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hasActiveSession: phiên Firebase thật HOẶC phiên DEMO (không cần cấu hình Firebase).
        // runCatching để phòng trường hợp google-services.json placeholder — app không crash.
        val hasSession = runCatching { AuthRepository(this).hasActiveSession() }.getOrDefault(false)
        if (!hasSession) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Android 15+: chừa khoảng thanh trạng thái cho nội dung, thanh điều hướng cho bottom nav
        applySystemBarInsets(binding.root, binding.fragmentContainer, binding.bottomNav)

        // Nhặt lại locale hiện hành (có thể vừa đổi ngôn ngữ trong Cài đặt - mục 3.10)
        DateTimeUtils.syncLocale(this)

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(HomeFragment())
                R.id.nav_transactions -> showFragment(ListFragment())
                R.id.nav_stats -> showFragment(StatFragment())
                R.id.nav_profile -> showFragment(ProfileFragment())
            }
            updateFabVisibility(item.itemId)
            true
        }

        // Quay lại sau khi xoay màn hình: cập nhật FAB theo tab đang chọn
        updateFabVisibility(binding.bottomNav.selectedItemId)

        // Nút (+): mở màn Thêm giao dịch (mục 5.6); fragment tự cập nhật nhờ Room Flow
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddEditTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Sau khi Activity bị tạo lại (đổi ngôn ngữ/chế độ tối), BottomNavigation khôi phục
        // tab đã chọn SAU onCreate nên phải đồng bộ lại FAB ở đây.
        if (::binding.isInitialized) updateFabVisibility(binding.bottomNav.selectedItemId)
    }

    /** FAB (+) chỉ hiện ở tab Trang chủ và Giao dịch (mục 5.2) */
    private fun updateFabVisibility(tabId: Int) {
        binding.fabAdd.visibility = if (tabId == R.id.nav_home || tabId == R.id.nav_transactions) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
