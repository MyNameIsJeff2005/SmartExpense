package com.example.smartexpense.repository

import com.example.smartexpense.R

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.smartexpense.data.local.AppDatabase
import com.example.smartexpense.model.UserProfile
import com.example.smartexpense.utils.AppPrefs
import com.example.smartexpense.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime

/**
 * Quản lý accounts.csv trong filesDir (mục 3.2, 3.2a, 3.2c):
 * - Đăng ký/login: thêm dòng mới, chống trùng theo uid
 * - File bị xóa: tự dựng lại từ Room theo công tắc của admin
 * - Ghi file kiểu an toàn: ghi file tạm rồi đổi tên (tránh hỏng CSV giữa chừng)
 * - XUẤT ra file do người dùng chọn (SAF) và NHẬP từ file CSV bất kỳ (mục 3.2c)
 */
object CsvRepository {

    const val FILE_NAME = "accounts.csv"
    private const val HEADER = "uid,display_name,email,created_at"

    fun csvFile(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Thêm 1 hồ sơ: nếu file chưa tồn tại thì dựng lại từ Room trước; không ghi trùng uid */
    suspend fun ensureProfileAdded(context: Context, profile: UserProfile) = withContext(Dispatchers.IO) {
        val file = csvFile(context)
        if (!file.exists()) {
            rebuildFromRoom(context)
        }
        if (!containsUid(file, profile.uid)) {
            file.appendText(toCsvLine(profile) + "\n", Charsets.UTF_8)
        }
    }

    /** Dựng lại file từ toàn bộ hồ sơ trong Room (bản dựng đầy đủ, không mất hồ sơ) */
    suspend fun rebuildFromRoom(context: Context) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.get(context).userProfileDao()
        val profiles = dao.getAllOnce()
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(HEADER)
            writer.write("\n")
            profiles.forEach { p ->
                writer.write(toCsvLine(p))
                writer.write("\n")
            }
        }
        val target = csvFile(context)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    /** Tự kiểm tra khi mở app / đăng nhập: chỉ dựng lại nếu công tắc đang BẬT (mục 3.2a) */
    suspend fun autoRecoverIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val switchOn = AppPrefs.get(context).autoRecreateCsv
        if (switchOn && !csvFile(context).exists()) {
            rebuildFromRoom(context)
        }
    }

    private fun containsUid(file: File, uid: String): Boolean =
        runCatching {
            file.useLines(Charsets.UTF_8) { lines ->
                lines.drop(1).any { it.split(',').firstOrNull()?.trim('"') == uid }
            }
        }.getOrDefault(false)

    data class CsvStatus(
        val exists: Boolean,
        val sizeBytes: Long,
        val dataRows: Int,
        val lastModified: Long,
        val roomCount: Int
    ) {
        val mismatch: Boolean get() = !exists || dataRows != roomCount
    }

    suspend fun status(context: Context): CsvStatus = withContext(Dispatchers.IO) {
        val file = csvFile(context)
        val roomCount = AppDatabase.get(context).userProfileDao().countProfiles()
        if (file.exists()) {
            val rows = runCatching { file.useLines(Charsets.UTF_8) { it.count() - 1 } }.getOrDefault(0)
            CsvStatus(true, file.length(), rows.coerceAtLeast(0), file.lastModified(), roomCount)
        } else {
            CsvStatus(false, 0, 0, 0, roomCount)
        }
    }

    /** Xóa file (chỉ admin, phải có xác nhận 2 bước ở UI - mục 3.2c) */
    fun delete(context: Context): Boolean = csvFile(context).delete()

    /** Chia sẻ/bản sao qua FileProvider - không sửa file gốc (mục 3.2a) */
    fun shareUri(context: Context): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile(context))

    // ==================== XUẤT / NHẬP CSV (mục 3.2c) ====================

    data class ImportResult(
        val inserted: Int,
        val updated: Int,
        val skipped: Int
    ) {
        val total: Int get() = inserted + updated
    }

    /**
     * XUẤT CSV ra vị trí người dùng chọn (Storage Access Framework, không cần quyền bộ nhớ).
     * Luôn dựng lại file từ Room trước để bản xuất là đầy đủ nhất.
     * @return số dòng dữ liệu đã ghi
     */
    suspend fun exportTo(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        rebuildFromRoom(context)
        val file = csvFile(context)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        } ?: error(context.getString(R.string.csv_err_write_target))
        val rows = runCatching { file.useLines(Charsets.UTF_8) { it.count() - 1 } }.getOrDefault(0)
        rows.coerceAtLeast(0)
    }

    /**
     * NHẬP CSV từ file người dùng chọn: đọc -> kiểm tra -> gộp vào Room -> dựng lại accounts.csv.
     * - uid trùng hồ sơ đã có: cập nhật tên/email/ngày tạo (giữ nguyên role/status đang có)
     * - uid mới: thêm hồ sơ mới với role USER, trạng thái ACTIVE
     * - dòng thiếu uid hoặc sai định dạng: bỏ qua và đếm vào `skipped`
     */
    suspend fun importFrom(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error(context.getString(R.string.csv_err_read_source))

        val dao = AppDatabase.get(context).userProfileDao()
        val parsed = parseProfiles(text)
        var inserted = 0
        var updated = 0
        var skipped = 0

        parsed.forEach { candidate ->
            val uid = candidate.uid.trim()
            if (uid.isEmpty()) {
                skipped++
                return@forEach
            }
            val existing = dao.getByUid(uid)
            if (existing == null) {
                dao.upsert(candidate.copy(uid = uid))
                inserted++
            } else {
                dao.upsert(
                    existing.copy(
                        displayName = candidate.displayName.ifBlank { existing.displayName },
                        email = candidate.email.ifBlank { existing.email },
                        createdAt = candidate.createdAt
                    )
                )
                updated++
            }
        }

        // Đồng bộ lại file accounts.csv theo dữ liệu vừa gộp
        rebuildFromRoom(context)
        ImportResult(inserted = inserted, updated = updated, skipped = skipped)
    }

    /**
     * Đọc nội dung CSV thành danh sách hồ sơ.
     * Hỗ trợ trường có dấu phẩy/ngoặc kép/xuống dòng (chuẩn RFC 4180) và tự nhận cột theo header.
     */
    fun parseProfiles(text: String): List<UserProfile> {
        val records = parseRecords(text)
        if (records.isEmpty()) return emptyList()

        // Xác định cột theo header; nếu không có header thì dùng đúng thứ tự chuẩn
        val first = records.first().map { it.trim().lowercase() }
        val hasHeader = first.any { it == "uid" || it == "email" }
        val indexOfUid = first.indexOfFirst { it == "uid" }.takeIf { it >= 0 } ?: 0
        val indexOfName = first.indexOfFirst {
            it == "display_name" || it == "displayname" || it == "name" || it == "full_name"
        }.takeIf { it >= 0 } ?: 1
        val indexOfEmail = first.indexOfFirst { it == "email" }.takeIf { it >= 0 } ?: 2
        val indexOfCreated = first.indexOfFirst {
            it == "created_at" || it == "createdat" || it == "created"
        }.takeIf { it >= 0 } ?: 3

        val rows = if (hasHeader) records.drop(1) else records
        return rows.mapNotNull { row ->
            val uid = row.getOrNull(indexOfUid)?.trim().orEmpty()
            if (uid.isEmpty()) return@mapNotNull null
            UserProfile(
                uid = uid,
                displayName = row.getOrNull(indexOfName)?.trim().orEmpty().ifBlank { uid },
                email = row.getOrNull(indexOfEmail)?.trim().orEmpty(),
                createdAt = parseCreatedAt(row.getOrNull(indexOfCreated).orEmpty()),
                role = UserProfile.ROLE_USER,
                status = UserProfile.STATUS_ACTIVE,
                lastLoginAt = 0L
            )
        }
    }

    /** Tách bản ghi CSV có hỗ trợ ngoặc kép và xuống dòng bên trong ô. */
    private fun parseRecords(text: String): List<List<String>> {
        val source = text.removePrefix("\uFEFF")
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < source.length) {
            val c = source[i]
            when {
                inQuotes && c == '"' -> {
                    if (i + 1 < source.length && source[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                inQuotes -> cell.append(c)
                c == '"' -> inQuotes = true
                c == ',' -> {
                    fields.add(cell.toString())
                    cell.clear()
                }
                c == '\n' -> {
                    fields.add(cell.toString())
                    cell.clear()
                    records.add(fields)
                    fields = mutableListOf()
                }
                c == '\r' -> Unit // bỏ qua, "\r\n" đã xử lý ở '\n'
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(cell.toString())
            records.add(fields)
        }
        // Bỏ các dòng hoàn toàn trống
        return records.filter { row -> row.any { it.isNotBlank() } }
    }

    /** created_at: chấp nhận epoch millis hoặc ISO-8601 (vd 2026-10-29T17:00:00+07:00). */
    private fun parseCreatedAt(raw: String): Long {
        val value = raw.trim()
        if (value.isEmpty()) return System.currentTimeMillis()
        value.toLongOrNull()?.let { return it }
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
    }

    // ---------- Định dạng dòng CSV ----------

    private fun toCsvLine(p: UserProfile): String {
        val createdAtIso = DateTimeUtils.toIsoOffsetString(p.createdAt)
        // KHÔNG BAO GIỜ ghi mật khẩu/token vào CSV (mục 3.2)
        return listOf(p.uid, p.displayName, p.email, createdAtIso)
            .joinToString(",") { it.escapeCsv() }
    }

    private fun String.escapeCsv(): String =
        if (contains(',') || contains('"') || contains('\n') || contains('\r')) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }
}
