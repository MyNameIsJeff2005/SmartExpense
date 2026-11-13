package com.example.smartexpense.model

/**
 * DTO phản hồi của API open.er-api.com/v6/latest/{base} (phân hệ v1.4):
 * {
 *   "result": "success",
 *   "base_code": "USD",
 *   "rates": { "VND": 25540.0, "EUR": 0.92, ... }
 * }
 * Chỉ lấy các trường cần dùng; Gson bỏ qua phần còn lại.
 */
data class ExchangeRateResponse(
    val result: String?,
    val base_code: String?,
    val rates: Map<String, Double>?
) {
    /** Lấy tỷ giá của một mã tiền tệ, null nếu API lỗi hoặc thiếu mã. */
    fun rateOf(code: String): Double? =
        if (result == "success") rates?.get(code) else null
}
