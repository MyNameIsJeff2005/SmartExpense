package com.example.smartexpense.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.BuildConfig
import com.example.smartexpense.R
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.UserSyncRepository
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Tab Cá nhân: hồ sơ, hạn mức, ngôn ngữ, giao diện, đồng bộ API, đăng xuất (mục 5.8, 3.7, 3.10) */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository(app)
    private val prefs = AppPrefs.get(app)
    private val userDao = AppDatabase.get(app).userProfileDao()
    private val syncRepo = UserSyncRepository(app)

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: MutableStateFlow<UserProfile?> = _profile

    val isAdmin: Boolean = auth.isCurrentUserAdmin()

    val budget = MutableStateFlow(prefs.monthlyBudget)

    /** Chế độ sáng/tối đang chọn: system | light | dark */
    val themeMode = MutableStateFlow(prefs.themeMode)

    /** Chuỗi phiên bản hiển thị ở màn Cài đặt, vd "1.2.3 (6)" */
    val appVersion: String =
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    /** Tên phiên bản thuần, hiển thị ở thẻ Giới thiệu: "1.2.3" */
    val versionName: String = BuildConfig.VERSION_NAME

    /** Chế độ đồng bộ: Firestore luôn sẵn sàng (1.3.0), chỉ cần có Internet */
    val lastSyncAt = MutableStateFlow(prefs.lastSyncAt)
    val syncState = MutableStateFlow<UiState<String>>(UiState.Idle)

    init {
        reload()
    }

    /** Đọc lại hồ sơ từ Room (sau khi chỉnh sửa ở màn Account). */
    fun reload() {
        viewModelScope.launch {
            val uid = auth.currentUserUid() ?: return@launch
            _profile.value = userDao.getByUid(uid)
        }
    }

    fun saveBudget(amount: Long) {
        prefs.monthlyBudget = amount
        budget.value = amount
    }

    /** Đổi chế độ sáng/tối và áp dụng ngay (Activity tự tạo lại). */
    fun setThemeMode(context: Context, mode: String) {
        ThemeManager.setMode(context, mode)
        themeMode.value = mode
    }

    /**
     * Đồng bộ hồ sơ hiện tại lên Firestore.
     * Không ảnh hưởng dữ liệu cục bộ nếu mạng lỗi.
     */
    fun syncNow() {
        if (syncState.value is UiState.Loading) return
        val current = _profile.value
        if (current == null) {
            syncState.value = UiState.Error(getApplication<Application>().getString(R.string.profile_sync_no_profile))
            return
        }
        viewModelScope.launch {
            syncState.value = UiState.Loading
            syncRepo.pushProfile(current).fold(
                onSuccess = {
                    lastSyncAt.value = prefs.lastSyncAt
                    syncState.value = UiState.Success(getApplication<Application>().getString(R.string.profile_sync_done))
                },
                onFailure = { syncState.value = UiState.Error(it.message.orEmpty()) }
            )
        }
    }

    fun logout() = auth.signOut()
}
