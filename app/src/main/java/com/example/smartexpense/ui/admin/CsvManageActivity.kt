package com.example.smartexpense.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.smartexpense.R
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.repository.CsvRepository
import com.example.smartexpense.databinding.ActivityCsvManageBinding
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.viewmodel.AdminViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Quản lý file CSV (mục 3.2c): chỉ admin thấy.
 * - Xem trạng thái (tồn tại / kích thước / dòng / sửa lần cuối / đếm Room)
 * - Dựng lại từ Room (xác nhận, không duplicate)
 * - Xóa file (xác nhận 2 BƯỚC)
 * - Công tắc "Tự động tạo lại CSV khi file bị xóa"
 * - XUẤT CSV ra file do admin chọn và NHẬP CSV từ file (Storage Access Framework)
 */
class CsvManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCsvManageBinding
    private val viewModel: AdminViewModel by viewModels()

    /** Xuất CSV: Android mở trình chọn nơi lưu, không cần quyền bộ nhớ. */
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportCsvTo(uri)
    }

    /** Nhập CSV: Android mở trình chọn file có sẵn. */
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importCsvFrom(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCsvManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        // Nhật lục file CSV hiển theo ngôn ngữ đã chọn (mục 3.10)
        DateTimeUtils.syncLocale(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnRebuild.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.rebuild_confirm_title)
                .setMessage(R.string.rebuild_confirm_msg)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok) { _, _ -> viewModel.rebuildCsv() }
                .show()
        }

        // Xóa file CSV: xác nhận 2 bước (mục 3.2c)
        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_csv_confirm_title_1)
                .setMessage(R.string.delete_csv_confirm_msg_1)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok) { _, _ ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete_csv_confirm_title_2)
                        .setMessage(R.string.delete_csv_confirm_msg_2)
                        .setNegativeButton(R.string.btn_cancel, null)
                        .setPositiveButton(R.string.btn_delete) { _, _ ->
                            viewModel.deleteCsv()
                            Snackbar.make(binding.root, getString(Messages.CSV_DELETED), Snackbar.LENGTH_SHORT).show()
                        }
                        .show()
                }
                .show()
        }

        // Xuất CSV: đặt tên gợi ý accounts_YYYYMMDD_HHmm.csv
        // Trước khi mở trình chọn tệp, hiển thị thông báo cấp quyền dữ liệu (mục 3.2c)
        binding.btnExportFile.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.csv_permission_title)
                .setMessage(getString(R.string.csv_permission_msg))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_continue) { _, _ ->
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    exportLauncher.launch("accounts_$stamp.csv")
                }
                .show()
        }

        // Nhập CSV: thông báo cấp quyền dữ liệu -> xác nhận gộp dữ liệu vào Room
        binding.btnImportFile.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.csv_permission_title)
                .setMessage(getString(R.string.csv_permission_msg))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_continue) { _, _ -> confirmImport() }
                .show()
        }

        binding.switchAuto.setOnCheckedChangeListener { _, checked ->
            viewModel.setAutoRecreate(checked)
        }

        viewModel.refreshCsv()
    }

    /** Xác nhận các bước dữ liệu cuối cùng trước khi mở trình chọn tệp. */
    private fun confirmImport() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.csv_import_confirm_title)
            .setMessage(R.string.csv_import_confirm_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                importLauncher.launch(arrayOf("text/*", "application/csv"))
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.csvStatus.collect { status ->
                        if (status != null) render(status)
                    }
                }
                launch {
                    viewModel.csvEvent.collect { event ->
                        if (event == null) return@collect
                        showEvent(event)
                        viewModel.consumeCsvEvent()
                    }
                }
            }
        }
    }

    private fun showEvent(event: AdminViewModel.CsvEvent) {
        when (event) {
            is AdminViewModel.CsvEvent.Exported ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.csv_export_done, event.rows),
                    Snackbar.LENGTH_LONG
                ).show()

            is AdminViewModel.CsvEvent.Imported -> {
                val text = if (event.inserted + event.updated == 0) {
                    getString(Messages.CSV_IMPORT_EMPTY)
                } else {
                    getString(
                        R.string.csv_import_done,
                        event.inserted, event.updated, event.skipped
                    )
                }
                Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
            }

            is AdminViewModel.CsvEvent.Failed ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.csv_io_failed, event.message),
                    Snackbar.LENGTH_LONG
                ).show()
        }
    }

    private fun render(status: CsvRepository.CsvStatus) {
        // Công tắc - giữ trạng thái không vòng lặp khi set
        binding.switchAuto.setOnCheckedChangeListener(null)
        binding.switchAuto.isChecked = viewModel.prefs.autoRecreateCsv
        binding.switchAuto.setOnCheckedChangeListener { _, checked ->
            viewModel.setAutoRecreate(checked)
        }
        binding.tvSwitchDesc.setText(
            if (viewModel.prefs.autoRecreateCsv) R.string.switch_desc_on else R.string.switch_desc_off
        )

        // Trạng thái file
        if (status.exists) {
            binding.tvExists.setText(R.string.csv_exists)
            binding.tvExists.setBackgroundResource(R.drawable.bg_pill_green)
            binding.tvExists.setTextColor(getColor(R.color.green))

            binding.rowSize.tvLabel.setText(R.string.csv_size)
            binding.rowSize.tvValue.text = formatSize(status.sizeBytes)
            binding.rowRows.tvLabel.setText(R.string.csv_rows)
            binding.rowRows.tvValue.text = status.dataRows.toString()
            binding.rowModified.tvLabel.setText(R.string.csv_modified)
            binding.rowModified.tvValue.text = DateTimeUtils.formatDateTime(status.lastModified)
        } else {
            binding.tvExists.setText(R.string.csv_missing)
            binding.tvExists.setBackgroundResource(R.drawable.bg_pill_red)
            binding.tvExists.setTextColor(getColor(R.color.red))

            binding.rowSize.tvLabel.setText(R.string.csv_size)
            binding.rowSize.tvValue.text = "—"
            binding.rowRows.tvLabel.setText(R.string.csv_rows)
            binding.rowRows.tvValue.text = "—"
            binding.rowModified.tvLabel.setText(R.string.csv_modified)
            binding.rowModified.tvValue.text = "—"
        }

        binding.rowRoom.tvLabel.setText(R.string.csv_room_count)
        binding.rowRoom.tvValue.text = status.roomCount.toString()

        // OK / Cảnh báo (mục 3.2c)
        if (!status.mismatch) {
            binding.tvOk.visibility = View.VISIBLE
            binding.tvWarning.visibility = View.GONE
        } else {
            binding.tvOk.visibility = View.GONE
            binding.tvWarning.visibility = View.VISIBLE
            binding.tvWarning.setText(
                if (!status.exists) R.string.csv_warning_missing else R.string.csv_warning_mismatch
            )
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        return String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    }
}
