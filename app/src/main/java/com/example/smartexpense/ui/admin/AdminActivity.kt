package com.example.smartexpense.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ActivityAdminBinding
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.repository.AuthRepository
import com.google.android.material.snackbar.Snackbar

/**
 * Khối Quản trị (mục 6.1): Bottom Navigation 4 tab
 * (Tổng quan / Người dùng / Giao dịch minh họa / Cài đặt).
 * Vào được chỉ khi tài khoản hiện tại là admin (mục 3.2c).
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.admin_container, fragment)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = AuthRepository(this)
        // Rào cản: chỉ admin được vào phần quản trị (mục 6, 3.2c)
        if (!auth.isCurrentUserAdmin()) {
            Snackbar.make(window.decorView, getString(R.string.admin_only_msg), Snackbar.LENGTH_LONG)
                .show()
            finish()
            return
        }

        binding = ActivityAdminBinding.inflate(layoutInflater)
        // Cập nhật locale cho định dạng thời gian (mục 3.10)
        DateTimeUtils.syncLocale(this)
        setContentView(binding.root)
        applySystemBarInsets(binding.root, binding.adminContainer, binding.adminNav)

        if (savedInstanceState == null) {
            showFragment(AdminDashboardFragment())
        }

        binding.adminNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> showFragment(AdminDashboardFragment())
                R.id.nav_users -> showFragment(UserListFragment())
                R.id.nav_transactions -> showFragment(AdminTransactionFragment())
                R.id.nav_settings -> showFragment(AdminSettingsFragment())
            }
            true
        }
    }
}
