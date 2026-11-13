package com.example.smartexpense.data.remote

import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.repository.AuthRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Cơ sở dữ liệu đám mây Firebase Firestore (bản 1.3.0): hồ sơ người dùng nằm
 * trong collection "users", mỗi tài khoản một document với **uid của Firebase
 * Auth làm id**. Chỉ lưu các trường công khai - mật khẩu không bao giờ xuất hiện.
 *
 * Cấu trúc document:
 * ```
 * users/{uid} = { uid, displayName, email, createdAt, role, status, lastLoginAt }
 * ```
 * Được cấu hình sẵn qua `app/google-services.json`, không cần server riêng.
 */
class FirestoreUserRepository {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val collection get() = db.collection(COLLECTION)

    /** Đẩy (tạo mới hoặc cập nhật) hồ sơ lên Firestore. */
    suspend fun push(profile: UserProfile) {
        collection.document(profile.uid).set(profile.toMap()).await()
    }

    /** Lấy một hồ sơ theo uid; trả null nếu tài khoản chưa từng lưu trên Firestore. */
    suspend fun fetch(uid: String): UserProfile? =
        collection.document(uid).get().await().takeIf { it.exists() }?.toProfile()

    /** Lấy toàn bộ hồ sơ trong collection (dùng khi admin tải dữ liệu về máy). */
    suspend fun fetchAll(): List<UserProfile> =
        collection.get().await().documents.mapNotNull { it.toProfile() }

    /** Xóa hẳn document hồ sơ của một tài khoản (dùng khi xóa tài khoản - 1.3.1). */
    suspend fun delete(uid: String) {
        collection.document(uid).delete().await()
    }

    private fun DocumentSnapshot.toProfile(): UserProfile? {
        // Demo/UID rỗng: bỏ qua, không đưa vào dữ liệu thật
        val uid = when (val docUid = getString(KEY_UID)?.trim().orEmpty()) {
            "" -> id
            else -> docUid
        }
        if (uid.isBlank() || uid == AuthRepository.DEMO_UID) return null
        return UserProfile(
            uid = uid,
            displayName = getString(KEY_NAME).orEmpty(),
            email = getString(KEY_EMAIL).orEmpty(),
            createdAt = getLong(KEY_CREATED_AT) ?: 0L,
            role = fieldString(KEY_ROLE, UserProfile.ROLE_USER),
            status = fieldString(KEY_STATUS, UserProfile.STATUS_ACTIVE),
            lastLoginAt = getLong(KEY_LAST_LOGIN) ?: 0L
        )
    }

    private fun DocumentSnapshot.fieldString(key: String, fallback: String): String =
        getString(key)?.takeIf { it.isNotBlank() } ?: fallback

    private fun UserProfile.toMap() = mapOf(
        KEY_UID to uid,
        KEY_NAME to displayName,
        KEY_EMAIL to email,
        KEY_CREATED_AT to createdAt,
        KEY_ROLE to role,
        KEY_STATUS to status,
        KEY_LAST_LOGIN to lastLoginAt
    )

    private companion object {
        const val COLLECTION = "users"
        const val KEY_UID = "uid"
        const val KEY_NAME = "displayName"
        const val KEY_EMAIL = "email"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_ROLE = "role"
        const val KEY_STATUS = "status"
        const val KEY_LAST_LOGIN = "lastLoginAt"
    }
}
