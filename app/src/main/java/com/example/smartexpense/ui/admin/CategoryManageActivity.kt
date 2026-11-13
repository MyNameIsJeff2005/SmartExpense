package com.example.smartexpense.ui.admin
import com.example.smartexpense.adapter.CategoryManageAdapter

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartexpense.R
import com.example.smartexpense.utils.applySystemBarInsets
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.databinding.ActivityCategoryManageBinding
import com.example.smartexpense.databinding.DialogCategoryBinding
import com.example.smartexpense.viewmodel.AdminViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Quản lý danh mục (mục 6.6): danh sách + thêm/sửa bằng hộp thoại.
 * KHÔNG có nút xóa (mục 3.3.0: danh mục không bị xóa trong V1).
 */
class CategoryManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryManageBinding
    private val viewModel: AdminViewModel by viewModels()

    private lateinit var adapter: CategoryManageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = CategoryManageAdapter { category -> showCategoryDialog(category) }
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        binding.fabAdd.setOnClickListener { showCategoryDialog(null) }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    adapter.submit(categories)
                    binding.tvEmpty.visibility =
                        if (categories.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    /** Hộp thoại thêm/sửa danh mục (mục 6.6): tên, loại (Chi/Thu), mã màu hex */
    private fun showCategoryDialog(editing: CategoryEntity?) {
        val dialogBinding = DialogCategoryBinding.inflate(layoutInflater)
        editing?.let {
            dialogBinding.edtName.setText(it.name)
            dialogBinding.edtColor.setText(it.colorHex)
            if (it.type == CategoryEntity.TYPE_INCOME) {
                dialogBinding.rbIncome.isChecked = true
                dialogBinding.rbExpense.isChecked = false
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(
                if (editing == null) R.string.category_dialog_add_title
                else R.string.category_dialog_edit_title
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                val name = dialogBinding.edtName.text.toString().trim()
                val color = dialogBinding.edtColor.text.toString().trim()
                val type =
                    if (dialogBinding.rbIncome.isChecked) CategoryEntity.TYPE_INCOME
                    else CategoryEntity.TYPE_EXPENSE

                if (name.isEmpty()) {
                    Snackbar.make(binding.root, getString(R.string.error_required), Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                    Snackbar.make(binding.root, getString(R.string.category_error_invalid_color), Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val current = editing
                if (current == null) {
                    viewModel.insertCategory(name, type, color)
                    Snackbar.make(binding.root, getString(R.string.app_name), Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.updateCategory(current, name, type, color)
                }
            }
            .show()
    }
}
