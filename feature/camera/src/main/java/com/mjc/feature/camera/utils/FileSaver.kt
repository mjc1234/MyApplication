package com.mjc.feature.camera.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文件保存工具类
 */
object FileSaver {

    /**
     * 创建图片文件
     */
    fun createImageFile(context: Context, prefix: String = "IMG"): File {
        return createMediaFile(
            context = context,
            directory = Environment.DIRECTORY_PICTURES,
            prefix = prefix,
            extension = ".jpg"
        )
    }

    /**
     * 创建视频文件
     */
    fun createVideoFile(context: Context, prefix: String = "VID"): File {
        return createMediaFile(
            context = context,
            directory = Environment.DIRECTORY_MOVIES,
            prefix = prefix,
            extension = ".mp4"
        )
    }

    /**
     * 创建媒体文件（通用方法）
     */
    private fun createMediaFile(
        context: Context,
        directory: String,
        prefix: String,
        extension: String
    ): File {
        // 获取存储目录
        val storageDir = context.getExternalFilesDir(directory)
            ?: context.filesDir

        // 创建时间戳
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // 创建文件名
        val fileName = "${prefix}_${timeStamp}${extension}"

        return File(storageDir, fileName).apply {
            // 确保父目录存在
            parentFile?.mkdirs()
        }
    }

    /**
     * 检查存储空间是否足够
     */
    fun hasEnoughStorage(context: Context, requiredBytes: Long = 100 * 1024 * 1024): Boolean {
        return try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir

            val usableSpace = storageDir.usableSpace
            usableSpace > requiredBytes
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取文件URI
     */
    fun getFileUri(context: Context, file: File): Uri {
        return Uri.fromFile(file)
    }

    /**
     * 删除文件
     */
    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取文件大小（人类可读格式）
     */
    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * 获取应用专属存储目录中的文件列表
     */
    fun listMediaFiles(context: Context, directory: String): List<File> {
        return try {
            val storageDir = context.getExternalFilesDir(directory)
                ?: context.filesDir

            storageDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".mp4"))
            }?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清理旧文件（保留最近N个文件）
     */
    fun cleanupOldFiles(context: Context, directory: String, keepCount: Int = 50) {
        try {
            val files = listMediaFiles(context, directory)
                .sortedByDescending { it.lastModified() }

            if (files.size > keepCount) {
                files.subList(keepCount, files.size).forEach { file ->
                    deleteFile(file)
                }
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }

    /**
     * 创建MediaStore图片URI（Android 10+）
     * @return 图片的MediaStore URI，失败返回null
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun createMediaStoreImageUri(context: Context, fileName: String? = null): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName ?: "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MyApplication/")
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        }

        return try {
            context.contentResolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                contentValues
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将MediaStore中的待处理图片标记为已完成
     * 调用此方法后，图片将在图库中可见
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun finalizeMediaStoreImage(context: Context, uri: Uri) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        try {
            context.contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 清理失败的MediaStore图片
     */
    fun cleanupMediaStoreImage(context: Context, uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // 忽略错误
        }
    }
}