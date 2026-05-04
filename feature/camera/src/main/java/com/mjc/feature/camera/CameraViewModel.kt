package com.mjc.feature.camera

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mjc.AnalysisMode
import com.mjc.feature.camera.controller.CameraController
import com.mjc.feature.camera.controller.PermissionController
import com.mjc.feature.camera.model.AnalysisUiComposite
import com.mjc.feature.camera.model.DetectedImageLabelUiModel
import com.mjc.feature.camera.model.DetectedTextUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 相机ViewModel，管理相机状态和业务逻辑
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val stateHandle: SavedStateHandle,
    private val permissionController: PermissionController,
    private val cameraControllerFactory: CameraController.Factory
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

    private lateinit var cameraController: CameraController

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Initializing)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _permissionState = MutableStateFlow<PermissionController.PermissionState>(
        PermissionController.PermissionState.Initial
    )
    val permissionState: StateFlow<PermissionController.PermissionState> = _permissionState.asStateFlow()

    // 闪光灯支持状态流
    private val _flashSupported = MutableStateFlow(false)
    val flashSupported: StateFlow<Boolean> = _flashSupported.asStateFlow()

    // ML Kit 多处理器组合识别结果
    private val _analysisUiState = MutableStateFlow(AnalysisUiComposite())
    val analysisUiState: StateFlow<AnalysisUiComposite> = _analysisUiState.asStateFlow()

    // 当前激活的分析模式集合
    private val _activeAnalysisModes = MutableStateFlow<Set<AnalysisMode>>(setOf(AnalysisMode.IMAGE_LABELING))
    val activeAnalysisModes: StateFlow<Set<AnalysisMode>> = _activeAnalysisModes.asStateFlow()

    /**
     * 绑定LifecycleOwner并初始化相机
     * 在Composable中调用，提供Activity的LifecycleOwner
     */
    fun attachLifecycle(lifecycleOwner: LifecycleOwner) {
        if (::cameraController.isInitialized) return

        cameraController = cameraControllerFactory.create(lifecycleOwner, viewModelScope)

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
            // 启动识别结果收集
            startAnalysisCollection()
        } else {
            _cameraState.value = CameraState.Error("相机初始化失败") {
                viewModelScope.launch { initializeCamera() }
            }
            // 初始化失败时，重置闪光灯状态为false
            _flashSupported.value = false
        }
    }

    private fun startAnalysisCollection() {
        viewModelScope.launch {
            cameraController.analysisResultFlow
                ?.collect { composite ->
                    _analysisUiState.value = AnalysisUiComposite(
                        labels = composite.imageLabels
                            ?.filter { it.confidence >= 0.5f }
                            ?.map { DetectedImageLabelUiModel(text = it.text, confidence = it.confidence) },
                        text = composite.text?.let {
                            DetectedTextUiModel(text = it.text, textBlocks = it.textBlocks.map { block -> block.text })
                        }
                    )
                }
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
     * 添加分析处理器
     */
    fun addAnalysisProcessor(mode: AnalysisMode) {
        cameraController.addAnalysisProcessor(mode)
        _activeAnalysisModes.update { it + mode }
    }

    /**
     * 移除分析处理器
     */
    fun removeAnalysisProcessor(mode: AnalysisMode) {
        cameraController.removeAnalysisProcessor(mode)
        _activeAnalysisModes.update { it - mode }
        _analysisUiState.update { current ->
            when (mode) {
                AnalysisMode.IMAGE_LABELING -> current.copy(labels = null)
                else -> current
            }
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
        if (::cameraController.isInitialized) {
            cameraController.release()
        }
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
        val MY_REPOSITORY_KEY = CreationExtras.Key<Int>()
    }
}
