package com.mjc.feature.videoplayer.controller

import android.content.Context
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.mjc.core.mediaeffect.SimpleTextOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 视频播放器控制器，管理ExoPlayer生命周期和播放操作
 */
@UnstableApi
class VideoPlayerController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "VideoPlayerController"
        // 缓冲区配置：最小15秒，最大30秒
        private const val MIN_BUFFER_MS: Int = 15000
        private const val MAX_BUFFER_MS = 30000
        private const val BUFFER_FOR_PLAYBACK_MS = 2500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var isReleased = false
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Initializing)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_RESUME -> play()
            Lifecycle.Event.ON_DESTROY -> release()
            else -> Unit
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * 初始化播放器
     * ExoPlayer必须在主线程创建，否则可能导致并发问题
     */
    suspend fun initialize(videoUri: Uri? = null): Boolean {
        return try {
            _playerState.value = PlayerState.Initializing

            // 在主线程创建播放器（Media3要求）
            withContext(Dispatchers.Main) {
                // 创建轨道选择器
                val trackSelector = DefaultTrackSelector(context).apply {
                    setParameters(
                        buildUponParameters()
                            .setMaxVideoSizeSd()
                            .setPreferredAudioLanguage(java.util.Locale.getDefault().language)
                    )
                }

                // 创建播放器
                player = ExoPlayer.Builder(context)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(
                        DefaultLoadControl.Builder()
                            .setBufferDurationsMs(
                                MIN_BUFFER_MS,
                                MAX_BUFFER_MS,
                                BUFFER_FOR_PLAYBACK_MS,
                                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                            )
                            .build()
                    )
                    .build()
                    .apply {
                        setVideoEffects(
                            listOf(
                                OverlayEffect(
                                    listOf(
                                        SimpleTextOverlay(
                                            textProvider = {
                                                SpannableString("正在播放: ${videoUri?.lastPathSegment ?: "未知视频"}")
                                            },
                                            textColor = android.graphics.Color.WHITE,
                                            textSizePx = 48,
                                        )
                                    )
                                )
                            )
                        )
                        // 设置监听器
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                when (playbackState) {
                                    Player.STATE_IDLE -> _playerState.value = PlayerState.Initializing
                                    Player.STATE_BUFFERING -> _playerState.value =
                                        PlayerState.Buffering(bufferedPercentage)
                                    Player.STATE_READY -> {
                                        if (isPlaying) {
                                            _playerState.value = PlayerState.Playing
                                        } else {
                                            _playerState.value = PlayerState.Paused
                                        }
                                    }
                                    Player.STATE_ENDED -> _playerState.value = PlayerState.Ended
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                _playerState.value = PlayerState.Error(
                                    message = error.message ?: "播放错误",
                                    isRecoverable = true
                                )
                            }
                        })
                    }

                // 创建MediaSession（用于后台播放和控制集成）
                mediaSession = MediaSession.Builder(context, player!!).build()
            }

            // 如果有初始视频URI，则加载视频
            videoUri?.let { uri ->
                playVideo(uri)
            }

            true
        } catch (e: Exception) {
            _playerState.value = PlayerState.Error(
                message = "播放器初始化失败: ${e.message}",
                isRecoverable = true
            )
            false
        }
    }

    /**
     * 播放指定URI的视频
     */
    fun playVideo(uri: Uri) {
        player?.let { player ->
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    /**
     * 播放/暂停切换
     */
    fun togglePlayPause() {
        player?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    /**
     * 播放
     */
    fun play() {
        player?.play()
    }

    /**
     * 暂停
     */
    fun pause() {
        player?.pause()
    }

    /**
     * 跳转到指定位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    /**
     * 获取当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    /**
     * 获取视频总时长（毫秒）
     */
    fun getDuration(): Long = player?.duration ?: 0

    /**
     * 设置音量（0.0 - 1.0）
     */
    fun setVolume(volume: Float) {
        player?.volume = volume
    }

    /**
     * 获取当前音量
     */
    fun getVolume(): Float = player?.volume ?: 1.0f

    /**
     * 获取播放器实例（用于UI组件）
     */
    fun getPlayer(): Player? = player

    /**
     * 释放播放器资源
     */
    fun release() {
        if (isReleased) return
        isReleased = true

        // 移除生命周期观察者，防止释放后收到事件
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)

        player?.stop()
        player?.release()
        mediaSession?.release()
        player = null
        mediaSession = null
        _playerState.value = PlayerState.Released
    }
}