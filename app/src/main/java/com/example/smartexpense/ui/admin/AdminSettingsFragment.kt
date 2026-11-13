package com.example.smartexpense.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.BuildConfig
import com.example.smartexpense.R
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CsvRepository
import com.example.smartexpense.databinding.FragmentAdminSettingsBinding
import com.example.smartexpense.ui.auth.AuthActivity
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.LanguageDialog
import com.example.smartexpense.utils.LanguageManager
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.utils.ThemeDialog
import com.example.smartexpense.utils.ThemeManager
import com.example.smartexpense.utils.UiUtils
import com.example.smartexpense.viewmodel.AdminViewModel
import com.example.smartexpense.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Tab Cài đặt admin (mục 6.7): avatar + email, các dòng điều hướng
 * (Quản lý danh mục / Quản lý CSV với badge thiếu-lệch / Nhật ký) + Sao lưu và Đăng xuất.
 */
class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Avatar + email của admin hiện tại (AuthRepository)
        val email = AuthRepository(requireContext().applicationContext).currentUser()?.email ?: ""
        binding.tvAdminEmail.text = email
        binding.tvAvatar.text = UiUtils.initial(email.substringBefore("@").ifEmpty { "A" })

        binding.rowCategories.setOnClickListener {
            startActivity(Intent(requireContext(), CategoryManageActivity::class.java))
        }
        binding.rowCsv.setOnClickListener {
            startActivity(Intent(requireContext(), CsvManageActivity::class.java))
        }
        binding.rowLogs.setOnClickListener {
            startActivity(Intent(requireContext(), ActivityLogActivity::class.java))
        }
        binding.rowBackup.setOnClickListener { backupCsv() }

        // Cài đặt giao diện + ngôn ngữ + API + phiên bản
        binding.tvAdminThemeValue.text = ThemeManager.label(requireContext(), viewModel.prefs.themeMode)
        binding.rowTheme.setOnClickListener {
            ThemeDialog.show(requireContext(), viewModel.prefs.themeMode) { mode ->
                ThemeManager.setMode(requireContext(), mode)
            }
        }
        // Ngôn ngữ hiển thị (mục 3.10): Theo hệ thống / Tiếng Việt / English
        binding.tvAdminLanguageValue.text = LanguageManager.currentLabel(requireContext())
        binding.rowLanguage.setOnClickListener {
            LanguageDialog.show(requireContext()) { mode ->
                LanguageManager.apply(mode) // Activity tự tạo lại với ngôn ngữ mới
            }
        }
        binding.rowSync.setOnClickListener { showSyncDialog() }
        binding.tvVersionValue.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        binding.btnLogout.setOnClickListener { confirmLogout() }

        viewModel.refreshCsv() // cập nhật badge CSV thiếu/lệch (mục 6.7)
        viewModel.refreshApiBase()
    }

    override fun onStart() {
        super.onStart()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.csvStatus.collect { status ->
                        binding.tvCsvBadge.visibility =
                            if (status != null && status.mismatch) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.apiBaseUrl.collect { url ->
                        // Firestore luôn sẵn sàng -> hiển thị "Firestore · <project id>"
                        binding.tvApiValue.text = url
                    }
                }
                launch {
                    viewModel.syncState.collect { state ->
                        when (state) {
                            is UiState.Loading -> binding.tvSyncValue.text = getString(R.string.sys_syncing)
                            is UiState.Success -> {
                                Snackbar.make(
                                    binding.root, state.data.orEmpty(), Snackbar.LENGTH_SHORT
                                ).show()
                                binding.tvSyncValue.text = formatLastSync()
                            }
                            is UiState.Error -> Snackbar.make(
                                binding.root, state.message, Snackbar.LENGTH_LONG
                            ).show()
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCsv()
        viewModel.refreshApiBase()
        binding.tvSyncValue.text = formatLastSync()
    }

    private fun formatLastSync(): String {
        val last = viewModel.prefs.lastSyncAt
        return if (last <= 0L) getString(R.string.sync_never)
        else DateTimeUtils.formatDateTime(last)
    }

    /** Chọn chiều đồng bộ với Firestore: tải hồ sơ về máy hay đẩy dữ liệu máy lên đám mây. */
    private fun showSyncDialog() {
        val options = arrayOf(
            getString(R.string.sync_pull),
            getString(R.string.sync_push)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_sync)
            .setItems(options) { _, which ->
                if (which == 0) viewModel.pullFromServer() else viewModel.pushToServer()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun backupCsv() {
        val file = CsvRepository.csvFile(requireContext())
        if (!file.exists()) {
            Snackbar.make(binding.root, getString(Messages.CSV_SHARE_FAILED), Snackbar.LENGTH_LONG).show()
            return
        }
        runCatching {
            val uri: Uri = CsvRepository.shareUri(requireContext())
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.admin_settings_backup)))
        }.onFailure {
            Snackbar.make(binding.root, getString(Messages.CSV_SHARE_FAILED), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.btn_logout)
            .setMessage(getString(R.string.app_name))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                viewModel.signOut()
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
