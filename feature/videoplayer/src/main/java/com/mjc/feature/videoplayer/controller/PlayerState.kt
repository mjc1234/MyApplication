package com.mjc.feature.videoplayer.controller

/**
 * 播放器状态密封类
 */
sealed class PlayerState {
    /** 播放器正在初始化 */
    object Initializing : PlayerState()

    /** 播放器就绪，可以播放 */
    object Ready : PlayerState()

    /** 正在播放 */
    object Playing : PlayerState()

    /** 已暂停 */
    object Paused : PlayerState()

    /** 正在缓冲，[bufferedPercentage] 为缓冲进度 (0-100) */
    data class Buffering(val bufferedPercentage: Int) : PlayerState()

    /** 播放错误，[message] 为错误描述，[isRecoverable] 表示是否可重试 */
    data class Error(
        val message: String,
        val isRecoverable: Boolean = true
    ) : PlayerState()

    /** 播放结束 */
    object Ended : PlayerState()

    /** 播放器已释放资源 */
    object Released : PlayerState()
}
