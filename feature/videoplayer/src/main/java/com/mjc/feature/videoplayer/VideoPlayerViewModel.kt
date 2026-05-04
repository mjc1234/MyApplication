package com.mjc.feature.videoplayer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mjc.feature.videoplayer.controller.PlayerState
import com.mjc.feature.videoplayer.controller.VideoPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 视频播放器ViewModel，管理播放器状态和业务逻辑
 */
@HiltViewModel
@UnstableApi
class VideoPlayerViewModel @Inject constructor(
    val videoPlayerController: VideoPlayerController
) : ViewModel() {

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 250L
    }

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Initializing)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // 当前播放位置（毫秒）
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    // 视频总时长（毫秒）
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // 音量（0.0 - 1.0）
    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // 是否全屏
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    // 定时更新播放位置的任务
    private var positionUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            initializePlayer()
        }

        // 监听播放器状态变化
        viewModelScope.launch {
            videoPlayerController.playerState.collect { state ->
                _playerState.value = state
                // 更新时长
                _duration.value = videoPlayerController.getDuration()

                // 根据播放状态启动或停止位置更新
                when (state) {
                    is PlayerState.Playing -> startPositionUpdate()
                    else -> stopPositionUpdate()
                }
            }
        }
    }

    /**
     * 启动定时更新播放位置
     */
    private fun startPositionUpdate() {
        stopPositionUpdate()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                _currentPosition.value = videoPlayerController.getCurrentPosition()
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * 停止定时更新播放位置
     */
    private fun stopPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        // 停止时同步一次当前位置
        _currentPosition.value = videoPlayerController.getCurrentPosition()
    }

    /**
     * 初始化播放器（不加载视频）
     */
    private suspend fun initializePlayer() {
        val initialized = withContext(Dispatchers.IO) {
            videoPlayerController.initialize(null)
        }

        if (!initialized) {
            _playerState.value = PlayerState.Error("播放器初始化失败")
        }
    }

    /**
     * 加载并播放视频
     */
    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            videoPlayerController.playVideo(uri)
        }
    }

    /**
     * 播放/暂停切换
     */
    fun togglePlayPause() {
        videoPlayerController.togglePlayPause()
    }

    /**
     * 播放
     */
    fun play() {
        videoPlayerController.play()
    }

    /**
     * 暂停
     */
    fun pause() {
        videoPlayerController.pause()
    }

    /**
     * 跳转到指定位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        videoPlayerController.seekTo(positionMs)
    }

    /**
     * 设置音量
     */
    fun setVolume(volume: Float) {
        videoPlayerController.setVolume(volume)
        _volume.value = volume
    }

    /**
     * 切换全屏模式
     */
    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    /**
     * 重新加载视频（错误恢复）
     */
    fun reloadVideo(uri: Uri) {
        viewModelScope.launch {
            videoPlayerController.playVideo(uri)
        }
    }

    /**
     * 重试初始化播放器（从错误状态恢复）
     */
    fun retry() {
        viewModelScope.launch { initializePlayer() }
    }

    override fun onCleared() {
        super.onCleared()
        videoPlayerController.release()
    }
}
