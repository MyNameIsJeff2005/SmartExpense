package com.example.smartexpense.repository

import android.content.Context
import com.example.smartexpense.BuildConfig
import com.example.smartexpense.R
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.data.remote.FirestoreUserRepository
import com.example.smartexpense.utils.LockedAccountException
import com.example.smartexpense.utils.Messages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository xác thực: Firebase Auth + đồng bộ Room/CSV (mục 3.1, 3.2).
 * isAdmin là kiểm tra phía client theo email trong local.properties (mục 3.2c),
 * đủ dùng cho đồ án; bảo mật thật cần Firebase custom claims + backend (ngoài phạm vi).
 */
class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val userDao get() = AppDatabase.get(context).userProfileDao()

    // Lưu trạng thái phiên DEMO (không phải thông tin đăng nhập, dùng SharedPreferences thường)
    private val demoPrefs = context.getSharedPreferences("demo_session", Context.MODE_PRIVATE)

    data class AuthOutcome(val profile: UserProfile, val csvOk: Boolean)

    /** Đăng ký: tạo user Firebase -> set displayName -> insert Room -> thêm dòng CSV */
    suspend fun register(fullName: String, email: String, password: String): Result<AuthOutcome> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: error(context.getString(Messages.REGISTER_FAILED))
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(fullName).build()
        ).await()

        val now = System.currentTimeMillis()
        val profile = UserProfile(
            uid = user.uid,
            displayName = fullName,
            email = user.email ?: email,
            createdAt = user.metadata?.creationTimestamp ?: now,
            role = if (isAdminEmail(user.email)) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER,
            status = UserProfile.STATUS_ACTIVE,
            lastLoginAt = 0
        )
        withContext(Dispatchers.IO) { userDao.upsert(profile) }
        // Phiên Firebase thật -> tắt chế độ demo nếu còn bật
        demoPrefs.edit().putBoolean(KEY_DEMO_ACTIVE, false).apply()
        // Tự động thêm dòng CSV; nếu thất bại KHÔNG báo thành công giả: trả csvOk=false
        val csvOk = runCatching { CsvRepository.ensureProfileAdded(context, profile) }.isSuccess
        // Lưu hồ sơ lên Firestore (chạy nền, lỗi mạng không ảnh hưởng đăng ký)
        syncProfileInBackground(profile)
        AuthOutcome(profile, csvOk)
    }.recoverCatching { throw mapFirebaseError(it) }

    /** Đăng nhập: từ chối LOCKED, khôi phục hồ sơ nếu thiếu, cập nhật lastLoginAt + CSV */
    suspend fun login(email: String, password: String): Result<AuthOutcome> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: error(context.getString(Messages.LOGIN_FAILED))
        val now = System.currentTimeMillis()

        val existing = withContext(Dispatchers.IO) { userDao.getByUid(user.uid) }

        // Tài khoản bị khóa trên thiết bị này -> từ chối đăng nhập (mục 3.1.2)
        if (existing?.status == UserProfile.STATUS_LOCKED) {
            auth.signOut()
            throw LockedAccountException(context.getString(Messages.ACCOUNT_LOCKED))
        }

        val profile: UserProfile
        if (existing == null) {
            // Xóa dữ liệu app / đổi máy: dựng lại hồ sơ (3.2a) - nếu tài khoản này
            // đã từng dùng, lấy tên/ngày tạo/status từ Firestore (bản 1.3.0)
            val cloud = runCatching { FirestoreUserRepository().fetch(user.uid) }.getOrNull()
            val cloudName = cloud?.displayName?.takeIf { it.isNotBlank() }
            profile = UserProfile(
                uid = user.uid,
                displayName = cloudName ?: user.displayName ?: email,
                email = (cloud?.email?.takeIf { it.isNotBlank() }) ?: user.email ?: email,
                createdAt = cloud?.createdAt?.takeIf { it > 0 }
                    ?: user.metadata?.creationTimestamp ?: now,
                role = cloud?.role?.takeIf { it.isNotBlank() }
                    ?: if (isAdminEmail(user.email)) UserProfile.ROLE_ADMIN else UserProfile.ROLE_USER,
                status = cloud?.status?.takeIf { it.isNotBlank() } ?: UserProfile.STATUS_ACTIVE,
                lastLoginAt = now
            )
            withContext(Dispatchers.IO) { userDao.upsert(profile) }
        } else {
            profile = existing.copy(lastLoginAt = now)
            withContext(Dispatchers.IO) { userDao.updateLastLogin(user.uid, now) }
        }
        // Phiên Firebase thật -> tắt chế độ demo nếu còn bật
        demoPrefs.edit().putBoolean(KEY_DEMO_ACTIVE, false).apply()
        val csvOk = runCatching { CsvRepository.ensureProfileAdded(context, profile) }.isSuccess
        // Cập nhật lần đăng nhập gần nhất lên Firestore (chạy nền)
        syncProfileInBackground(profile)
        AuthOutcome(profile, csvOk)
    }.recoverCatching { throw mapFirebaseError(it) }

    /** Quên mật khẩu: gửi email đặt lại (mục 3.4) */
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        // Tắt phiên demo (nếu có) rồi đăng xuất Firebase (nếu đã cấu hình)
        demoPrefs.edit().putBoolean(KEY_DEMO_ACTIVE, false).apply()
        runCatching { auth.signOut() }
    }

    fun currentUser(): FirebaseUser? = runCatching { auth.currentUser }.getOrNull()

    /** UID phiên hiện tại: tài khoản Firebase thật, hoặc tài khoản DEMO nếu đang ở chế độ demo */
    fun currentUserUid(): String? =
        runCatching { auth.currentUser?.uid }.getOrNull() ?: if (isDemoActive()) DEMO_UID else null

    /** Có phiên nào đang chạy không (Firebase thật HOẶC demo) - dùng ở cổng MainActivity */
    fun hasActiveSession(): Boolean = currentUserUid() != null

    // ---------- Phân quyền admin (mục 3.2c) ----------

    fun isAdminEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val adminEmails = BuildConfig.ADMIN_EMAILS
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        return adminEmails.contains(email.trim().lowercase())
    }

    fun isCurrentUserAdmin(): Boolean = isDemoActive() || isAdminEmail(currentUser()?.email)

    // ---------- Chế độ DEMO (xem nhanh app khi chưa cấu hình Firebase) ----------

    /**
     * Vào app bằng hồ sơ cục bộ + dữ liệu mẫu, KHÔNG đụng Firebase.
     * Hồ sơ demo là ADMIN để xem thử cả phần Quản trị; Đăng xuất có thể quay lại.
     */
    suspend fun demoLogin(): Result<AuthOutcome> = runCatching {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val profile = UserProfile(
                uid = DEMO_UID,
                displayName = context.getString(R.string.demo_account_name),
                email = DEMO_EMAIL,
                createdAt = now,
                role = UserProfile.ROLE_ADMIN, // chế độ demo cho xem thử cả mục Quản trị
                status = UserProfile.STATUS_ACTIVE,
                lastLoginAt = now
            )
            CategoryRepository(context).seedIfNeeded()
            userDao.upsert(profile)
            DemoData.seedIfEmpty(context, DEMO_UID)
            demoPrefs.edit().putBoolean(KEY_DEMO_ACTIVE, true).apply()
            AuthOutcome(profile, true)
        }
    }

    fun isDemoActive(): Boolean = demoPrefs.getBoolean(KEY_DEMO_ACTIVE, false)

    /** Đồng bộ hồ sơ lên server ở luồng nền, bỏ qua mọi lỗi (offline-first). */
    private fun syncProfileInBackground(profile: UserProfile) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { UserSyncRepository(context).pushProfile(profile) }
        }
    }

    /** Đổi lỗi Firebase nguyên bản thành thông báo tiếng Việt dễ hiểu (mục 3.1.11) */
    private fun mapFirebaseError(e: Throwable): Throwable = when (e) {
        is LockedAccountException -> e
        is FirebaseAuthUserCollisionException -> Exception(context.getString(Messages.EMAIL_EXISTS))
        is FirebaseAuthWeakPasswordException -> Exception(context.getString(Messages.WEAK_PASSWORD))
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> Exception(context.getString(Messages.WRONG_CREDENTIALS))
        else -> {
            val msg = (e.message ?: "").lowercase()
            if (msg.contains("configuration_not_found") || msg.contains("api key")) {
                Exception(context.getString(Messages.NOT_CONFIGURED))
            } else {
                Exception(context.getString(Messages.AUTH_FAILED))
            }
        }
    }

    companion object {
        const val DEMO_UID = "demo-local-user"
        const val DEMO_EMAIL = "demo@smartexpense.local"
        private const val KEY_DEMO_ACTIVE = "demo_active"
    }
}
