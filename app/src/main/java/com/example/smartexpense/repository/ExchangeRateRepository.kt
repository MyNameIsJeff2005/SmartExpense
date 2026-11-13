package com.example.smartexpense.repository

import android.content.Context
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.data.local.dao.ExchangeRateDao
import com.example.smartexpense.data.remote.exchange.ExchangeRateApi
import com.example.smartexpense.data.remote.exchange.ExchangeRateClient
import com.example.smartexpense.model.ExchangeRateEntity
import com.example.smartexpense.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phân hệ Tỷ giá USD→VND (v1.4):
 * 1. Gọi API (Dispatchers.IO, suspend fun) -> lưu cache vào Room.
 * 2. Thành công -> Resource.Success(dữ liệu mới).
 * 3. Lỗi mạng / HTTP lỗi -> đọc cache Room: có cache -> Success (dữ liệu cũ),
 *    không có -> Resource.Error để UI báo lỗi + cho bấm thử lại.
 */
class ExchangeRateRepository(context: Context) {

    private val dao: ExchangeRateDao = AppDatabase.get(context).exchangeRateDao()
    private val api: ExchangeRateApi = ExchangeRateClient.api

    /** Mặc định làm việc với cặp USD -> VND. */
    companion object {
        const val DEFAULT_BASE = "USD"
        const val DEFAULT_CODE = "VND"
    }

    /** Quan sát cache tỷ giá trong Room (Flow -> UI tự cập nhật khi dữ liệu đổi). */
    fun observeRates(base: String = DEFAULT_BASE) = dao.observeAll(base)

    /** Đọc một dòng cache cụ thể (không quan sát). */
    suspend fun getCachedRate(
        base: String = DEFAULT_BASE,
        code: String = DEFAULT_CODE
    ): ExchangeRateEntity? = dao.get(base, code)

    /**
     * Tải tỷ giá mới nhất về và cập nhật cache Room.
     * Trả về [Resource] theo 3 trạng thái UI (Loading do ViewModel phát trước khi gọi).
     */
    suspend fun refresh(
        base: String = DEFAULT_BASE,
        code: String = DEFAULT_CODE
    ): Resource<ExchangeRateEntity> = withContext(Dispatchers.IO) {
        try {
            val response = api.latest(base)
            val rate = response.rateOf(code)
            if (rate != null && rate > 0.0) {
                val entity = ExchangeRateEntity(
                    code = code,
                    base = base,
                    rate = rate,
                    fetchedAt = System.currentTimeMillis()
                )
                dao.upsertAll(listOf(entity))
                Resource.Success(entity)
            } else {
                // API trả lời nhưng nội dung không dùng được (HTTP 4xx/5xx trả body lỗi)
                fallbackToCache(base, code)
            }
        } catch (e: Exception) {
            // Ngắt mạng / timeout / DNS: dùng cache nếu có
            fallbackToCache(base, code)
        }
    }

    private suspend fun fallbackToCache(
        base: String,
        code: String
    ): Resource<ExchangeRateEntity> {
        val cached = dao.get(base, code)
        return if (cached != null) {
            Resource.Success(cached) // dữ liệu cũ, UI vẫn hiển thị được ở chế độ Máy bay
        } else {
            Resource.Error("no_data")
        }
    }
}
