package com.example.smartexpense.repository

import android.content.Context
import androidx.annotation.StringRes
import com.example.smartexpense.R
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Dữ liệu MẪU cho chế độ Demo (vào app không cần Firebase - dùng khi chưa cấu hình API).
 * Seed đúng 1 lần cho tài khoản demo; các lần mở lại giữ nguyên dữ liệu để trải nghiệm mượt.
 * Khi xài Firebase thật và đăng kí tài khoản, dữ liệu demo này KHÔNG ảnh hưởng tài khoản thật.
 *
 * Ghi chú được lấy từ strings.xml theo ngôn ngữ máy (demo_note_*), danh mục được khớp theo
 * iconKey (không phụ thuộc tên) nên chạy đúng cả ở tiếng Việt và tiếng Anh.
 */
object DemoData {

    /** (iconKey danh mục, là thu?, số tiền VND, ngày trong tháng, giờ, phút, ghi chú) */
    private data class Sample(
        val categoryKey: String,
        val income: Boolean,
        val amount: Long,
        val dayOfMonth: Int,
        val hour: Int,
        val minute: Int,
        @StringRes val noteRes: Int
    )

    // Kịch bản đời thường: lương + thưởng + chi tiêu đa dạng (dùng cho 2 tháng liên tiếp)
    private val samples = listOf(
        Sample("ic_salary", true, 8_500_000, 5, 9, 0, R.string.demo_note_salary),
        Sample("ic_gift", true, 600_000, 20, 15, 30, R.string.demo_note_bonus),
        Sample("ic_food", false, 35_000, 2, 12, 0, R.string.demo_note_lunch),
        Sample("ic_food", false, 68_000, 7, 19, 30, R.string.demo_note_coffee),
        Sample("ic_car", false, 120_000, 10, 8, 0, R.string.demo_note_fuel),
        Sample("ic_school", false, 250_000, 14, 20, 45, R.string.demo_note_book),
        Sample("ic_shopping", false, 1_200_000, 18, 14, 0, R.string.demo_note_clothes),
        Sample("ic_movie", false, 180_000, 22, 20, 0, R.string.demo_note_movie),
        Sample("ic_health", false, 300_000, 25, 10, 15, R.string.demo_note_dental),
        Sample("ic_food", false, 45_000, 26, 12, 30, R.string.demo_note_party)
    )

    /** Chèn mẫu nếu tài khoản demo chưa có giao dịch nào */
    suspend fun seedIfEmpty(context: Context, demoUid: String) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).transactionDao()
        val categories = CategoryRepository(context).getAllOnce()
        if (dao.countByUid(demoUid) > 0 || categories.isEmpty()) return@withContext

        val byKey = categories.associateBy { it.iconKey }
        val monthAnchor = LocalDate.now().withDayOfMonth(1)
        for (month in listOf(monthAnchor.minusMonths(1), monthAnchor)) {
            for (s in samples) {
                val day = s.dayOfMonth.coerceAtMost(month.lengthOfMonth())
                val dateTime = month.withDayOfMonth(day)
                    .atTime(LocalTime.of(s.hour, s.minute))
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val categoryId = byKey[s.categoryKey]?.id ?: continue
                dao.insert(
                    TransactionEntity(
                        uid = demoUid,
                        type = if (s.income) TransactionEntity.TYPE_INCOME else TransactionEntity.TYPE_EXPENSE,
                        amount = s.amount,
                        categoryId = categoryId,
                        dateTime = dateTime,
                        note = context.getString(s.noteRes),
                        createdAt = dateTime
                    )
                )
            }
        }
    }
}
