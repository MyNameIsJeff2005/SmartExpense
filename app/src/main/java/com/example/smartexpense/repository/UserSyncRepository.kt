package com.example.smartexpense.repository

import android.content.Context
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.data.remote.FirestoreUserRepository
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.Messages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Đồng bộ hồ sơ người dùng giữa Room (offline) và **Firebase Firestore** (mục 3.2).
 *
 * Chiến lược "offline-first":
 *  - App luôn đọc/ghi Room trước, nên mất mạng vẫn dùng bình thường.
 *  - Khi có mạng: đẩy hồ sơ lên Firestore (push) và/hoặc kéo toàn bộ về Room (pull).
 *  - Không bao giờ gửi mật khẩu/token; chỉ các trường hồ sơ công khai.
 *
 * Từ bản 1.3.0: Firestore thay cho server REST MockAPI - hồ sơ nằm trong
 * collection `users`, được cấu hình sẵn qua `app/google-services.json`.
 */
class UserSyncRepository(private val context: Context) {

    private val dao get() = AppDatabase.get(context).userProfileDao()
    private val prefs get() = AppPrefs.get(context)
    private val remote = FirestoreUserRepository()

    data class SyncSummary(
        val pulled: Int = 0,
        val pushed: Int = 0,
        val skipped: Int = 0
    )

    /** Firestore luôn sẵn sàng vì đã có app/google-services.json (bản 1.3.0). */
    fun isConfigured(): Boolean = true

    /** Tên cơ sở dữ liệu đang dùng, hiển thị trong màn Quản trị. */
    fun baseUrl(): String = runCatching {
        "Firestore · ${com.google.firebase.firestore.FirebaseFirestore.getInstance().app.options.projectId}"
    }.getOrDefault("Firestore")

    /**
     * KÉO dữ liệu: lấy toàn bộ hồ sơ trên Firestore và gộp vào Room.
     * Quy tắc gộp: giữ role/status đang có ở máy (admin không bị hạ quyền),
     * cập nhật tên/email/thời điểm tạo/lần đăng nhập nếu đám mây có dữ liệu.
     */
    suspend fun pullAll(): Result<SyncSummary> = runCatching {
        val remoteProfiles = remote.fetchAll()
        var pulled = 0
        withContext(Dispatchers.IO) {
            remoteProfiles.forEach { r ->
                val local = dao.getByUid(r.uid)
                dao.upsert(merge(local, r))
                pulled++
            }
            // Room vừa đổi -> dựng lại accounts.csv cho khớp
            CsvRepository.rebuildFromRoom(context)
        }
        prefs.lastSyncAt = System.currentTimeMillis()
        SyncSummary(pulled = pulled)
    }.mapError()

    /** ĐẨY dữ liệu: gửi toàn bộ hồ sơ trong Room lên Firestore (tạo mới hoặc cập nhật). */
    suspend fun pushAll(): Result<SyncSummary> = runCatching {
        val profiles = withContext(Dispatchers.IO) { dao.getAllOnce() }
        var pushed = 0
        var skipped = 0
        profiles.forEach { p ->
            // Bỏ qua hồ sơ demo (không phải tài khoản thật trên Firebase)
            if (p.uid.isBlank() || p.uid == AuthRepository.DEMO_UID) {
                skipped++
                return@forEach
            }
            remote.push(p)
            pushed++
        }
        prefs.lastSyncAt = System.currentTimeMillis()
        SyncSummary(pushed = pushed, skipped = skipped)
    }.mapError()

    /**
     * Đẩy MỘT hồ sơ lên Firestore - gọi sau khi đăng ký/đăng nhập/đổi tên.
     * Không chặn luồng chính và không báo lỗi cho người dùng nếu mạng hỏng.
     */
    suspend fun pushProfile(profile: UserProfile): Result<Unit> = runCatching {
        if (profile.uid.isBlank() || profile.uid == AuthRepository.DEMO_UID) return@runCatching
        remote.push(profile)
        prefs.lastSyncAt = System.currentTimeMillis()
    }

    /** Quy tắc gộp khi cả máy và đám mây đều có hồ sơ cùng uid. */
    private fun merge(local: UserProfile?, remote: UserProfile): UserProfile = UserProfile(
        uid = remote.uid,
        displayName = remote.displayName.ifBlank { local?.displayName.orEmpty() },
        email = remote.email.ifBlank { local?.email.orEmpty() },
        createdAt = when {
            remote.createdAt > 0 -> remote.createdAt
            local != null -> local.createdAt
            else -> System.currentTimeMillis()
        },
        // Giữ quyền/khóa của máy: đám mây không được tự nâng quyền admin
        role = local?.role ?: remote.role,
        status = local?.status ?: remote.status,
        lastLoginAt = maxOf(local?.lastLoginAt ?: 0L, remote.lastLoginAt)
    )

    /** Đổi lỗi kỹ thuật thành thông báo tiếng Việt dễ hiểu (mục 3.1.11). */
    private fun <T> Result<T>.mapError(): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { e ->
            val message = when {
                e is IOException -> "${context.getString(Messages.API_UNREACHABLE)} (${e.message.orEmpty().take(80)})"
                else -> "${context.getString(Messages.API_SYNC_FAILED)}: ${e.message.orEmpty().take(120)}"
            }
            Result.failure(Exception(message, e))
        }
    )
}
