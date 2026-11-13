package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.R
import com.example.smartexpense.model.CategoryEntity
import com.example.smartexpense.model.TransactionEntity
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CategoryRepository
import com.example.smartexpense.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel màn Thêm/Sửa giao dịch (mục 5.6, 3.1.4):
 * - Lưới 4x2 danh mục: LOẠI giao dịch lấy theo danh mục được chọn (chi/thu)
 * - Kiểm tra số tiền/danh mục do Activity làm trước khi gọi save()
 * - Bấm Lưu có loading; lỗi trả về qua callback để hiển thị
 */
class AddEditTransactionViewModel(app: Application) : AndroidViewModel(app) {

    private val txRepo = TransactionRepository(app)
    private val categoryRepo = CategoryRepository(app)

    val uid: String = AuthRepository(app).currentUserUid() ?: ""

    /** Lưới 4x2 danh mục: toàn bộ danh mục hiện có (admin có thể thêm bằng quản lý danh mục) */
    val categories: StateFlow<List<CategoryEntity>> = categoryRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    suspend fun loadTransaction(txId: Long): TransactionEntity? = withContext(Dispatchers.IO) {
        txRepo.getById(txId)
    }

    /** insert nếu isNew, ngược lại update; onDone(null) = thành công, onDone(lỗi) = hiển thị thông báo */
    fun save(tx: TransactionEntity, isNew: Boolean, onDone: (String?) -> Unit) {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            try {
                if (isNew) txRepo.insert(tx) else txRepo.update(tx)
                withContext(Dispatchers.Main) { onDone(null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onDone(e.message ?: getApplication<Application>().getString(R.string.add_edit_save_failed))
                }
            } finally {
                _loading.value = false
            }
        }
    }
}
