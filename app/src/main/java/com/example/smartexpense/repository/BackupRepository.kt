package com.example.smartexpense.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.smartexpense.BuildConfig
import com.example.smartexpense.R
import com.example.smartexpense.repository.CsvRepository.ImportResult
import com.example.smartexpense.utils.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sao lưu / khôi phục dữ liệu hồ sơ (mục 5.8 - nhóm Dữ liệu).
 *
 * - BACKUP: dựng lại accounts.csv từ Room rồi chép thành một tệp mới
 *   `smartexpense_backup_yyyyMMdd_HHmmss.csv` vào thư mục người dùng chọn
 *   (Storage Access Framework, không cần quyền bộ nhớ) hoặc thư mục mặc định
 *   của app: Android/data/<package>/files/backups.
 * - RESTORE: đọc tệp CSV người dùng chọn và gộp vào Room + dựng lại accounts.csv
 *   (dùng lại [CsvRepository.importFrom] để giữ nguyên quy tắc chống trùng uid).
 * - TỰ ĐỘNG SAO LƯU: khi phát hiện app vừa được cập nhật lên versionCode mới.
 */
object BackupRepository {

    private const val DIR = "backups"
    private const val MIME_CSV = "text/csv"

    /** Thư mục mặc định (bộ nhớ ngoài riêng của app, không cần quyền). */
    fun defaultDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, DIR)

    /** Tên tệp sao lưu theo thời gian, vd smartexpense_backup_20261030_104200.csv */
    fun buildFileName(now: Long = System.currentTimeMillis()): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        return "smartexpense_backup_$stamp.csv"
    }

    /** Đường dẫn hiển thị cho người dùng (mặc định = đường dẫn tuyệt đối trong bộ nhớ app). */
    fun displayPath(context: Context, treeUri: String?): String {
        if (treeUri.isNullOrBlank()) return defaultDir(context).absolutePath
        return runCatching {
            val documentId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
            // "primary:Documents/Backup" -> "Documents/Backup"
            documentId.substringAfter(':', documentId).ifBlank { documentId }
        }.getOrDefault(treeUri)
    }

    fun isDefaultPath(treeUri: String?): Boolean = treeUri.isNullOrBlank()

    /**
     * Tạo một bản sao lưu mới.
     * @return đường dẫn (hoặc tên thư mục + tên tệp) để hiển thị cho người dùng
     */
    suspend fun backup(context: Context, treeUri: String?): String = withContext(Dispatchers.IO) {
        // Luôn dựng lại CSV từ Room trước khi sao lưu để bản sao là mới nhất
        CsvRepository.rebuildFromRoom(context)
        val source = CsvRepository.csvFile(context)
        if (!source.exists() || source.length() == 0L) {
            error(context.getString(R.string.backup_no_data))
        }

        val fileName = buildFileName()
        if (isDefaultPath(treeUri)) {
            val dir = defaultDir(context).apply { mkdirs() }
            val target = File(dir, fileName)
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.absolutePath
        } else {
            val tree = Uri.parse(treeUri)
            val resolver = context.contentResolver
            val document = createDocumentInTree(context, tree, fileName)
            resolver.openOutputStream(document)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error(context.getString(R.string.backup_write_failed))
            displayPath(context, treeUri) + "/" + fileName
        }
    }

    /**
     * Tạo tệp mới trong thư mục người dùng chọn.
     *
     * `DocumentsContract.createDocument` yêu cầu URI của **document cha**, không phải
     * tree URI trả về từ `OpenDocumentTree`. Vì vậy phải dựng lại bằng
     * [DocumentsContract.buildDocumentUriUsingTree] + [DocumentsContract.getTreeDocumentId].
     * Một số provider còn cần quyền ghi còn hiệu lực, nếu không sẽ ném SecurityException.
     */
    private fun createDocumentInTree(context: Context, tree: Uri, fileName: String): Uri {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree)
        )
        val document = runCatching {
            DocumentsContract.createDocument(resolver, parent, MIME_CSV, fileName)
        }.getOrNull()
        if (document != null) return document

        // Một số nhà cung cấp (vd Files của AOSP) chỉ chấp nhận tree URI trực tiếp.
        return runCatching {
            DocumentsContract.createDocument(resolver, tree, MIME_CSV, fileName)
        }.getOrNull() ?: error(context.getString(R.string.backup_write_failed))
    }

    /** Khôi phục từ tệp CSV người dùng chọn (chỉ admin - kiểm tra ở UI). */
    suspend fun restore(context: Context, uri: Uri): ImportResult =
        CsvRepository.importFrom(context, uri)

    /**
     * Tự động sao lưu khi app vừa được cập nhật lên phiên bản mới (công tắc Automatic backup).
     * Chạy ngầm lúc mở app, bỏ qua nếu tắt công tắc hoặc đã sao lưu cho versionCode hiện tại.
     */
    suspend fun autoBackupIfVersionChanged(context: Context) = withContext(Dispatchers.IO) {
        val prefs = AppPrefs.get(context)
        if (!prefs.autoBackup) return@withContext
        if (prefs.lastBackupVersionCode == BuildConfig.VERSION_CODE) return@withContext
        runCatching {
            backup(context, prefs.backupTreeUri)
            prefs.lastBackupVersionCode = BuildConfig.VERSION_CODE
            prefs.lastBackupAt = System.currentTimeMillis()
        }
    }
}
