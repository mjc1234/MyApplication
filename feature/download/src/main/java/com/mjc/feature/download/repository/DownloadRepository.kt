package com.mjc.feature.download.repository

import com.mjc.core.download.executor.DownloadEvent
import com.mjc.core.download.executor.DownloadExecutor
import com.mjc.core.download.model.DownloadStatus
import com.mjc.core.download.model.DownloadTask
import com.mjc.core.download.model.ResumeData
import com.mjc.feature.download.utils.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * 下载仓库
 * 处理下载业务逻辑，支持断点续传
 *
 * 使用 DownloadExecutor 执行实际下载，自身负责任务管理
 */
class DownloadRepository @Inject constructor(
    private val executor: DownloadExecutor,
    private val fileUtils: FileUtils
) {
    private val downloadTasks = ConcurrentHashMap<String, DownloadTask>()
    private val taskStateFlows = ConcurrentHashMap<String, MutableStateFlow<DownloadTask>>()
    private val allTasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    private val mutex = Mutex()

    // 用于取消下载的标志
    private val cancelFlags = ConcurrentHashMap<String, Boolean>()

    /**
     * 开始或恢复下载
     * @param url 文件URL
     * @param destinationPath 目标路径（目录）
     * @param fileName 文件名（可选）
     * @return 下载任务ID
     */
    suspend fun startOrResumeDownload(
        url: String,
        destinationPath: String,
        fileName: String? = null
    ): String = withContext(Dispatchers.IO) {
        // 锁仅保护任务状态的查找/创建（短临界区）
        val (taskId, isResume) = mutex.withLock {
            val existingTask = findTaskByUrl(url)
            if (existingTask != null) {
                if (existingTask.canResume() && existingTask.status == DownloadStatus.PAUSED) {
                    cancelFlags[existingTask.id] = false
                    updateTask(existingTask.copy(
                        status = DownloadStatus.DOWNLOADING,
                        updatedAt = System.currentTimeMillis()
                    ))
                    return@withLock existingTask.id to true
                }
                if (existingTask.status == DownloadStatus.DOWNLOADING) {
                    return@withContext existingTask.id
                }
            }

            // 创建新任务
            val taskId = generateTaskId()
            val actualFileName = fileName ?: extractFileNameFromUrl(url)

            val task = DownloadTask(
                id = taskId,
                url = url,
                fileName = actualFileName,
                destinationPath = destinationPath,
                status = DownloadStatus.PENDING,
                progress = 0f,
                totalBytes = 0L,
                downloadedBytes = 0L,
                speedBytesPerSecond = 0L,
                resumeData = null
            )

            downloadTasks[taskId] = task
            taskStateFlows[taskId] = MutableStateFlow(task)
            cancelFlags[taskId] = false
            taskId to false
        }

        // 下载执行在锁外，不阻塞 pauseDownload/cancelDownload
        startDownloadInternal(taskId, isResume)
        taskId
    }

    /**
     * 暂停下载
     */
    suspend fun pauseDownload(taskId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val task = downloadTasks[taskId] ?: return@withContext false

            if (task.status != DownloadStatus.DOWNLOADING) return@withContext false

            // 设置取消标志（下载循环会检查）
            cancelFlags[taskId] = true

            val updatedTask = task.copy(
                status = DownloadStatus.PAUSED,
                speedBytesPerSecond = 0L,
                updatedAt = System.currentTimeMillis()
            )
            updateTask(updatedTask)

            return@withContext true
        }
    }

    /**
     * 恢复下载
     */
    suspend fun resumeDownload(taskId: String): String = withContext(Dispatchers.IO) {
        val restartInfo = mutex.withLock {
            val task = downloadTasks[taskId]
                ?: throw IllegalArgumentException("任务不存在: $taskId")

            if (task.status != DownloadStatus.PAUSED) {
                throw IllegalStateException("只有暂停的任务才能恢复")
            }

            if (!task.canResume()) {
                // 无法续传，提取任务信息后在锁外重新开始
                val info = Triple(task.url, task.destinationPath, task.fileName)
                removeTask(taskId)
                return@withLock info
            }

            cancelFlags[taskId] = false
            updateTask(task.copy(
                status = DownloadStatus.DOWNLOADING,
                updatedAt = System.currentTimeMillis()
            ))
            null // 信号：可以续传，无需重启
        }

        if (restartInfo != null) {
            return@withContext startOrResumeDownload(
                restartInfo.first, restartInfo.second, restartInfo.third
            )
        }

        startDownloadInternal(taskId, true)
        taskId
    }

    /**
     * 取消下载
     */
    suspend fun cancelDownload(taskId: String, deleteTempFile: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val task = mutex.withLock {
            val t = downloadTasks[taskId] ?: return@withContext false
            cancelFlags[taskId] = true
            updateTask(t.copy(
                status = DownloadStatus.CANCELLED,
                speedBytesPerSecond = 0L,
                updatedAt = System.currentTimeMillis()
            ))
            cancelFlags.remove(taskId)
            t
        }

        // 文件删除在锁外执行
        if (deleteTempFile) {
            task.resumeData?.let { resumeData ->
                File(resumeData.tempFilePath).delete()
            }
        }
        true
    }

    /**
     * 获取任务状态流
     */
    fun getTaskStateFlow(taskId: String): Flow<DownloadTask>? {
        return taskStateFlows[taskId]
    }

    /**
     * 获取所有任务
     */
    fun getAllTasks(): List<DownloadTask> {
        return downloadTasks.values.toList()
    }

    /**
     * 获取所有任务状态流
     */
    fun getAllTasksFlow(): StateFlow<List<DownloadTask>> = allTasksFlow

    /**
     * 获取任务
     */
    fun getTask(taskId: String): DownloadTask? {
        return downloadTasks[taskId]
    }

    // region 内部方法

    private suspend fun startDownloadInternal(taskId: String, isResume: Boolean = false) {
        try {
            if (!isResume) {
                mutex.withLock {
                    val task = downloadTasks[taskId] ?: return@withLock
                    updateTask(task.copy(
                        status = DownloadStatus.DOWNLOADING,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }

            downloadFile(taskId, isResume)
        } catch (_: CancellationException) {
            mutex.withLock {
                downloadTasks[taskId]?.let { task ->
                    updateTask(task.copy(
                        status = DownloadStatus.CANCELLED,
                        speedBytesPerSecond = 0L,
                        updatedAt = System.currentTimeMillis()
                    ))
                    removeTask(taskId)
                }
            }
            throw CancellationException()
        } catch (_: Exception) {
            mutex.withLock {
                downloadTasks[taskId]?.let { task ->
                    updateTask(task.copy(
                        status = DownloadStatus.FAILED,
                        speedBytesPerSecond = 0L,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
        }
    }

    private suspend fun downloadFile(taskId: String, isResume: Boolean) {
        val task = downloadTasks[taskId] ?: return

        val resumeFromBytes = if (isResume && task.canResume()) task.downloadedBytes else 0L
        val tempFilePath = if (isResume && task.canResume()) {
            task.resumeData?.tempFilePath ?: createTempFilePath(task.destinationPath, task.fileName)
        } else {
            createTempFilePath(task.destinationPath, task.fileName)
        }

        val tempFile = File(tempFilePath)
        if (!isResume && tempFile.exists()) {
            tempFile.delete()
        }

        // 获取文件信息
        var totalBytes = task.totalBytes
        var etag: String? = task.resumeData?.etag
        var lastModified: String? = task.resumeData?.lastModified

        if (totalBytes == 0L) {
            val fileInfo = executor.getFileInfo(task.url)
            if (fileInfo.success) {
                totalBytes = fileInfo.contentLength ?: 0L
                etag = fileInfo.etag
                lastModified = fileInfo.lastModified
            }
        }

        var lastKnownDownloadedBytes = resumeFromBytes

        try {
            // 使用 Flow 收集下载事件（Executor 已以 250ms 间隔节流）
            executor.execute(
                url = task.url,
                targetFile = tempFile,
                resumeFromBytes = resumeFromBytes,
                etag = etag,
                lastModified = lastModified
            ).collect { event ->
                when (event) {
                    is DownloadEvent.Progress -> {
                        // 检查是否需要取消/暂停
                        if (cancelFlags[taskId] == true) {
                            throw CancellationException("下载被用户取消")
                        }

                        val currentTask = downloadTasks[taskId]
                        if (currentTask?.status != DownloadStatus.DOWNLOADING) {
                            throw CancellationException("下载状态已变更")
                        }

                        lastKnownDownloadedBytes = event.progress.downloadedBytes

                        val updatedTask = currentTask.copy(
                            downloadedBytes = event.progress.downloadedBytes,
                            totalBytes = event.progress.totalBytes,
                            progress = event.progress.progress,
                            speedBytesPerSecond = event.progress.speedBytesPerSecond,
                            updatedAt = System.currentTimeMillis()
                        )
                        updateTask(updatedTask)
                    }
                    is DownloadEvent.Success -> {
                        // 下载完成，移动文件
                        fileUtils.moveTempFileToDestination(tempFile, task.destinationPath, task.fileName)

                        val completedTask = downloadTasks[taskId]?.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 1f,
                            downloadedBytes = event.result.downloadedBytes,
                            speedBytesPerSecond = 0L,
                            resumeData = null,
                            updatedAt = System.currentTimeMillis()
                        ) ?: return@collect

                        updateTask(completedTask)
                        removeTask(taskId)
                    }
                    is DownloadEvent.Failure -> {
                        throw event.result.error ?: java.io.IOException("下载失败")
                    }
                }
            }
        } catch (e: CancellationException) {
            // 保存断点信息（暂停状态）
            if (downloadTasks[taskId]?.status == DownloadStatus.PAUSED) {
                val resumeData = ResumeData(
                    tempFilePath = tempFilePath,
                    downloadedBytes = lastKnownDownloadedBytes,
                    totalBytes = totalBytes,
                    etag = etag,
                    lastModified = lastModified,
                    savedAt = System.currentTimeMillis()
                )
                val updatedTask = downloadTasks[taskId]?.copy(resumeData = resumeData) ?: throw e
                updateTask(updatedTask)
            }
            throw e
        }
    }

    private fun updateTask(task: DownloadTask) {
        downloadTasks[task.id] = task
        taskStateFlows[task.id]?.value = task
        allTasksFlow.value = downloadTasks.values.toList()
    }

    /**
     * 移除任务，释放 StateFlow 资源
     * 先更新 allTasksFlow（移除前从列表中删除），再清理 per-task 的 StateFlow
     */
    private fun removeTask(taskId: String) {
        downloadTasks.remove(taskId)
        taskStateFlows.remove(taskId)
        allTasksFlow.value = downloadTasks.values.toList()
    }

    private fun findTaskByUrl(url: String): DownloadTask? {
        return downloadTasks.values.find { it.url == url }
    }

    private fun generateTaskId(): String {
        return UUID.randomUUID().toString()
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val path = java.net.URL(url).path
            val fileName = path.substringAfterLast('/')
            fileName.ifEmpty { "download_${System.currentTimeMillis()}" }
        } catch (_: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    private fun createTempFilePath(destinationPath: String, fileName: String): String {
        val dir = File(destinationPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "${fileName}.tmp").absolutePath
    }

    // endregion
}