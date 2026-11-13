package com.example.smartexpense.model

/**
 * Bọc 3 trạng thái UI cho dữ liệu từ mạng (mục "Resource State" - yêu cầu v1.4):
 * - Loading: đang gọi API -> hiển thị ProgressBar
 * - Success: có dữ liệu (từ mạng hoặc cache Room) -> nạp lên giao diện
 * - Error: ngắt mạng hoặc HTTP 400/401/500 -> thông báo lỗi
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
