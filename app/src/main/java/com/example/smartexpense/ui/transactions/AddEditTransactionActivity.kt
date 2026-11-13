package com.example.smartexpense.ui.transactions
import com.example.smartexpense.adapter.CategoryGridAdapter

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartexpense.R
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.databinding.ActivityAddEditBinding
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.model.TransactionEntity
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.viewmodel.AddEditTransactionViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Thêm/Sửa giao dịch (mục 5.6): chỉ có bàn phím số; Số tiền và điểm giao dịch đều là
 * entered digits; cập nhật lại UI mỗi lần bấm nút; ngày/giờ dùng Material pickers.
 * Chọn danh mục quyết định LOẠI giao dịch (chi/thu theo category.type - mục 3.1.4).
 */
class AddEditTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TX_ID = "extra_tx_id"
    }

    private lateinit var binding: ActivityAddEditBinding
    private val viewModel: AddEditTransactionViewModel by viewModels()

    private lateinit var gridAdapter: CategoryGridAdapter

    private var editingTxId: Long = 0L
    private var isNew = true

    private var amountDigits = ""        // chỉ số thô; format khi hiển thị (5.6.2)
    private var selectedCategory: CategoryEntity? = null
    private var selectedDate: LocalDate = DateTimeUtils.today()
    private var selectedTime: LocalTime = LocalTime.now().withSecond(0).withNano(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        editingTxId = intent.getLongExtra(EXTRA_TX_ID, 0L)
        isNew = editingTxId == 0L
        binding.tvTitle.setText(if (isNew) R.string.add_edit_add_title else R.string.add_edit_edit_title)

        gridAdapter = CategoryGridAdapter { category ->
            selectedCategory = category
        }
        binding.rvCategories.layoutManager = GridLayoutManager(this, 4)
        binding.rvCategories.adapter = gridAdapter

        setupKeypad()
        setupPickers()

        binding.btnClose.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener { attemptSave() }

        if (!isNew) loadForEdit()
        refreshAmountText()
        refreshDateButtons()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        gridAdapter.submit(categories, selectedCategory?.id ?: -1L)
                        // Với sửa: chọn lại danh mục của giao dịch đang sửa (nếu vẫn còn tồn tại)
                        if (!isNew && selectedCategory == null) {
                            editingTx?.let { tx ->
                                categories.firstOrNull { it.id == tx.categoryId }?.let {
                                    selectedCategory = it
                                    gridAdapter.setSelectedId(it.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private var editingTx: TransactionEntity? = null

    private fun loadForEdit() {
        lifecycleScope.launch {
            val tx = viewModel.loadTransaction(editingTxId) ?: run {
                Snackbar.make(binding.root, getString(Messages.TX_LOAD_FAILED), Snackbar.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            editingTx = tx
            amountDigits = tx.amount.toString()
            selectedDate = DateTimeUtils.toLocalDate(tx.dateTime)
            selectedTime = Instant.ofEpochMilli(tx.dateTime).atZone(ZoneId.systemDefault())
                .toLocalTime().withSecond(0).withNano(0)
            binding.edtNote.setText(tx.note)
            // Chọn lại danh mục của giao dịch đang sửa (từ danh mục đã có trong VM)
            viewModel.categories.value.firstOrNull { it.id == tx.categoryId }?.let {
                selectedCategory = it
                gridAdapter.setSelectedId(it.id)
            }
            refreshAmountText()
            refreshDateButtons()
        }
    }

    // ==================== Bàn phím số (mục 5.6.5, 5.6.2) ====================

    private fun setupKeypad() {
        val digits = mapOf(
            binding.pad1 to "1", binding.pad2 to "2", binding.pad3 to "3",
            binding.pad4 to "4", binding.pad5 to "5", binding.pad6 to "6",
            binding.pad7 to "7", binding.pad8 to "8", binding.pad9 to "9",
            binding.pad0 to "0"
        )
        digits.forEach { (view, d) ->
            view.setOnClickListener { appendDigit(d) }
        }
        binding.pad000.setOnClickListener { appendDigit("000") }
        binding.padDel.setOnClickListener {
            if (amountDigits.isNotEmpty()) {
                amountDigits = amountDigits.dropLast(1)
                refreshAmountText()
            }
        }
    }

    private fun appendDigit(d: String) {
        // không quá 12 chữ số (~999 tỷ) và không để đầu 0 vô nghĩa
        val candidate = (amountDigits + d).trimStart('0').ifEmpty { "" }
        if (candidate.length > 12) return
        amountDigits = candidate
        refreshAmountText()
    }

    private fun refreshAmountText() {
        val value = amountDigits.toLongOrNull() ?: 0L
        binding.tvAmount.text = if (amountDigits.isEmpty()) "" else CurrencyUtils.formatPlain(value)
    }

    // ==================== Ngày / giờ (mục 5.6.3, 5.6.4) ====================

    private fun setupPickers() {
        binding.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { utcMillis ->
                // MaterialDatePicker trả về millis UTC của NGÀY -> đổi về LocalDate
                selectedDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                refreshDateButtons()
            }
            picker.show(supportFragmentManager, "date_picker")
        }

        binding.btnPickTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedTime.hour)
                .setMinute(selectedTime.minute)
                .build()
            picker.addOnPositiveButtonClickListener {
                selectedTime = LocalTime.of(picker.hour, picker.minute)
                refreshDateButtons()
            }
            picker.show(supportFragmentManager, "time_picker")
        }
    }

    private fun refreshDateButtons() {
        binding.btnPickDate.text = getString(R.string.chip_date) + ": " +
            DateTimeUtils.dateFormatter.format(selectedDate)
        binding.btnPickTime.text = getString(R.string.chip_time) + ": " +
            "%02d:%02d".format(selectedTime.hour, selectedTime.minute)

        // Cảnh báo ngày tương lai (mục 3.1.4) - vẫn cho lưu
        binding.tvFutureWarning.visibility =
            if (selectedDate.isAfter(DateTimeUtils.today())) View.VISIBLE else View.GONE
    }

    // ==================== Lưu ====================

    private fun attemptSave() {
        val amount = amountDigits.toLongOrNull() ?: 0L
        if (amount <= 0) {
            Snackbar.make(binding.root, getString(R.string.error_amount), Snackbar.LENGTH_SHORT).show()
            return
        }
        val category = selectedCategory ?: run {
            Snackbar.make(binding.root, getString(R.string.error_category), Snackbar.LENGTH_SHORT).show()
            return
        }

        val dateTime = LocalDateTime.of(selectedDate, selectedTime)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val tx = TransactionEntity(
            id = if (isNew) 0L else editingTxId,
            uid = viewModel.uid,
            type = category.type,
            amount = amount,
            categoryId = category.id,
            dateTime = dateTime,
            note = binding.edtNote.text.toString().trim(),
            createdAt = editingTx?.createdAt ?: System.currentTimeMillis()
        )

        binding.btnSave.isEnabled = false
        viewModel.save(tx, isNew) { error ->
            binding.btnSave.isEnabled = true
            if (error == null) {
                Snackbar.make(binding.root, getString(Messages.TX_SAVED), Snackbar.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
