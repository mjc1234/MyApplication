package com.mjc.feature.download.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.mjc.core.download.utils.DownloadUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * 文件操作工具类
 * 处理下载相关的文件操作，兼容Android 10+分区存储
 */
class FileUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val DOWNLOAD_SUBDIRECTORY = "Downloads"
        private const val TEMP_FILE_SUFFIX = ".tmp"
        private const val MIN_FREE_SPACE_BYTES = 50 * 1024 * 1024L // 50MB最小空闲空间
    }

    /**
     * 获取应用私有下载目录
     * 优先使用外部存储私有目录，如果不可用则使用内部存储
     */
    fun getDownloadDirectory(): File {
        return if (isExternalStorageWritable()) {
            // Android 10+ 使用应用私有目录，不需要权限
            val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            externalDir ?: context.filesDir
        } else {
            context.filesDir
        }.also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    /**
     * 获取公共下载目录（需要权限）
     * 注意：Android 10+需要MANAGE_EXTERNAL_STORAGE权限
     */
    @Suppress("DEPRECATION")
    fun getPublicDownloadDirectory(): File? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            // Android 10+ 需要使用MediaStore或SAF
            null
        }
    }

    /**
     * 检查是否有足够的存储空间
     * @param requiredBytes 需要的字节数
     * @param directory 目标目录（可选，默认使用下载目录）
     */
    fun hasEnoughSpace(requiredBytes: Long, directory: File = getDownloadDirectory()): Boolean {
        return try {
            val usableSpace = directory.usableSpace
            usableSpace >= requiredBytes + MIN_FREE_SPACE_BYTES
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * 创建唯一文件名，避免冲突
     * @param fileName 原始文件名
     * @param directory 目标目录
     */
    fun createUniqueFile(fileName: String, directory: File = getDownloadDirectory()): File {
        val baseName = fileName.substringBeforeLast('.', "")
        val extension = if ('.' in fileName) fileName.substringAfterLast('.') else ""

        var counter = 1
        var uniqueFile = File(directory, fileName)

        while (uniqueFile.exists()) {
            val newName = if (extension.isNotEmpty()) {
                "${baseName}_($counter).$extension"
            } else {
                "${baseName}_($counter)"
            }
            uniqueFile = File(directory, newName)
            counter++
        }

        return uniqueFile
    }

    /**
     * 创建临时文件
     * @param fileName 最终文件名（临时文件会添加.tmp后缀）
     * @param directory 目标目录
     */
    fun createTempFile(fileName: String, directory: File = getDownloadDirectory()): File {
        val tempFileName = "$fileName$TEMP_FILE_SUFFIX"
        return createUniqueFile(tempFileName, directory)
    }

    /**
     * 移动临时文件到最终位置
     * @param tempFile 临时文件
     * @param destinationDir 目标目录
     * @param fileName 最终文件名
     */
    fun moveTempFileToDestination(
        tempFile: File,
        destinationDir: String,
        fileName: String
    ): File {
        val destDir = File(destinationDir)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        val destFile = createUniqueFile(fileName, destDir)

        return try {
            if (tempFile.renameTo(destFile)) {
                destFile
            } else {
                // 如果重命名失败，尝试复制
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
                destFile
            }
        } catch (e: IOException) {
            throw IOException("移动文件失败: ${e.message}", e)
        }
    }

    /**
     * 删除临时文件
     * @param taskId 任务ID
     * @param tempFilePath 临时文件路径（可选，如果为null则查找对应的临时文件）
     */
    fun deleteTempFile(taskId: String, tempFilePath: String? = null): Boolean {
        return try {
            val file = if (tempFilePath != null) {
                File(tempFilePath)
            } else {
                // 根据任务ID查找临时文件
                val downloadDir = getDownloadDirectory()
                val tempFiles = downloadDir.listFiles { _, name ->
                    name.endsWith("_$taskId$TEMP_FILE_SUFFIX") || name.contains(taskId)
                }
                tempFiles?.firstOrNull()
            }

            file?.delete() ?: false
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * 获取文件大小（字节）
     */
    fun getFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0L
    }

    /**
     * 格式化文件大小（委托给DownloadUtils）
     */
    fun formatFileSize(bytes: Long): String {
        return DownloadUtils.formatFileSize(bytes)
    }

    /**
     * 清理旧的临时文件（超过7天）
     */
    fun cleanupOldTempFiles(maxAgeDays: Int = 7): Int {
        val downloadDir = getDownloadDirectory()
        val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)

        return downloadDir.listFiles { _, name ->
            name.endsWith(TEMP_FILE_SUFFIX)
        }?.count { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            } else {
                false
            }
        } ?: 0
    }

    /**
     * 检查外部存储是否可写
     */
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * 检查外部存储是否可读
     */
    @Suppress("unused")
    private fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
}