package com.example.smartexpense.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.R
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.databinding.ActivityUserDetailBinding
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.utils.UiUtils
import com.example.smartexpense.viewmodel.AdminViewModel
import com.example.smartexpense.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Chi tiết người dùng (mục 6.4): Khóa/mở khóa (chỉ cờ Room, xác nhận 2 bước "dễ",
 * không khóa chính mình/admin) + Đặt lại mật khẩu (gửi email Firebase - admin không bao giờ biết mật khẩu).
 */
class UserDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UID = "extra_uid"
    }

    private lateinit var binding: ActivityUserDetailBinding
    private val viewModel: AdminViewModel by viewModels()

    private var user: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val uid = intent.getStringExtra(EXTRA_UID) ?: run {
            finish(); return
        }
        viewModel.loadUser(uid)

        binding.btnLock.setOnClickListener { onLockClicked() }
        binding.btnResetPassword.setOnClickListener { onResetPasswordClicked() }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedUser.collect { profile ->
                        user = profile ?: return@collect
                        render(profile)
                    }
                }
                launch {
                    viewModel.selectedUserTxCount.collect { count ->
                        binding.rowTxCount.tvValue.text = count.toString()
                    }
                }
                launch {
                    viewModel.resetState.collect { state ->
                        binding.btnResetPassword.isEnabled = state !is UiState.Loading
                        when (state) {
                            is UiState.Success -> {
                                Snackbar.make(binding.root, getString(Messages.RESET_SENT), Snackbar.LENGTH_LONG).show()
                                viewModel.resetState.value = UiState.Idle
                            }
                            is UiState.Error -> {
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                                viewModel.resetState.value = UiState.Idle
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun render(profile: UserProfile) {
        binding.tvAvatar.text = UiUtils.initial(profile.displayName)
        binding.tvName.text = profile.displayName
        binding.tvEmail.text = profile.email

        val locked = profile.status == UserProfile.STATUS_LOCKED
        binding.tvStatus.text = getString(
            if (locked) R.string.status_locked else R.string.status_active
        )
        binding.tvStatus.setBackgroundResource(
            if (locked) R.drawable.bg_pill_red else R.drawable.bg_pill_green
        )
        binding.tvStatus.setTextColor(
            getColor(if (locked) R.color.red else R.color.green)
        )
        binding.tvStatus.visibility = android.view.View.VISIBLE

        binding.rowCreated.tvLabel.setText(R.string.user_created_at)
        binding.rowCreated.tvValue.text = DateTimeUtils.formatDateTime(profile.createdAt)
        binding.rowLastLogin.tvLabel.setText(R.string.user_last_login)
        binding.rowLastLogin.tvValue.text =
            if (profile.lastLoginAt > 0) DateTimeUtils.formatDateTime(profile.lastLoginAt) else "—"
        binding.rowRole.tvLabel.setText(R.string.user_role_row)
        binding.rowRole.tvValue.setText(
            if (profile.role == UserProfile.ROLE_ADMIN) R.string.user_role_admin else R.string.user_role_user
        )

        // Nút khóa: đổi nhãn theo trạng thái; ẩn khi là admin hoặc chính mình (mục 6.4)
        val selfUid = AuthRepository(this).currentUserUid()
        val lockable = profile.role != UserProfile.ROLE_ADMIN && profile.uid != selfUid
        binding.btnLock.text = getString(if (locked) R.string.btn_unlock else R.string.btn_lock)
        binding.btnLock.visibility = if (lockable) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun onLockClicked() {
        val target = user ?: return
        val locked = target.status == UserProfile.STATUS_LOCKED
        MaterialAlertDialogBuilder(this)
            .setTitle(
                if (locked) R.string.unlock_confirm_title else R.string.lock_confirm_title
            )
            .setMessage(
                if (locked) R.string.unlock_confirm_msg else R.string.lock_confirm_msg
            )
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                viewModel.toggleLock(target)
                Snackbar.make(
                    binding.root,
                    if (locked) getString(Messages.UNLOCKED_TOAST) else getString(Messages.LOCKED_TOAST),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun onResetPasswordClicked() {
        val target = user ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_reset_password)
            .setMessage(getString(R.string.reset_password_confirm) + "\n" + target.email)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                viewModel.resetPassword(target.email)
            }
            .show()
    }
}
