package com.example.smartexpense.viewmodel

/** Trạng thái UI chung theo mục 9: Loading / Success / Error */
sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T? = null) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
