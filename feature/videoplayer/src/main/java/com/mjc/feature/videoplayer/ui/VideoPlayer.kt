package com.mjc.feature.videoplayer.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import com.mjc.feature.videoplayer.controller.VideoPlayerController

/**
 * 视频播放器组件，使用Media3的PlayerSurface（Compose UI组件）。
 * 自动适配不同宽高比的视频，保持视频原始比例居中显示。
 *
 * 注意：当 [controller.getPlayer] 返回 null 时（播放器尚未初始化），
 * 本组件不渲染任何内容。调用方应配合 [PlayerState] 判断是否显示此组件。
 *
 * @param controller 视频播放控制器
 * @param modifier 修饰符
 * @param showBuffering 是否显示缓冲指示器
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    controller: VideoPlayerController,
    modifier: Modifier = Modifier,
    showBuffering: Boolean = true
) {
    val player = controller.getPlayer()

    if (player != null) {
        VideoPlayerContent(
            player = player,
            modifier = modifier,
            showBuffering = showBuffering
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerContent(
    player: Player,
    modifier: Modifier = Modifier,
    showBuffering: Boolean = true
) {
    var videoWidth by remember { mutableIntStateOf(16) }
    var videoHeight by remember { mutableIntStateOf(9) }
    var isBuffering by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)

        // 初始化时获取当前状态
        val currentSize = player.videoSize
        if (currentSize.width > 0 && currentSize.height > 0) {
            videoWidth = currentSize.width
            videoHeight = currentSize.height
        }
        isBuffering = player.playbackState == Player.STATE_BUFFERING

        onDispose {
            player.removeListener(listener)
        }
    }

    val aspectRatio = if (videoHeight > 0) {
        videoWidth.toFloat() / videoHeight.toFloat()
    } else {
        16f / 9f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        PlayerSurface(
            player = player,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
        )

        // 缓冲指示器
        if (showBuffering && isBuffering) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}