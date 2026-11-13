package com.example.smartexpense.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lưu ảnh đại diện của người dùng (màn Account):
 * ảnh được sao chép vào bộ nhớ riêng của app và thu nhỏ còn tối đa 512px
 * để không phình dung lượng. Đường dẫn lưu trong [AppPrefs.avatarPath].
 */
object AvatarStore {

    private const val MAX_SIZE = 512
    private const val DIR = "avatars"

    /** Đọc ảnh đã lưu; trả về null nếu chưa có hoặc file bị xóa. */
    fun load(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    /**
     * Sao chép ảnh người dùng chọn (photo picker) vào filesDir/avatars.
     * Trả về đường dẫn tuyệt đối, hoặc null nếu không đọc được ảnh.
     */
    suspend fun save(context: Context, uid: String, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

                var sample = 1
                while (bounds.outWidth / sample > MAX_SIZE ||
                    bounds.outHeight / sample > MAX_SIZE
                ) {
                    sample *= 2
                }

                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return@runCatching null

                val dir = File(context.filesDir, DIR).apply { mkdirs() }
                val target = File(dir, "avatar_$uid.jpg")
                target.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                target.absolutePath
            }.getOrNull()
        }
}
