package com.mjc.feature.download.controller

import com.mjc.core.download.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * 下载控制器
 * 处理下载业务逻辑
 */
interface DownloadController {

    /**
     * 开始下载
     * @param url 文件URL
     * @param destination 保存路径
     * @return 下载任务ID
     */
    suspend fun startDownload(url: String, destination: String): String

    /**
     * 暂停下载
     * @param taskId 下载任务ID
     */
    suspend fun pauseDownload(taskId: String)

    /**
     * 继续下载
     * @param taskId 下载任务ID
     */
    suspend fun resumeDownload(taskId: String)

    /**
     * 取消下载
     * @param taskId 下载任务ID
     */
    suspend fun cancelDownload(taskId: String)

    /**
     * 获取下载进度
     * @param taskId 下载任务ID
     * @return 进度流（0.0 - 1.0）
     */
    fun getDownloadProgress(taskId: String): Flow<Float>

    /**
     * 获取下载速度
     * @param taskId 下载任务ID
     * @return 速度（字节/秒）
     */
    fun getDownloadSpeed(taskId: String): Flow<Long>

    /**
     * 获取下载任务状态流
     * @param taskId 下载任务ID
     * @return 任务信息流，包含进度、速度、状态等完整信息
     */
    fun getTaskFlow(taskId: String): Flow<DownloadTaskInfo>

    /**
     * 获取所有下载任务
     */
    fun getAllDownloads(): Flow<List<DownloadTaskInfo>>
}

/**
 * 下载任务信息
 */
data class DownloadTaskInfo(
    val id: String,
    val url: String,
    val fileName: String,
    val destination: String,
    val status: DownloadStatus,
    val progress: Float,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val speedBytesPerSecond: Long
)