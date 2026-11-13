package com.example.smartexpense

import android.app.Application
import com.example.smartexpense.repository.BackupRepository
import com.example.smartexpense.repository.CategoryRepository
import com.example.smartexpense.repository.CsvRepository
import com.example.smartexpense.utils.DateTimeUtils
import com.example.smartexpense.utils.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application class:
 * - Seed 8 danh mục mặc định lần chạy đầu
 * - Kiểm tra/tự dựng lại accounts.csv nếu bị xóa (chỉ khi công tắc BẬT - mục 3.2a)
 *   Việc này chạy ngầm IO, không chặn mở app, không hỏi người dùng.
 * - Tự động sao lưu dữ liệu khi phát hiện phiên bản app mới (công tắc Automatic backup)
 */
class SmartExpenseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Áp dụng chế độ sáng/tối đã lưu TRƯỚC khi Activity đầu tiên được tạo
        runCatching { ThemeManager.apply(this) }
        // Đồng bộ ngôn ngữ hiện tại để tên thứ/tháng hiển thị đúng tiếng Việt hoặc tiếng Anh
        runCatching { DateTimeUtils.syncLocale(this) }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                CategoryRepository(this@SmartExpenseApp).seedIfNeeded()
                CsvRepository.autoRecoverIfNeeded(this@SmartExpenseApp)
                BackupRepository.autoBackupIfVersionChanged(this@SmartExpenseApp)
            }
        }
    }
}
