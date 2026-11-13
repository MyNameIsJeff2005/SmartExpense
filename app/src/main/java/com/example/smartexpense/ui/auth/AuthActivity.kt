package com.example.smartexpense.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartexpense.R
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.databinding.ActivityAuthBinding

/**
 * Activity duy nhất cho phần xác thực, chứa 3 Fragment:
 * LoginFragment <-> RegisterFragment <-> ForgotPasswordFragment (mục 5.1).
 * Chuyển màn hình bằng FragmentTransaction kèm animation fade/slide.
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }
    }

    /** Gọi từ Fragment để chuyển sang màn hình xác thực khác (bị chặn chỉ khi Activity không chạy) */
    fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.auth_container, fragment)
            .commit()
    }
}
