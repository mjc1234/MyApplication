package com.mjc.feature.download.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mjc.feature.download.DownloadState

/**
 * 下载控制组件
 * 包含URL输入框和下载控制按钮
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DownloadControls(
    downloadState: DownloadState,
    onStartDownload: (url: String) -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var url by rememberSaveable { mutableStateOf("http://192.168.31.161:8000/185726-876210695.mp4") }
    var urlError by rememberSaveable { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL输入框
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    urlError = null
                },
                label = { Text("文件URL") },
                placeholder = { Text("http://192.168.31.161:8000/185726-876210695.mp4") },
                isError = urlError != null,
                supportingText = {
                    if (urlError != null) {
                        Text(text = urlError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("支持HTTP/HTTPS协议，支持断点续传")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        validateAndStartDownload(url, downloadState, onStartDownload) { error ->
                            urlError = error
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 控制按钮
            ControlButtons(
                downloadState = downloadState,
                url = url,
                onStartDownload = {
                    validateAndStartDownload(url, downloadState, onStartDownload) { error ->
                        urlError = error
                    }
                },
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onCancelDownload = onCancelDownload,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 控制按钮组
 */
@Composable
private fun ControlButtons(
    downloadState: DownloadState,
    url: String,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (downloadState) {
            is DownloadState.Idle -> {
                // 空闲状态：显示开始下载按钮
                ActionButton(
                    text = "开始下载",
                    icon = Icons.Download,
                    enabled = url.isNotBlank(),
                    onClick = onStartDownload,
                    modifier = Modifier.weight(1f)
                )
            }

            is DownloadState.Preparing -> {
                // 准备状态：显示取消按钮
                ActionButton(
                    text = "取消",
                    icon = Icons.Cancel,
                    onClick = onCancelDownload,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            is DownloadState.Downloading -> {
                // 下载中状态：显示暂停和取消按钮
                ActionButton(
                    text = "暂停",
                    icon = Icons.Pause,
                    onClick = onPauseDownload,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "取消",
                    icon = Icons.Cancel,
                    onClick = onCancelDownload,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            }

            is DownloadState.Paused -> {
                // 暂停状态：显示继续和取消按钮
                ActionButton(
                    text = "继续",
                    icon = Icons.Play,
                    onClick = onResumeDownload,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "取消",
                    icon = Icons.Cancel,
                    onClick = onCancelDownload,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            }

            is DownloadState.Completed -> {
                // 完成状态：显示新的下载按钮
                ActionButton(
                    text = "新的下载",
                    icon = Icons.Download,
                    enabled = url.isNotBlank(),
                    onClick = onStartDownload,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "清空",
                    icon = Icons.Clear,
                    onClick = {
                        // 清空URL
                        // 这里需要外部处理，暂时只调用取消
                        onCancelDownload()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            is DownloadState.Failed -> {
                // 失败状态：显示重试和取消按钮
                ActionButton(
                    text = "重试",
                    icon = Icons.Retry,
                    onClick = onStartDownload,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "取消",
                    icon = Icons.Cancel,
                    onClick = onCancelDownload,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

/**
 * 带图标的操作按钮
 */
@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(text)
    }
}

/**
 * 验证URL并开始下载
 */
private fun validateAndStartDownload(
    url: String,
    downloadState: DownloadState,
    onStartDownload: (String) -> Unit,
    onError: (String) -> Unit
) {
    if (url.isBlank()) {
        onError("请输入URL")
        return
    }

    if (!isValidUrl(url)) {
        onError("URL格式不正确")
        return
    }

    // 检查当前状态是否允许开始新的下载
    if (downloadState is DownloadState.Downloading || downloadState is DownloadState.Preparing) {
        onError("当前有任务正在下载，请先暂停或取消")
        return
    }

    onStartDownload(url)
}

/**
 * 简单的URL验证
 */
private fun isValidUrl(url: String): Boolean {
    return try {
        java.net.URL(url)
        url.startsWith("http://") || url.startsWith("https://")
    } catch (e: Exception) {
        false
    }
}

/**
 * 图标资源（使用Material Icons）
 * 注意：实际项目中应该使用实际的图标资源
 */
object Icons {
    val Download = androidx.compose.material.icons.Icons.Filled.Download
    val Pause = androidx.compose.material.icons.Icons.Filled.Pause
    val Play = androidx.compose.material.icons.Icons.Filled.PlayArrow
    val Cancel = androidx.compose.material.icons.Icons.Filled.Cancel
    val Retry = androidx.compose.material.icons.Icons.Filled.Refresh
    val Clear = androidx.compose.material.icons.Icons.Filled.Clear
}