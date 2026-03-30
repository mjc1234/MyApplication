package com.mjc.feature.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mjc.feature.camera.R

/**
 * 相机控制按钮组件
 */
@Composable
fun CameraControls(
    onCapture: () -> Unit,
    onToggleFlash: (() -> Unit)? = null,
    onSwitchCamera: (() -> Unit)? = null,
    hasFlash: Boolean = false,
    isFlashOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：闪光灯按钮（如果有闪光灯）
            if (hasFlash && onToggleFlash != null) {
                FlashButton(
                    isFlashOn = isFlashOn,
                    onToggle = onToggleFlash,
                    enabled = hasFlash
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            // 中间：拍照按钮
            CaptureButton(
                onCapture = onCapture,
                modifier = Modifier
            )

            // 右侧：切换相机按钮
            if (onSwitchCamera != null) {
                SwitchCameraButton(
                    onSwitch = onSwitchCamera
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

/**
 * 拍照按钮
 */
@Composable
fun CaptureButton(
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 外圈
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
        )

        // 内圈（拍照按钮）
        IconButton(
            onClick = onCapture,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "拍照",
                modifier = Modifier.size(32.dp),
                tint = Color.Black
            )
        }
    }
}

/**
 * 闪光灯按钮
 */
@Composable
fun FlashButton(
    isFlashOn: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(48.dp),
        enabled = enabled
    ) {
        Icon(
            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
            contentDescription = if (isFlashOn) "关闭闪光灯" else "打开闪光灯",
            modifier = Modifier.size(32.dp),
            tint = if (enabled) Color.White else Color.Gray
        )
    }
}

/**
 * 切换相机按钮
 */
@Composable
fun SwitchCameraButton(
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onSwitch,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SwitchCamera,
            contentDescription = "切换相机",
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )
    }
}

/**
 * 权限请求屏幕
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要权限",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "此应用需要相机和录音权限才能正常使用拍照功能。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "请授予以下权限：\n• 相机权限 - 用于拍摄照片\n• 录音权限 - 用于视频录制（可选）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 权限请求按钮
        androidx.compose.material3.Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("授予权限")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 返回按钮
        androidx.compose.material3.OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回")
        }
    }
}

/**
 * 拍照成功屏幕
 */
@Composable
fun CaptureSuccessScreen(
    imageUri: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = "拍照成功",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "拍照成功",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "图片已保存到设备",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 继续拍照按钮
        androidx.compose.material3.Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("继续拍照")
        }
    }
}

/**
 * 错误屏幕
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "发生错误",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 重试按钮
        androidx.compose.material3.Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重试")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 返回按钮
        androidx.compose.material3.OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回")
        }
    }
}

@Preview
@Composable
private fun CameraControlsPreview() {
    MaterialTheme {
        CameraControls(
            onCapture = {},
            onToggleFlash = {},
            onSwitchCamera = {},
            hasFlash = true,
            isFlashOn = false
        )
    }
}

@Preview
@Composable
private fun PermissionRequestScreenPreview() {
    MaterialTheme {
        PermissionRequestScreen(
            onRequestPermissions = {}
        )
    }
}