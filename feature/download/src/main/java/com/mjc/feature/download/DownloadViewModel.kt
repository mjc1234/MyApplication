package com.mjc.feature.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mjc.feature.download.controller.DownloadController
import com.mjc.feature.download.controller.DownloadTaskInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 下载模块的ViewModel
 * 管理下载状态和业务逻辑
 */
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadController: DownloadController
) : ViewModel() {

    // 下载状态
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    // 下载任务列表
    private val _downloadTasks = MutableStateFlow<List<DownloadTaskInfo>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTaskInfo>> = _downloadTasks

    // 当前下载进度
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    // 当前活跃下载任务ID
    private val _currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskId: StateFlow<String?> = _currentTaskId

    init {
        // 收集所有下载任务
        viewModelScope.launch {
            downloadController.getAllDownloads().collectLatest { downloadTaskInfos ->
                _downloadTasks.value = downloadTaskInfos
            }
        }
    }

    /**
     * 开始下载文件
     * @param url 文件URL
     * @param destination 保存路径
     */
    fun startDownload(url: String, destination: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Preparing
            try {
                val taskId = downloadController.startDownload(url, destination)
                _currentTaskId.value = taskId

                // 收集下载进度
                downloadController.getDownloadProgress(taskId).collect { progress ->
                    _downloadProgress.value = progress
                    _downloadState.value = DownloadState.Downloading(progress = progress)
                }
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Failed("开始下载失败: ${e.message}")
            }
        }
    }

    /**
     * 暂停当前下载
     */
    fun pauseDownload() {
        viewModelScope.launch {
            val taskId = _currentTaskId.value
            if (taskId != null) {
                try {
                    downloadController.pauseDownload(taskId)
                    _downloadState.value = DownloadState.Paused
                } catch (e: Exception) {
                    _downloadState.value = DownloadState.Failed("暂停下载失败: ${e.message}")
                }
            } else {
                _downloadState.value = DownloadState.Failed("没有活跃的下载任务")
            }
        }
    }

    /**
     * 继续下载
     */
    fun resumeDownload() {
        viewModelScope.launch {
            val taskId = _currentTaskId.value
            if (taskId != null) {
                try {
                    downloadController.resumeDownload(taskId)
                    _downloadState.value = DownloadState.Downloading(progress = _downloadProgress.value)
                } catch (e: Exception) {
                    _downloadState.value = DownloadState.Failed("继续下载失败: ${e.message}")
                }
            } else {
                _downloadState.value = DownloadState.Failed("没有可恢复的下载任务")
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        viewModelScope.launch {
            val taskId = _currentTaskId.value
            if (taskId != null) {
                try {
                    downloadController.cancelDownload(taskId)
                    _downloadState.value = DownloadState.Idle
                    _downloadProgress.value = 0f
                    _currentTaskId.value = null
                } catch (e: Exception) {
                    _downloadState.value = DownloadState.Failed("取消下载失败: ${e.message}")
                }
            } else {
                _downloadState.value = DownloadState.Idle
                _downloadProgress.value = 0f
            }
        }
    }

}

/**
 * 下载状态
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Preparing : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val filePath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
    data object Paused : DownloadState()
}
