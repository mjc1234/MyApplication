package com.mjc.feature.videoplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mjc.core.ui.MIconButton
import com.mjc.feature.videoplayer.controller.PlayerState

/**
 * 视频播放器控制器 - 基于Figma设计实现
 * 包含顶部额外选项、中央播放控制、底部进度条和信息
 */
@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerState: PlayerState,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    isFullscreen: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    title: String = "",
    subtitle: String = "",
    onSkipForward: () -> Unit = {},
    onSkipBackward: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val isMuted = volume <= 0.01f
    val isPlaying = playerState is PlayerState.Playing

    val progress: Float = when {
        isDragging   -> sliderPosition
        duration > 0 -> currentPosition.toFloat() / duration.toFloat()
        else         -> 0f
    }

    val timeDisplay = remember(currentPosition, duration) {
        "${formatTime(currentPosition)} / ${formatTime(duration)}"
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── 1. 顶部右侧：额外选项（3个小按钮 48dp） ──
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "设置",
                size = 48.dp,
                iconSize = 24.dp,
                onClick = onSettingsClick,
            )
            MIconButton(
                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "取消静音" else "静音",
                size = 48.dp,
                iconSize = 24.dp,
                onClick = { onVolumeChange(if (isMuted) 1.0f else 0.0f) },
            )
            MIconButton(
                icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                size = 48.dp,
                iconSize = 20.dp,
                onClick = onFullscreenToggle,
            )
        }

        // ── 2. 中央：主播放控制（后退 64dp + 播放/暂停 96dp + 快进 64dp） ──
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MIconButton(
                icon = Icons.Default.FastRewind,
                contentDescription = "后退10秒",
                size = 64.dp,
                iconSize = 32.dp,
                onClick = onSkipBackward,
            )
            MIconButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停播放" else "开始播放",
                size = 96.dp,
                iconSize = 58.dp,
                onClick = onPlayPauseToggle,
            )
            MIconButton(
                icon = Icons.Default.FastForward,
                contentDescription = "快进10秒",
                size = 64.dp,
                iconSize = 32.dp,
                onClick = onSkipForward,
            )
        }

        // ── 3. 底部：进度条和信息 ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 信息行：标题/副标题 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                lineHeight = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 0.1.sp,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.1.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // 自定义进度条（薄型，2dp高度）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .semantics {
                        progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(
                            current = progress,
                            range = 0f..1f,
                            steps = 0
                        )
                        contentDescription = "视频进度 $timeDisplay"
                        if (duration <= 0) disabled()
                    }
                    .pointerInput(duration) {
                        if (duration <= 0) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            isDragging = true
                            sliderPosition = (down.position.x / size.width).coerceIn(0f, 1f)

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.all { !it.pressed }) {
                                        onSeek((sliderPosition * duration).toLong())
                                        break
                                    }
                                    event.changes.forEach { change ->
                                        change.consume()
                                        sliderPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                                    }
                                }
                            } finally {
                                isDragging = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimary,
                            RoundedCornerShape(20.dp)
                        )
                )
            }
        }
    }
}

/**
 * 格式化时间（毫秒 -> MM:SS 或 H:MM:SS）
 */
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
