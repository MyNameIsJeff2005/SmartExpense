package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.model.ExchangeRateEntity
import com.example.smartexpense.model.Resource
import com.example.smartexpense.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel phân hệ Tỷ giá USD→VND (v1.4).
 * Phát [Resource] 3 trạng thái qua StateFlow: Loading -> Success/Error,
 * xoay màn hình giữ nguyên dữ liệu vì ViewModel sống độc lập với Activity.
 */
class RatesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ExchangeRateRepository(app)

    private val _rate =
        MutableStateFlow<Resource<ExchangeRateEntity>>(Resource.Loading)
    val rate: StateFlow<Resource<ExchangeRateEntity>> = _rate

    init {
        refresh()
    }

    /** Gọi lại API (nút thử lại / mở màn hình). */
    fun refresh() {
        _rate.value = Resource.Loading
        viewModelScope.launch {
            _rate.value = repo.refresh()
        }
    }
}
