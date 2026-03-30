package com.mjc.feature.camera.utils

import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 设备方向管理器
 * 使用OrientationEventListener实时监听设备方向变化
 * 提供旋转角度（0°, 90°, 180°, 270°）的状态流
 */
class DeviceOrientationManager(context: Context) {
    companion object {
        private const val TAG = "DeviceOrientationManager"
    }

    // 屏幕UI旋转角度
    private val _rotation = MutableStateFlow(getDefaultRotation(context))
    val rotation: StateFlow<DeviceOrientation> = _rotation

    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            val surfaceRotation = DeviceOrientation.fromSensorAngle(orientation)

            if (_rotation.value != surfaceRotation) {
                Log.d(TAG, "设备方向变化: orientation=$orientation, surfaceRotation=$surfaceRotation")
                _rotation.value = surfaceRotation
            }
        }
    }

    /**
     * 开始监听设备方向变化
     */
    fun start() {
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
    }

    /**
     * 获取默认设备旋转角度
     */
    private fun getDefaultRotation(context: Context): DeviceOrientation {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val display = windowManager?.defaultDisplay
            DeviceOrientation.fromSensorAngle(display?.rotation ?: 0)
        } catch (e: Exception) {
            Log.w(TAG, "获取默认旋转失败", e)
            DeviceOrientation.PORTRAIT
        }
    }

    /**
     * 停止监听设备方向变化
     */
    fun stop() {
        orientationListener.disable()
    }
}

/**
 * 表示设备相对于自然方向（Portrait）的物理旋转状态
 */
enum class DeviceOrientation(
    val surfaceRotation: Int,
    val uiCounterRotation: Int, // UI 元素需要旋转的角度（用于 setRotation）
) {
    PORTRAIT(Surface.ROTATION_0, 0),
    LANDSCAPE_LEFT(Surface.ROTATION_90, 90),    // 左侧朝下
    UPSIDE_DOWN(Surface.ROTATION_180, 180),
    LANDSCAPE_RIGHT(Surface.ROTATION_270, 270);  // 右侧朝下

    companion object {
        /**
         * 将 OrientationEventListener 的 0-359 角度映射为枚举
         */
        fun fromSensorAngle(angle: Int): DeviceOrientation {
            return when (angle) {
                in 45..134 -> LANDSCAPE_RIGHT    // 90° 左右
                in 135..224 -> UPSIDE_DOWN  // 180° 左右
                in 225..314 -> LANDSCAPE_LEFT   // 270° 左右
                else -> PORTRAIT                 // 0° 左右
            }
        }
    }
}