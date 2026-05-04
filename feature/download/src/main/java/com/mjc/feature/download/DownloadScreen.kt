package com.mjc.feature.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mjc.feature.download.ui.DownloadControls
import com.mjc.feature.download.ui.DownloadList
import com.mjc.feature.download.ui.DownloadProgress

/**
 * 下载模块主界面
 * 显示下载列表、进度和控制按钮
 */
@Composable
fun DownloadScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // 收集ViewModel状态
    val downloadState by viewModel.downloadState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadTasks by viewModel.downloadTasks.collectAsState()
    val currentTaskId by viewModel.currentTaskId.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            // 顶部栏
            Text(
                text = "文件下载",
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 下载进度显示区域
            DownloadProgressSection(
                downloadState = downloadState,
                downloadProgress = downloadProgress,
                currentTaskId = currentTaskId,
                downloadTasks = downloadTasks
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 下载任务列表
            DownloadListSection(downloadTasks = downloadTasks)

            Spacer(modifier = Modifier.height(16.dp))

            // 下载控制按钮
            DownloadControlsSection(
                downloadState = downloadState,
                onStartDownload = { url ->
                    // 简化：使用默认保存路径
                    val destination = context.cacheDir.path
                    viewModel.startDownload(url, destination)
                },
                onPauseDownload = viewModel::pauseDownload,
                onResumeDownload = viewModel::resumeDownload,
                onCancelDownload = viewModel::cancelDownload
            )
        }
    }
}

@Composable
private fun DownloadProgressSection(
    downloadState: DownloadState,
    downloadProgress: Float,
    currentTaskId: String?,
    downloadTasks: List<com.mjc.feature.download.controller.DownloadTaskInfo>
) {
    // 查找当前任务详情
    val currentTask = currentTaskId?.let { taskId ->
        downloadTasks.find { it.id == taskId }
    }

    when (downloadState) {
        is DownloadState.Idle -> {
            // 空闲状态：显示提示
            Text(
                text = "准备下载",
                modifier = Modifier.padding(16.dp)
            )
        }
        is DownloadState.Preparing -> {
            // 准备状态
            Text(
                text = "准备下载中...",
                modifier = Modifier.padding(16.dp)
            )
        }
        is DownloadState.Downloading -> {
            // 下载中：显示进度
            if (currentTask != null) {
                DownloadProgress(
                    progress = currentTask.progress,
                    downloadedBytes = currentTask.downloadedBytes,
                    totalBytes = currentTask.totalBytes,
                    speedBytesPerSecond = currentTask.speedBytesPerSecond,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // 没有任务详情，显示简单进度
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                Text(
                    text = "${(downloadProgress * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        is DownloadState.Paused -> {
            // 暂停状态
            Text(
                text = "下载已暂停",
                modifier = Modifier.padding(16.dp)
            )
            if (currentTask != null) {
                Text(
                    text = "进度: ${(currentTask.progress * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        is DownloadState.Completed -> {
            // 完成状态
            Text(
                text = "下载完成!",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "文件保存路径: ${downloadState.filePath}",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        is DownloadState.Failed -> {
            // 失败状态
            Text(
                text = "下载失败: ${downloadState.error}",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DownloadListSection(
    downloadTasks: List<com.mjc.feature.download.controller.DownloadTaskInfo>
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "下载任务列表",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        DownloadList(tasks = downloadTasks)
    }
}

@Composable
private fun DownloadControlsSection(
    downloadState: DownloadState,
    onStartDownload: (String) -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    DownloadControls(
        downloadState = downloadState,
        onStartDownload = onStartDownload,
        onPauseDownload = onPauseDownload,
        onResumeDownload = onResumeDownload,
        onCancelDownload = onCancelDownload,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun DownloadScreenPreview() {
    DownloadScreen()
}

// 扩展属性，用于预览
private val Int.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this.toFloat())