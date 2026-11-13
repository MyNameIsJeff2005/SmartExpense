package com.example.smartexpense.ui.transactions

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smartexpense.R
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.TransactionEntity
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.databinding.ActivityDetailBinding
import com.example.smartexpense.utils.CurrencyUtils
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.Messages
import com.example.smartexpense.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Chi tiết giao dịch (mục 5.7): Xem; Xóa (hộp thoại xác nhận, mục 3.1.8),
 * Sửa -> AddEditTransactionActivity; chỉ chủ sở hữu mới xóa được (uid kiểm tra trong DAO - 3.1.6).
 */
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private var txId: Long = 0
    private var loadedTx: TransactionEntity? = null

    private val uid: String by lazy { AuthRepository(this).currentUserUid() ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        txId = intent.getLongExtra(EXTRA_TX_ID, 0L)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnEdit.setOnClickListener {
            if (loadedTx != null) {
                startActivity(
                    Intent(this, AddEditTransactionActivity::class.java)
                        .putExtra(AddEditTransactionActivity.EXTRA_TX_ID, txId)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load() // reload khi quay lại sau khi sửa (mục 5.7)
    }

    private fun load() {
        lifecycleScope.launch {
            val tx = withContext(Dispatchers.IO) {
                AppDatabase.get(this@DetailActivity).transactionDao().getById(txId)
            } ?: run {
                Snackbar.make(binding.root, getString(Messages.TX_LOAD_FAILED), Snackbar.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            loadedTx = tx

            val category = withContext(Dispatchers.IO) {
                AppDatabase.get(this@DetailActivity).categoryDao().getById(tx.categoryId)
            }

            binding.tvName.text = tx.note.ifBlank { category?.name ?: getString(Messages.TX_LOAD_FAILED) }
            val sign = if (tx.type == TransactionEntity.TYPE_INCOME) "+" else "-"
            binding.tvAmount.text = sign + CurrencyUtils.formatVnd(tx.amount)
            binding.tvAmount.setTextColor(
                if (tx.type == TransactionEntity.TYPE_INCOME) getColor(R.color.green)
                else getColor(R.color.text_primary)
            )

            binding.badgeCircle.backgroundTintList =
                ColorStateList.valueOf(UiUtils.parseColor(category?.colorHex))
            binding.ivIcon.setImageResource(UiUtils.categoryIconId(this@DetailActivity, category?.iconKey))

            binding.rowType.tvLabel.setText(R.string.detail_type)
            binding.rowType.tvValue.setText(
                if (tx.type == TransactionEntity.TYPE_INCOME) R.string.list_filter_income
                else R.string.list_filter_expense
            )
            binding.rowCategory.tvLabel.setText(R.string.detail_category)
            binding.rowCategory.tvValue.text = category?.name ?: ""
            binding.rowDate.tvLabel.setText(R.string.detail_date)
            binding.rowDate.tvValue.text = DateTimeUtils.formatDateTime(tx.dateTime)
            binding.rowNote.tvLabel.setText(R.string.detail_note)
            binding.rowNote.tvValue.text = tx.note.ifBlank { "—" }
            binding.rowCreated.tvLabel.setText(R.string.detail_created)
            binding.rowCreated.tvValue.text = DateTimeUtils.formatDateTime(tx.createdAt)
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                lifecycleScope.launch {
                    // DAO delete sẽ lọc theo uid nên không xóa được giao dịch của người khác (3.1.6)
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(this@DetailActivity).transactionDao().delete(txId, uid)
                    }
                    Snackbar.make(binding.root, getString(Messages.TX_DELETE_SUCCESS), Snackbar.LENGTH_SHORT).show()
                    finish()
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_TX_ID = "extra_tx_id"
    }
}
