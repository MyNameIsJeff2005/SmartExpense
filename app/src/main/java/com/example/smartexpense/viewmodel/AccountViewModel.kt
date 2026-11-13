package com.example.smartexpense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.data.remote.FirestoreUserRepository
import com.example.smartexpense.repository.AuthRepository
import com.example.smartexpense.repository.CsvRepository
import com.example.smartexpense.utils.AppPrefs
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Màn Account: đọc hồ sơ hiện tại và lưu thay đổi.
 *
 * - Tên hiển thị: cập nhật bảng Room user_profiles + Firebase Auth (nếu là tài khoản thật),
 *   sau đó dựng lại accounts.csv để file CSV không bị lệch.
 * - Giới tính / tuổi / ảnh đại diện: lưu trong AppPrefs (không cần đổi schema Room).
 */
class AccountViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepository(app)
    private val prefs = AppPrefs.get(app)
    private val userDao = AppDatabase.get(app).userProfileDao()
    private val transactionDao = AppDatabase.get(app).transactionDao()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: MutableStateFlow<UserProfile?> = _profile

    val gender = MutableStateFlow(prefs.accountGender)
    val age = MutableStateFlow(prefs.accountAge)
    val avatarPath = MutableStateFlow(prefs.avatarPath)

    /** UID phiên hiện tại (dùng để đặt tên file ảnh đại diện). */
    val uid: String? get() = auth.currentUserUid()

    init {
        viewModelScope.launch {
            val uid = auth.currentUserUid() ?: return@launch
            _profile.value = userDao.getByUid(uid)
        }
    }

    /**
     * Lưu thông tin tài khoản. [onDone] nhận true nếu ghi Room thành công.
     * Việc cập nhật Firebase/CSV chạy kèm và không chặn kết quả lưu cục bộ.
     */
    fun save(name: String, genderValue: String, ageValue: Int, avatar: String, onDone: (Boolean) -> Unit) {
        val current = _profile.value
        if (current == null) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val updated = current.copy(displayName = name)
            val saved = runCatching { userDao.upsert(updated) }.isSuccess
            if (!saved) {
                onDone(false)
                return@launch
            }

            prefs.accountGender = genderValue
            prefs.accountAge = ageValue
            prefs.avatarPath = avatar
            _profile.value = updated

            // Giữ accounts.csv khớp với tên mới trong Room (mục 3.2c)
            runCatching { CsvRepository.rebuildFromRoom(getApplication<Application>()) }

            // Cập nhật tên trên Firebase nếu đây là tài khoản thật (demo thì bỏ qua)
            runCatching {
                auth.currentUser()?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )?.await()
            }
            onDone(true)
        }
    }

    /**
     * Xóa sổ tài khoản khỏi cơ sở dữ liệu (bản 1.3.1):
     * - Room: xóa toàn bộ giao dịch + hồ sơ của uid hiện tại
     * - Firestore: xóa document `users/{uid}` (tài khoản Firebase thật)
     * - Firebase Auth: xóa hẳn user; nếu yêu cầu đăng nhập lại gần đây thì
     *   bỏ qua bước này (chỉ đăng xuất, doc Firestore đã xóa)
     * - Xóa ảnh đại diện và các tùy chọn riêng của tài khoản
     * [onResult] nhận true khi dữ liệu cục bộ đã dọn sạch.
     */
    fun deleteAccount(onResult: (Boolean) -> Unit) {
        val uid = uid
        if (uid.isNullOrBlank()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    transactionDao.deleteAllByUid(uid)
                    userDao.deleteByUid(uid)
                    CsvRepository.rebuildFromRoom(getApplication())
                }
                // Xóa ảnh đại diện của tài khoản (nếu có)
                if (prefs.avatarPath.isNotBlank()) {
                    runCatching { File(prefs.avatarPath).delete() }
                }
                prefs.avatarPath = ""
                prefs.accountGender = AppPrefs.GENDER_MALE
                prefs.accountAge = 0
            }.isSuccess

            if (result) {
                // Dọn phía đám mây (bỏ qua lỗi mạng / phiên demo)
                if (uid != AuthRepository.DEMO_UID) {
                    runCatching { FirestoreUserRepository().delete(uid) }
                }
                // Xóa tài khoản Firebase nếu phiên còn "tươi"; thất bại thì chỉ đăng xuất
                runCatching { auth.currentUser()?.delete()?.await() }
                auth.signOut()
            }
            onResult(result)
        }
    }
}
