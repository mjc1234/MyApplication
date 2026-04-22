package com.mjc.feature.download.controller

import com.mjc.core.download.model.DownloadStatus
import com.mjc.core.download.model.DownloadTask
import com.mjc.feature.download.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 下载控制器实现
 * 协调下载仓库和UI层，提供统一的业务接口
 */
class DownloadControllerImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : DownloadController {

    override suspend fun startDownload(url: String, destination: String): String {
        return downloadRepository.startOrResumeDownload(url, destination)
    }

    override suspend fun pauseDownload(taskId: String) {
        downloadRepository.pauseDownload(taskId)
    }

    override suspend fun resumeDownload(taskId: String) {
        downloadRepository.resumeDownload(taskId)
    }

    override suspend fun cancelDownload(taskId: String) {
        downloadRepository.cancelDownload(taskId)
    }

    override fun getDownloadProgress(taskId: String): Flow<Float> {
        return downloadRepository.getTaskStateFlow(taskId)
            ?.map { it.progress }
            ?: emptyFlow()
    }

    override fun getDownloadSpeed(taskId: String): Flow<Long> {
        return downloadRepository.getTaskStateFlow(taskId)
            ?.map { it.speedBytesPerSecond }
            ?: emptyFlow()
    }

    override fun getTaskFlow(taskId: String): Flow<DownloadTaskInfo> {
        return downloadRepository.getTaskStateFlow(taskId)
            ?.map { it.toDownloadTaskInfo() }
            ?: emptyFlow()
    }

    override fun getAllDownloads(): Flow<List<DownloadTaskInfo>> {
        // 转换为UI模型
        return downloadRepository.getAllTasksFlow()
            .map { tasks ->
                tasks.map { task ->
                    task.toDownloadTaskInfo()
                }
            }
    }

    /**
     * 获取下载任务详情
     */
    suspend fun getDownloadTask(taskId: String): DownloadTask? {
        return downloadRepository.getTask(taskId)
    }

    /**
     * 批量暂停所有下载任务
     */
    suspend fun pauseAllDownloads(): Boolean {
        val tasks = downloadRepository.getAllTasks()
            .filter { it.status == DownloadStatus.DOWNLOADING }

        tasks.forEach { task ->
            downloadRepository.pauseDownload(task.id)
        }

        return tasks.isNotEmpty()
    }

    /**
     * 批量恢复所有暂停的任务
     */
    suspend fun resumeAllDownloads(): Boolean {
        val tasks = downloadRepository.getAllTasks()
            .filter { it.status == DownloadStatus.PAUSED }

        tasks.forEach { task ->
            if (task.canResume()) {
                downloadRepository.resumeDownload(task.id)
            }
        }

        return tasks.isNotEmpty()
    }

    /**
     * 批量取消所有任务
     */
    suspend fun cancelAllDownloads(): Boolean {
        val tasks = downloadRepository.getAllTasks()
            .filter { it.status in setOf(
                DownloadStatus.DOWNLOADING,
                DownloadStatus.PAUSED,
                DownloadStatus.PENDING
            ) }

        tasks.forEach { task ->
            downloadRepository.cancelDownload(task.id)
        }

        return tasks.isNotEmpty()
    }

    /**
     * 获取活跃下载任务数量
     */
    suspend fun getActiveDownloadCount(): Int {
        return downloadRepository.getAllTasks()
            .count { it.status == DownloadStatus.DOWNLOADING }
    }

    /**
     * 清理已完成的下载任务记录
     */
    suspend fun cleanupCompletedDownloads(): Int {
        // 注意：这里只是从内存中清理，实际文件已保存
        // 如果需要持久化存储，需要数据库支持
        return 0 // 目前仅返回0，未来可以扩展
    }

    /**
     * 将领域模型转换为UI模型
     */
    private fun DownloadTask.toDownloadTaskInfo(): DownloadTaskInfo {
        return DownloadTaskInfo(
            id = id,
            url = url,
            fileName = fileName,
            destination = destinationPath,
            status = status,
            progress = progress,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            speedBytesPerSecond = speedBytesPerSecond
        )
    }
}


/**
 * 创建空流
 */
private fun <T> emptyFlow(): Flow<T> {
    return kotlinx.coroutines.flow.emptyFlow()
}