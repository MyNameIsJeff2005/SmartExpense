package com.example.smartexpense.ui.profile

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.R
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.repository.BackupRepository
import com.example.smartexpense.databinding.DialogBudgetBinding
import com.example.smartexpense.databinding.FragmentProfileBinding
import com.example.smartexpense.ui.admin.AdminActivity
import com.example.smartexpense.ui.auth.AuthActivity
import com.example.smartexpense.ui.changelog.ChangelogActivity
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.AvatarStore
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.LanguageDialog
import com.example.smartexpense.utils.LanguageManager
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.utils.ThemeDialog
import com.example.smartexpense.utils.ThemeManager
import com.example.smartexpense.utils.UiUtils
import com.example.smartexpense.viewmodel.ProfileViewModel
import com.example.smartexpense.viewmodel.RatesViewModel
import com.example.smartexpense.viewmodel.UiState
import com.example.smartexpense.model.ExchangeRateEntity
import com.example.smartexpense.model.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Tab Cài đặt (mục 5.8, 3.7, 3.10) theo bố cục 4 nhóm:
 * Tài khoản (mở màn chỉnh sửa), Cài đặt (ngôn ngữ, thông báo, hạn mức, chế độ tối, trợ giúp),
 * Dữ liệu (đồng bộ server, sao lưu, khôi phục, tự động sao lưu, đường dẫn sao lưu),
 * Giới thiệu (phiên bản, Github) và Đăng xuất.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    /** Phân hệ Tỷ giá USD→VND (v1.4): ViewModel riêng, 3 trạng thái Resource. */
    private val ratesViewModel: RatesViewModel by viewModels()

    /** Chặn callback của công tắc khi đồng bộ trạng thái từ dữ liệu. */
    private var suppressThemeCallback = false
    private var suppressAutoBackupCallback = false

    private val prefs: AppPrefs by lazy { AppPrefs.get(requireContext()) }

    /** Chọn thư mục sao lưu (SAF - quyền được giữ lại sau khi khởi động lại máy). */
    private val pickBackupFolder = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        prefs.backupTreeUri = uri.toString()
        renderBackupPath()
        view?.let { Snackbar.make(it, getString(R.string.backup_path_saved), Snackbar.LENGTH_SHORT).show() }
    }

    /** Chọn tệp CSV để khôi phục (chỉ admin). */
    private val pickRestoreFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) restoreNow(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tài khoản: mở màn xem/sửa thông tin cá nhân (ảnh, tên, giới tính, tuổi, email)
        binding.rowAccount.setOnClickListener {
            startActivity(Intent(requireContext(), AccountActivity::class.java))
        }

        // Hàng Quản trị chỉ hiện với tài khoản admin (mục 3.2c, 2)
        binding.rowAdmin.visibility = if (viewModel.isAdmin) View.VISIBLE else View.GONE
        binding.rowAdmin.setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
        }

        binding.rowBudget.setOnClickListener { showBudgetDialog() }
        binding.rowSync.setOnClickListener { confirmSync() }
        binding.rowBackup.setOnClickListener { confirmBackup() }
        binding.rowRestore.setOnClickListener { confirmRestore() }
        binding.rowAutoBackup.setOnClickListener { binding.switchAutoBackup.toggle() }
        binding.rowBackupPath.setOnClickListener { showBackupPathDialog() }
        binding.rowNotifications.setOnClickListener { openNotificationSettings() }
        binding.rowHelp.setOnClickListener { showHelp() }
        binding.rowAboutApp.setOnClickListener { showAbout() }
        binding.rowChangelog.setOnClickListener {
            startActivity(Intent(requireContext(), ChangelogActivity::class.java))
        }
        binding.rowGithub.setOnClickListener { openGithub() }

        // Tỷ giá USD→VND (v1.4): bấm dòng = tải lại từ API
        binding.rowRates.setOnClickListener { ratesViewModel.refresh() }

        // Tự động sao lưu + đường dẫn sao lưu (nhóm Dữ liệu)
        renderAutoBackup(prefs.autoBackup)
        binding.switchAutoBackup.setOnCheckedChangeListener { _, checked ->
            if (suppressAutoBackupCallback) return@setOnCheckedChangeListener
            prefs.autoBackup = checked
            Snackbar.make(
                binding.root,
                getString(if (checked) R.string.auto_backup_on else R.string.auto_backup_off),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        renderBackupPath()

        // Ngôn ngữ hiển thị (mục 3.10): Theo hệ thống / Tiếng Việt / English
        binding.tvLanguageValue.text = LanguageManager.currentLabel(requireContext())
        binding.rowLanguage.setOnClickListener {
            LanguageDialog.show(requireContext()) { mode ->
                LanguageManager.apply(mode) // Activity tự tạo lại với ngôn ngữ mới
            }
        }

        // Chế độ sáng / tối: công tắc đổi nhanh Sáng <-> Tối, bấm cả dòng để chọn "Theo hệ thống"
        renderTheme(viewModel.themeMode.value)
        binding.switchTheme.setOnCheckedChangeListener { _, checked ->
            if (suppressThemeCallback) return@setOnCheckedChangeListener
            viewModel.setThemeMode(
                requireContext(),
                if (checked) ThemeManager.MODE_DARK else ThemeManager.MODE_LIGHT
            )
        }
        binding.rowTheme.setOnClickListener {
            ThemeDialog.show(requireContext(), viewModel.themeMode.value) { mode ->
                viewModel.setThemeMode(requireContext(), mode)
            }
        }

        // Phiên bản app (đọc từ BuildConfig) + đồng bộ server API
        binding.tvVersionValue.text = getString(R.string.about_version, viewModel.versionName)
        binding.tvChangelogDesc.text = getString(R.string.settings_changelog_desc)
        binding.tvGithubValue.setText(R.string.settings_github_link)

        binding.btnLogout.setOnClickListener { confirmLogout() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profile.collect { profile -> renderAccount(profile) }
                }
                launch {
                    viewModel.budget.collect {
                        binding.tvBudgetValue.text =
                            if (it > 0) CurrencyUtils.formatVnd(it) else "—"
                    }
                }
                launch {
                    viewModel.themeMode.collect { mode -> renderTheme(mode) }
                }
                launch {
                    viewModel.lastSyncAt.collect { ts ->
                        binding.tvSyncValue.text = if (ts <= 0L) {
                            getString(R.string.sync_never)
                        } else {
                            DateTimeUtils.formatDateTime(ts)
                        }
                    }
                }
                launch {
                    viewModel.syncState.collect { state ->
                        when (state) {
                            is UiState.Loading -> binding.tvSyncValue.text = getString(R.string.sys_syncing)
                            is UiState.Success -> Snackbar.make(
                                binding.root, state.data.orEmpty(), Snackbar.LENGTH_SHORT
                            ).show()
                            is UiState.Error -> Snackbar.make(
                                binding.root, state.message, Snackbar.LENGTH_LONG
                            ).show()
                            else -> Unit
                        }
                    }
                }
                launch {
                    // Phân hệ Tỷ giá: Loading = ProgressBar, Success = giá trị,
                    // Error = thông báo lỗi + nút thử lại (v1.4)
                    ratesViewModel.rate.collect { resource ->
                        renderRate(resource)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Tên/ảnh có thể vừa được sửa ở màn Account, thư mục sao lưu có thể vừa đổi
        viewModel.reload()
        _binding?.let {
            renderBackupPath()
            renderAutoBackup(prefs.autoBackup)
        }
    }

    /** 3 trạng thái Resource của phân hệ Tỷ giá USD→VND (v1.4). */
    private fun renderRate(resource: Resource<ExchangeRateEntity>) {
        when (resource) {
            is Resource.Loading -> {
                binding.progressRates.visibility = View.VISIBLE
                binding.ivRatesRetry.visibility = View.GONE
                binding.tvRatesValue.text = "…"
                binding.tvRatesDesc.text = getString(R.string.settings_rates_desc)
            }
            is Resource.Success -> {
                binding.progressRates.visibility = View.GONE
                binding.ivRatesRetry.visibility = View.GONE
                val rate = resource.data
                binding.tvRatesValue.text = CurrencyUtils.formatVnd(rate.rate.toLong())
                binding.tvRatesDesc.text = getString(
                    R.string.rates_updated,
                    DateTimeUtils.formatDateTime(rate.fetchedAt)
                )
            }
            is Resource.Error -> {
                binding.progressRates.visibility = View.GONE
                binding.ivRatesRetry.visibility = View.VISIBLE
                binding.tvRatesValue.text = "—"
                binding.tvRatesDesc.text = getString(R.string.rates_error)
            }
        }
    }

    /** Hiển thị tên + ảnh đại diện ở thẻ Tài khoản. */
    private fun renderAccount(profile: UserProfile?) {
        if (profile == null) return
        binding.tvName.text = profile.displayName

        val bitmap = AvatarStore.load(AppPrefs.get(requireContext()).avatarPath)
        if (bitmap != null) {
            binding.imgAvatar.setImageBitmap(bitmap)
            binding.imgAvatar.visibility = View.VISIBLE
            binding.tvAvatar.visibility = View.GONE
        } else {
            binding.imgAvatar.visibility = View.GONE
            binding.tvAvatar.visibility = View.VISIBLE
            binding.tvAvatar.text = UiUtils.initial(profile.displayName)
        }
    }

    /** Đồng bộ công tắc + nhãn Bật/Tắt theo chế độ đang lưu (kể cả "Theo hệ thống"). */
    private fun renderTheme(mode: String) {
        val dark = when (mode) {
            ThemeManager.MODE_DARK -> true
            ThemeManager.MODE_LIGHT -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        suppressThemeCallback = true
        binding.switchTheme.isChecked = dark
        suppressThemeCallback = false
        binding.tvThemeValue.setText(if (dark) R.string.settings_on else R.string.settings_off)
    }

    /** Hộp thoại hạn mức chi tiêu tháng (mục 3.7), lưu vào EncryptedSharedPreferences */
    private fun showBudgetDialog() {
        val dialogBinding = DialogBudgetBinding.inflate(layoutInflater)
        val current = viewModel.budget.value
        if (current > 0) {
            dialogBinding.edtBudget.setText(current.toString())
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.budget_dialog_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                val value = dialogBinding.edtBudget.text.toString().trim().toLongOrNull() ?: 0L
                viewModel.saveBudget(value.coerceAtLeast(0L))
            }
            .show()
    }

    /** Mở cài đặt thông báo của app trong hệ thống Android. */
    private fun openNotificationSettings() {
        val context = requireContext()
        val direct = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        val opened = runCatching { startActivity(direct) }.isSuccess
        if (opened) return

        val fallback = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        if (runCatching { startActivity(fallback) }.isFailure) {
            Snackbar.make(
                binding.root,
                getString(R.string.settings_notifications_unavailable),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /** Trợ giúp nhanh ngay trong app. */
    private fun showHelp() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_help_title)
            .setMessage(R.string.settings_help_msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** Thẻ Giới thiệu: tên app + phiên bản hiện tại. */
    private fun showAbout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.app_name)
            .setMessage(
                getString(R.string.about_app_msg) + "\n\n" +
                    getString(R.string.about_version, viewModel.versionName)
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openGithub() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
        if (runCatching { startActivity(intent) }.isFailure) {
            Snackbar.make(
                binding.root,
                getString(R.string.settings_github_unavailable),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /** Đồng bộ công tắc "Tự động sao lưu" theo giá trị đã lưu. */
    private fun renderAutoBackup(enabled: Boolean) {
        suppressAutoBackupCallback = true
        binding.switchAutoBackup.isChecked = enabled
        suppressAutoBackupCallback = false
    }

    /** Hiển thị thư mục sao lưu hiện tại (mặc định = thư mục riêng của app). */
    private fun renderBackupPath() {
        val custom = !BackupRepository.isDefaultPath(prefs.backupTreeUri)
        val path = BackupRepository.displayPath(requireContext(), prefs.backupTreeUri)
        binding.tvBackupPathValue.text = if (custom) path else {
            getString(R.string.settings_backup_path_default) + " · " + shortPath(path)
        }
    }

    /** Rút gọn đường dẫn dài còn 2 thư mục cuối, vd "…/files/backups". */
    private fun shortPath(path: String): String {
        val parts = path.trimEnd('/').split('/').filter { it.isNotEmpty() }
        return if (parts.size <= 3) path else "…/" + parts.takeLast(2).joinToString("/")
    }

    /** Thông báo cấp quyền dữ liệu trước khi app ghi/đọc tệp (mục 3.2c). */
    private fun confirmBackup() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.csv_permission_title)
            .setMessage(R.string.csv_permission_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_continue) { _, _ -> backupNow() }
            .show()
    }

    /**
     * Sao lưu: dựng lại accounts.csv từ Room rồi ghi một tệp mới vào thư mục sao lưu
     * (thư mục người dùng chọn qua SAF, hoặc thư mục mặc định của app).
     */
    private fun backupNow() {
        val appContext = requireContext().applicationContext
        val treeUri = prefs.backupTreeUri
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { BackupRepository.backup(appContext, treeUri) }
            if (_binding == null) return@launch
            result.onSuccess { path ->
                prefs.lastBackupAt = System.currentTimeMillis()
                Snackbar.make(
                    binding.root,
                    getString(R.string.backup_done, path),
                    Snackbar.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.backup_failed_msg, error.message.orEmpty()),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Khôi phục chỉ dành cho admin (dữ liệu gộp vào accounts.csv dùng chung). */
    private fun confirmRestore() {
        if (!viewModel.isAdmin) {
            Snackbar.make(binding.root, getString(R.string.admin_only_msg), Snackbar.LENGTH_LONG)
                .show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.csv_permission_title)
            .setMessage(R.string.csv_permission_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_continue) { _, _ ->
                pickRestoreFile.launch(
                    arrayOf(
                        "text/csv",
                        "text/comma-separated-values",
                        "text/plain",
                        "application/csv",
                        "application/vnd.ms-excel",
                        "application/octet-stream"
                    )
                )
            }
            .show()
    }

    /** Gộp tệp CSV đã chọn vào Room rồi dựng lại accounts.csv. */
    private fun restoreNow(uri: Uri) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { BackupRepository.restore(appContext, uri) }
            if (_binding == null) return@launch
            result.onSuccess { summary ->
                Snackbar.make(
                    binding.root,
                    getString(
                        R.string.restore_done,
                        summary.inserted,
                        summary.updated,
                        summary.skipped
                    ),
                    Snackbar.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.restore_failed, error.message.orEmpty()),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Hộp thoại đường dẫn sao lưu: đổi thư mục (SAF) hoặc quay về mặc định. */
    private fun showBackupPathDialog() {
        val custom = !BackupRepository.isDefaultPath(prefs.backupTreeUri)
        val current = BackupRepository.displayPath(requireContext(), prefs.backupTreeUri)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_backup_path)
            .setMessage(getString(R.string.backup_path_dialog_msg, current))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.backup_path_change) { _, _ -> pickBackupFolder.launch(null) }
        if (custom) {
            dialog.setNeutralButton(R.string.backup_path_reset) { _, _ ->
                prefs.backupTreeUri = ""
                renderBackupPath()
            }
        }
        dialog.show()
    }

    /** Đồng bộ hồ sơ lên Firestore (chỉ gửi tên/email/ngày tạo, không gửi mật khẩu). */
    private fun confirmSync() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_sync)
            .setMessage(R.string.sync_confirm_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.sync_confirm_now) { _, _ -> viewModel.syncNow() }
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.btn_logout)
            .setMessage(R.string.logout_confirm_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                viewModel.logout()
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

    private companion object {
        const val GITHUB_URL = "https://github.com/"
    }
}
