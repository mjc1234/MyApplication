package com.mjc.core.download.executor

import android.util.Log
import com.mjc.core.download.config.DownloadConfig
import com.mjc.core.download.service.DownloadService
import com.mjc.core.download.utils.SpeedCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import javax.inject.Inject

/**
 * 下载执行器
 * 封装通用的文件下载逻辑，支持断点续传和进度回调
 */
class DownloadExecutor @Inject constructor(
    private val downloadService: DownloadService,
    private val config: DownloadConfig = DownloadConfig.default()
) {

    /**
     * 执行下载
     * @param url 文件URL
     * @param targetFile 目标文件
     * @param resumeFromBytes 从指定字节位置继续下载（断点续传）
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    suspend fun execute(
        url: String,
        targetFile: File,
        resumeFromBytes: Long = 0L,
        etag: String? = null,
        lastModified: String? = null,
        progressCallback: DownloadProgressCallback? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        val speedCalculator = SpeedCalculator()
        var downloadedBytes = resumeFromBytes
        var totalBytes = 0L
        var lastProgressUpdateTime = System.currentTimeMillis()
        var lastBytesUpdate = downloadedBytes

        // 确保父目录存在
        targetFile.parentFile?.mkdirs()

        val randomAccessFile = RandomAccessFile(targetFile, "rw")

        try {
            // 定位到续传位置
            if (resumeFromBytes > 0) {
                randomAccessFile.seek(resumeFromBytes)
            } else {
                randomAccessFile.setLength(0)
            }

            // 构建Range头
            val rangeHeader = if (downloadedBytes > 0) {
                "bytes=$downloadedBytes-"
            } else null

            Log.d("Debug", "start download range : $rangeHeader")

            // 执行下载请求
            val response = downloadService.downloadFile(
                url = url,
                range = rangeHeader,
                ifRange = if (downloadedBytes > 0) (etag ?: lastModified) else null
            )

            Log.d("Debug", "download response : ${response.statusCode}")
            if (!response.isSuccessful && response.statusCode != 206) {
                return@withContext DownloadResult.failure(
                    IOException("下载失败: HTTP ${response.statusCode}"),
                    downloadedBytes
                )
            }

            val inputStream = response.bodyStream
                ?: return@withContext DownloadResult.failure(IOException("响应体为空"), downloadedBytes)

            // 计算总大小
            val contentLength = response.contentLength
            if (contentLength > 0) {
                totalBytes = if (response.statusCode == 206) {
                    downloadedBytes + contentLength
                } else {
                    contentLength
                }
            }

            // 从响应头获取总大小（Content-Range: bytes 0-100/200）
            if (totalBytes == 0L) {
                val contentRange = response.headers["Content-Range"]
                if (contentRange != null) {
                    totalBytes = parseTotalFromContentRange(contentRange)
                }
            }

            progressCallback?.onProgress(DownloadProgress(downloadedBytes, totalBytes, 0L))

            // 下载循环
            val buffer = ByteArray(config.bufferSize)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // 检查是否被取消
                if (Thread.currentThread().isInterrupted) {
                    throw CancellationException("下载被中断")
                }

                // 写入文件
                randomAccessFile.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                // 更新进度（限制频率）
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastProgressUpdateTime

                if (timeSinceLastUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                    val bytesSinceLastUpdate = downloadedBytes - lastBytesUpdate
                    val speed = speedCalculator.calculateSpeed(bytesSinceLastUpdate, timeSinceLastUpdate)

                    Log.d("Debug", "update progress $downloadedBytes")
                    progressCallback?.onProgress(DownloadProgress(downloadedBytes, totalBytes, speed))

                    lastProgressUpdateTime = currentTime
                    lastBytesUpdate = downloadedBytes
                }
            }

            // 最终进度更新
            progressCallback?.onProgress(DownloadProgress(downloadedBytes, totalBytes, 0L))

            return@withContext DownloadResult.success(downloadedBytes, totalBytes)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext DownloadResult.failure(e, downloadedBytes)
        } finally {
            randomAccessFile.close()
        }
    }

    /**
     * 获取文件信息
     */
    suspend fun getFileInfo(url: String): FileInfoResult {
        return try {
            val response = downloadService.getFileInfo(url)
            if (response.isSuccessful) {
                FileInfoResult(
                    success = true,
                    contentLength = response.headers["Content-Length"]?.toLongOrNull(),
                    contentType = response.headers["Content-Type"],
                    etag = response.headers["ETag"],
                    lastModified = response.headers["Last-Modified"],
                    acceptRanges = response.headers["Accept-Ranges"]
                )
            } else {
                FileInfoResult(success = false, error = "HTTP ${response.statusCode}")
            }
        } catch (e: Exception) {
            FileInfoResult(success = false, error = e.message)
        }
    }

    private fun parseTotalFromContentRange(contentRange: String): Long {
        // 格式: bytes 0-100/200
        val pattern = "bytes \\d+-\\d+/(\\d+|\\*)".toRegex()
        val match = pattern.find(contentRange) ?: return 0L
        val total = match.groupValues[1]
        return if (total == "*") 0L else total.toLongOrNull() ?: 0L
    }

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L // 每250ms更新一次进度
    }
}

/**
 * 下载进度回调接口
 */
interface DownloadProgressCallback {
    fun onProgress(progress: DownloadProgress)
}

/**
 * 下载进度数据
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        } else 0f
}

/**
 * 下载结果
 */
data class DownloadResult(
    val success: Boolean,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val error: Throwable? = null
) {
    companion object {
        fun success(downloadedBytes: Long, totalBytes: Long) = DownloadResult(
            success = true,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes
        )

        fun failure(error: Throwable, downloadedBytes: Long) = DownloadResult(
            success = false,
            downloadedBytes = downloadedBytes,
            totalBytes = 0L,
            error = error
        )
    }
}

/**
 * 文件信息结果
 */
data class FileInfoResult(
    val success: Boolean,
    val contentLength: Long? = null,
    val contentType: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val acceptRanges: String? = null,
    val error: String? = null
) {
    val supportsRangeRequest: Boolean
        get() = acceptRanges == "bytes"
}
