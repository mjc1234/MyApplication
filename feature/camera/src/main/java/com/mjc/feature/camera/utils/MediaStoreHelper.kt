package com.mjc.feature.camera.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MediaStore助手类，处理图像保存到Android MediaStore PICTURES目录
 * 适用于Android 10+（minSdkVersion = 29）
 */
object MediaStoreHelper {

    private const val IMAGE_MIME_TYPE = "image/jpeg"
    private const val RELATIVE_PATH = "Pictures/MyApplicationFolder"
    private const val FILENAME_PREFIX = "IMG_"
    private const val FILENAME_EXTENSION = ".jpg"

    /**
     * 生成时间戳格式的文件名
     */
    fun generateImageFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${FILENAME_PREFIX}${timeStamp}${FILENAME_EXTENSION}"
    }

    /**
     * 创建用于插入MediaStore的ContentValues
     * @param fileName 文件名（包含扩展名）
     * @param dateTaken 拍摄时间戳（毫秒），如果为null则使用当前时间
     */
    fun createImageContentValues(fileName: String, dateTaken: Long? = null): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, IMAGE_MIME_TYPE)

            // 设置时间戳
            val now = System.currentTimeMillis() / 1000
            put(MediaStore.Images.Media.DATE_ADDED, now)
            put(MediaStore.Images.Media.DATE_MODIFIED, now)

            dateTaken?.let {
                put(MediaStore.Images.Media.DATE_TAKEN, it)
            }

            // Android 10+ 特性 (minSdkVersion = 29，所以总是可用)
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    /**
     * 获取MediaStore图片内容URI
     * 注意：minSdkVersion = 29 (Android 10)，所以总是使用VOLUME_EXTERNAL_PRIMARY
     */
    fun getImagesContentUri(): Uri {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    /**
     * 检查是否支持IS_PENDING标志（Android 10+）
     * 注意：minSdkVersion = 29，所以总是返回true
     */
    fun supportsPendingFlag(): Boolean {
        return true
    }
}