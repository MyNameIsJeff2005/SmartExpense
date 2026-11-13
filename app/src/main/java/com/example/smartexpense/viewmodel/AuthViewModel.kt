package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.AuthRepository.AuthOutcome
import com.example.smartexpense.utils.Messages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel cho Login / Register / Forgot (mục 5.1):
 * nút bấm có trạng thái loading, không nháy; lỗi trả thông báo tiếng Việt (mục 3.1.11).
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(app)

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState

    private val _registerState = MutableStateFlow<UiState<AuthOutcome>>(UiState.Idle)
    val registerState: StateFlow<UiState<AuthOutcome>> = _registerState

    private val _forgotState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val forgotState: StateFlow<UiState<Unit>> = _forgotState

    private val _demoState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val demoState: StateFlow<UiState<Unit>> = _demoState

    fun login(email: String, password: String) {
        if (_loginState.value is UiState.Loading) return // chống bấm nhiều lần
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            repo.login(email, password).fold(
                onSuccess = { _loginState.value = UiState.Success(Unit) },
                onFailure = { _loginState.value = UiState.Error(it.message ?: getApplication<Application>().getString(Messages.AUTH_FAILED)) }
            )
        }
    }

    fun register(fullName: String, email: String, password: String) {
        if (_registerState.value is UiState.Loading) return
        _registerState.value = UiState.Loading
        viewModelScope.launch {
            repo.register(fullName, email, password).fold(
                onSuccess = { outcome -> _registerState.value = UiState.Success(outcome) },
                onFailure = { _registerState.value = UiState.Error(it.message ?: getApplication<Application>().getString(Messages.REGISTER_FAILED)) }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (_forgotState.value is UiState.Loading) return
        _forgotState.value = UiState.Loading
        viewModelScope.launch {
            repo.sendPasswordReset(email).fold(
                onSuccess = { _forgotState.value = UiState.Success(Unit) },
                onFailure = { _forgotState.value = UiState.Error(it.message ?: getApplication<Application>().getString(Messages.RESET_FAILED)) }
            )
        }
    }

    /** Chế độ demo: vào app bằng hồ sơ nội bộ + dữ liệu mẫu, không cần cấu hình Firebase */
    fun demo() {
        if (_demoState.value is UiState.Loading) return
        _demoState.value = UiState.Loading
        viewModelScope.launch {
            repo.demoLogin().fold(
                onSuccess = { _demoState.value = UiState.Success(Unit) },
                onFailure = { _demoState.value = UiState.Error(it.message ?: getApplication<Application>().getString(Messages.AUTH_FAILED)) }
            )
        }
    }

    fun clearStates() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
        _forgotState.value = UiState.Idle
        _demoState.value = UiState.Idle
    }
}
