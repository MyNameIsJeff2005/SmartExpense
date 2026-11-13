package com.example.smartexpense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Một dòng tỷ giá lưu cache cục bộ (phân hệ v1.4): 1 đơn vị [base] = [rate] đơn vị [code].
 * VD: base = "USD", code = "VND", rate = 25_540.0 -> 1 USD ≈ 25.540 ₫.
 * Lưu Room để ở chế độ Máy bay vẫn xem được tỷ giá lần gần nhất.
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val code: String,    // mã tiền tệ đích, VD "VND"
    val base: String,                // mã tiền tệ gốc, VD "USD"
    val rate: Double,                // 1 base = rate
    val fetchedAt: Long              // thời điểm tải về (epoch millis)
)
