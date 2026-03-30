package com.mjc.feature.camera

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjc.feature.camera.controller.CameraController
import com.mjc.feature.camera.controller.PermissionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 相机ViewModel，管理相机状态和业务逻辑
 */
class CameraViewModel(
    private val permissionController: PermissionController,
    private val cameraController: CameraController
) : ViewModel() {

    /**
     * 相机状态密封类
     */
    sealed class CameraState {
        object Initializing : CameraState()
        object PermissionRequired : CameraState()
        object PermissionRequesting : CameraState()
        object CameraReady : CameraState()
        data class Capturing(val progress: Float = 0f) : CameraState()
        data class Success(val imageUri: String) : CameraState()
        data class Error(val message: String, val retryAction: () -> Unit = {}) : CameraState()
    }

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Initializing)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionController.PermissionState>(
        PermissionController.PermissionState.Initial
    )
    val permissionState: StateFlow<PermissionController.PermissionState> = _permissionState.asStateFlow()

    // 闪光灯支持状态流
    private val _flashSupported = MutableStateFlow(false)
    val flashSupported: StateFlow<Boolean> = _flashSupported.asStateFlow()

    init {
        viewModelScope.launch {
            initializeCamera()
        }

        // 同步PermissionController的状态到ViewModel
        viewModelScope.launch {
            permissionController.permissionState.collect { state ->
                updatePermissionState(state)
            }
        }
    }

    /**
     * 初始化相机
     */
    private suspend fun initializeCamera() {
        _cameraState.value = CameraState.Initializing

        // 检查权限
        val hasPermissions = withContext(Dispatchers.IO) {
            permissionController.checkPermissions()
        }

        if (!hasPermissions) {
            _cameraState.value = CameraState.PermissionRequired
            return
        }

        // 初始化相机控制器
        val initialized = withContext(Dispatchers.IO) {
            cameraController.initialize()
        }

        if (initialized) {
            _cameraState.value = CameraState.CameraReady
            // 更新闪光灯支持状态
            _flashSupported.value = cameraController.flashSupported
        } else {
            _cameraState.value = CameraState.Error("相机初始化失败") {
                viewModelScope.launch { initializeCamera() }
            }
            // 初始化失败时，重置闪光灯状态为false
            _flashSupported.value = false
        }
    }

    /**
     * 请求权限
     */
    fun requestPermissions(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        permissionsToRequest: Array<String>
    ) {
        _cameraState.value = CameraState.PermissionRequesting
        permissionController.requestPermissions(permissionLauncher, permissionsToRequest)
    }

    /**
     * 获取需要请求的权限列表
     */
    fun getPermissionsToRequest(): Array<String> {
        return permissionController.getPermissionsToRequest()
    }

    /**
     * 处理权限授予
     */
    fun onPermissionsGranted() {
        viewModelScope.launch {
            initializeCamera()
        }
    }

    /**
     * 处理权限拒绝
     */
    fun onPermissionsDenied() {
        _cameraState.value = CameraState.PermissionRequired
    }

    /**
     * 处理权限永久拒绝
     */
    fun onPermissionsPermanentlyDenied() {
        _cameraState.value = CameraState.Error(
            "需要相机和录音权限才能使用相机功能",
            retryAction = { _cameraState.value = CameraState.PermissionRequired }
        )
    }

    /**
     * 捕获图像
     */
    fun captureImage() {
        viewModelScope.launch {
            _cameraState.value = CameraState.Capturing(0f)

            val uri = withContext(Dispatchers.IO) {
                cameraController.captureImage()
            }

            if (uri != null) {
                _cameraState.value = CameraState.Success(uri.toString())
            } else {
                _cameraState.value = CameraState.Error("拍照失败") {
                    captureImage()
                }
            }
        }
    }

    /**
     * 继续使用相机（拍照成功后）
     */
    fun continueToCamera() {
        _cameraState.value = CameraState.CameraReady
    }

    /**
     * 重试初始化
     */
    fun retryInitialization() {
        viewModelScope.launch {
            initializeCamera()
        }
    }

    /**
     * 切换相机
     */
    fun switchCamera() {
        viewModelScope.launch {
            // 注意：这里简化实现，实际应该重新初始化相机
            cameraController.switchCamera()
            // 更新闪光灯支持状态
            _flashSupported.value = cameraController.flashSupported
            _cameraState.update {
                if (it is CameraState.CameraReady || it is CameraState.Error) {
                    CameraState.CameraReady
                } else {
                    it
                }
            }
        }
    }

    /**
     * 检查是否支持闪光灯
     * 从状态流中获取值，避免阻塞主线程
     */
    fun hasFlash(): Boolean {
        return _flashSupported.value
    }

    /**
     * 释放资源
     */
    override fun onCleared() {
        super.onCleared()
        cameraController.release()
    }

    /**
     * 获取相机控制器（用于UI预览）
     */
    fun getCameraController(): CameraController {
        return cameraController
    }

    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        permissionController.handlePermissionResult(permissions)
    }

    /**
     * 更新权限状态（从PermissionController同步）
     */
    fun updatePermissionState(state: PermissionController.PermissionState) {
        _permissionState.value = state

        when (state) {
            is PermissionController.PermissionState.Granted -> onPermissionsGranted()
            is PermissionController.PermissionState.Denied -> onPermissionsDenied()
            is PermissionController.PermissionState.PermanentlyDenied -> onPermissionsPermanentlyDenied()
            else -> {} // 其他状态不需要特殊处理
        }
    }

    companion object {
        /**
         * 创建ViewModel工厂函数（用于Compose中注入依赖）
         */
        fun createFactory(
            permissionController: PermissionController,
            cameraController: CameraController
        ): () -> CameraViewModel = {
            CameraViewModel(permissionController, cameraController)
        }
    }
}

/**
 * CameraViewModel的Factory类，实现ViewModelProvider.Factory接口
 */
class CameraViewModelFactory(
    private val permissionController: PermissionController,
    private val cameraController: CameraController
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(permissionController, cameraController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}