package com.example.smartexpense.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.smartexpense.model.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO cache tỷ giá (phân hệ v1.4): ghi bằng suspend fun, đọc bằng Flow để
 * UI tự cập nhật khi dữ liệu trong Room thay đổi.
 */
@Dao
interface ExchangeRateDao {

    /** Lưu (hoặc cập nhật) một loạt dòng tỷ giá theo khóa chính code. */
    @Upsert
    suspend fun upsertAll(rates: List<ExchangeRateEntity>)

    /** Quan sát toàn bộ tỷ giá của một base (VD "USD"). */
    @Query("SELECT * FROM exchange_rates WHERE base = :base")
    fun observeAll(base: String): Flow<List<ExchangeRateEntity>>

    /** Lấy một dòng cụ thể (base -> code) để đọc cache khi lỗi mạng. */
    @Query(
        "SELECT * FROM exchange_rates WHERE base = :base AND code = :code LIMIT 1"
    )
    suspend fun get(base: String, code: String): ExchangeRateEntity?

    /** Xóa sạch cache (dùng khi muốn tải lại từ đầu). */
    @Query("DELETE FROM exchange_rates")
    suspend fun clear()
}
