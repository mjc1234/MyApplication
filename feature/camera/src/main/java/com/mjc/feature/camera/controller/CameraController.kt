package com.mjc.feature.camera.controller

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.mjc.feature.camera.usercase.CameraPreviewUseCase
import com.mjc.feature.camera.usercase.ImageCaptureUseCase
import com.mjc.feature.camera.utils.DeviceOrientationManager
import com.mjc.AnalysisMode
import com.mjc.AnalysisResultComposite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mjc.feature.camera.usercase.AnalysisUseCase
import com.mjc.feature.camera.utils.DeviceOrientation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 相机控制器，管理CameraX生命周期和相机操作
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "CameraController"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: CameraPreviewUseCase? = null
    private var imageCaptureUseCase: ImageCaptureUseCase? = null
    private var analysisUseCase: AnalysisUseCase? = null
    private var cameraExecutor: ExecutorService? = null

    // --- Flow-based result delivery ---
    val analysisResultFlow: StateFlow<AnalysisResultComposite>?
        get() = analysisUseCase?.compositeStateFlow

    private var orientationManager: DeviceOrientationManager? = null

    // 屏幕方向锁定状态
    private var isScreenLockedToPortrait: Boolean = false

    /**
     * 检查屏幕方向锁定状态
     */
    private fun checkScreenOrientation() {
        val activity = context as? Activity
        isScreenLockedToPortrait = when (activity?.requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT -> true
            else -> false
        }
        Log.d(TAG, "屏幕方向锁定状态: $isScreenLockedToPortrait")
    }

    init {
        // 检查屏幕方向锁定状态
        checkScreenOrientation()
    }

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // 闪光灯支持状态缓存
    private var _flashSupported: Boolean = false
    val flashSupported: Boolean get() = _flashSupported

    private var _cameraState: CameraState = CameraState.Initializing
    var cameraState: CameraState
        get() = _cameraState
        private set(value) {
            _cameraState = value
            // 状态变化通知（可以通过回调或LiveData实现）
        }

    /**
     * 初始化相机控制器
     */
    suspend fun initialize(): Boolean {
        // 设置初始化状态（主线程）
        withContext(Dispatchers.Main) {
            _cameraState = CameraState.Initializing
        }

        return try {
            // 1. 初始化相机提供者（IO线程，阻塞操作）
            cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }

            // 2. 初始化用例（IO线程）
            previewUseCase = withContext(Dispatchers.IO) {
                CameraPreviewUseCase(context).apply {
                    createPreviewUseCase()
                }
            }

            imageCaptureUseCase = withContext(Dispatchers.IO) {
                ImageCaptureUseCase(context).apply {
                    createImageCaptureUseCase()
                }
            }

            analysisUseCase = withContext(Dispatchers.IO) {
                AnalysisUseCase(context).apply {
                    createAnalysisUseCase(
                        initialModes = setOf(AnalysisMode.IMAGE_LABELING, AnalysisMode.TEXT_RECOGNITION),
                        scope = coroutineScope
                    )
                }
            }

            // 3. 创建相机执行器（IO线程）
            cameraExecutor = withContext(Dispatchers.IO) {
                Executors.newSingleThreadExecutor()
            }

            // 4. 绑定相机到生命周期（主线程）
            withContext(Dispatchers.Main) {
                bindCameraToLifecycle()
                _cameraState = CameraState.Ready
                // 获取闪光灯支持状态
                _flashSupported = hasFlash()
                // 启动方向监听
                startOrientationListening()
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "相机初始化失败", e)
            // 设置错误状态（主线程）
            withContext(Dispatchers.Main) {
                _cameraState = CameraState.Error(e.message ?: "未知错误")
            }
            false
        }
    }

    /**
     * 启动设备方向监听
     */
    private fun startOrientationListening() {
        Log.d(TAG, "启动设备方向监听")
        orientationManager = DeviceOrientationManager(context)

        coroutineScope.launch {
            orientationManager?.rotation?.collect { rotation ->
                // 更新相机旋转（会考虑屏幕锁定状态）
                updateCameraRotation(rotation)
            }
        }

        orientationManager?.start()
        // 立即应用当前旋转
        orientationManager?.rotation?.value?.let { currentRotation ->
            updateCameraRotation(currentRotation)
        }
        Log.d(TAG, "设备方向监听已启动")
    }

    /**
     * 更新相机旋转设置
     */
    private fun updateCameraRotation(orientation: DeviceOrientation) {
        Log.d(TAG, "更新相机旋转: ${orientation.name}")

        coroutineScope.launch(Dispatchers.Main) {
            // targetRotation = 90(相机传感器的旋转角度) - rotation
//            previewUseCase?.preview?.targetRotation = rotation
            val rotation = orientation.surfaceRotation
            imageCaptureUseCase?.imageCapture?.targetRotation = rotation
            analysisUseCase?.imageAnalysis?.targetRotation = rotation
            Log.d("Debug", "set target rotation : $rotation")
        }
    }

    /**
     * 绑定相机到生命周期
     */
    private suspend fun bindCameraToLifecycle() = withContext(Dispatchers.Main) {
        val provider = cameraProvider ?: return@withContext
        val preview = previewUseCase?.preview ?: return@withContext
        val imageCapture = imageCaptureUseCase?.imageCapture ?: return@withContext

        try {
            // 先解绑所有用例
            provider.unbindAll()

            // 一次性绑定所有用例，避免多次 bindToLifecycle 导致会话重建
            val useCases = mutableListOf<androidx.camera.core.UseCase>()
            useCases.add(preview)
            useCases.add(imageCapture)
            analysisUseCase?.imageAnalysis?.let { useCases.add(it) }

            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            Log.d(TAG, "相机绑定成功")
        } catch (e: Exception) {
            Log.e(TAG, "相机绑定失败", e)
            _cameraState = CameraState.Error("相机绑定失败: ${e.message}")
        }
    }

    /**
     * 设置预览表面提供者（用于传统PreviewView）
     */
    fun setPreviewSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        previewUseCase?.setSurfaceProvider(surfaceProvider)
    }

    /**
     * 设置预览视图（兼容模式）
     */
    fun setPreviewView(previewView: PreviewView) {
        previewUseCase?.setSurfaceProvider(previewView.surfaceProvider)
    }

    /**
     * 捕获图像
     * @return 捕获的图像URI，失败则返回null
     */
    suspend fun captureImage(): Uri? {
        // 设置捕获状态（主线程）
        withContext(Dispatchers.Main) {
            _cameraState = CameraState.Capturing
        }

        return try {
            // 执行图像捕获（IO线程）
            val result = withContext(Dispatchers.IO) {
                imageCaptureUseCase?.captureImage()
            }

            // 更新状态（主线程）
            withContext(Dispatchers.Main) {
                _cameraState = if (result != null) {
                    CameraState.Ready
                } else {
                    CameraState.Error("拍照失败")
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "拍照失败", e)
            // 更新错误状态（主线程）
            withContext(Dispatchers.Main) {
                _cameraState = CameraState.Error("拍照失败: ${e.message}")
            }
            null
        }
    }

    /**
     * 切换前后摄像头
     */
    suspend fun switchCamera() = withContext(Dispatchers.Main) {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // 重新绑定相机（已在主线程）
        bindCameraToLifecycle()
        // 更新闪光灯支持状态
        _flashSupported = hasFlash()
    }

    /**
     * 获取当前相机方向
     */
    fun getCameraDirection(): CameraDirection {
        return when (cameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraDirection.BACK
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraDirection.FRONT
            else -> CameraDirection.BACK
        }
    }

    /**
     * 添加分析处理器
     */
    fun addAnalysisProcessor(mode: AnalysisMode) {
        analysisUseCase?.addProcessor(mode)
    }

    /**
     * 移除分析处理器
     */
    fun removeAnalysisProcessor(mode: AnalysisMode) {
        analysisUseCase?.removeProcessor(mode)
    }

    /**
     * 检查是否支持闪光灯
     */
    suspend fun hasFlash(): Boolean = withContext(Dispatchers.Main) {
        val provider = cameraProvider ?: return@withContext false
        val cameraInfo = provider.availableCameraInfos.find { info ->
            info.cameraSelector == cameraSelector
        }

        return@withContext cameraInfo?.hasFlashUnit() ?: false
    }

    /**
     * 释放相机资源
     */
    fun release() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                // 解绑所有用例（在主线程）
                cameraProvider?.unbindAll()

                // 清理用例（可以在主线程，清理是轻量操作）
                previewUseCase?.clear()
                imageCaptureUseCase?.clear()
                analysisUseCase?.clear()

                // 关闭执行器（IO线程操作）
                withContext(Dispatchers.IO) {
                    cameraExecutor?.shutdown()
                }
                cameraExecutor = null

                // 清理引用
                cameraProvider = null
                previewUseCase = null
                imageCaptureUseCase = null
                analysisUseCase = null
                // 停止方向监听
                orientationManager?.stop()
                orientationManager = null
                Log.d(TAG, "设备方向监听已停止")

                _cameraState = CameraState.Released
                Log.d(TAG, "相机资源已释放")
            } catch (e: Exception) {
                Log.e(TAG, "释放相机资源失败", e)
            }
        }
    }

    /**
     * 相机状态密封类
     */
    sealed class CameraState {
        object Initializing : CameraState()
        object Ready : CameraState()
        object Capturing : CameraState()
        data class Error(val message: String) : CameraState()
        object Released : CameraState()
    }

    /**
     * 相机方向枚举
     */
    enum class CameraDirection {
        FRONT, BACK
    }
}