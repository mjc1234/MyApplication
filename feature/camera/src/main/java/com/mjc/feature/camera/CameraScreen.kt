package com.mjc.feature.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mjc.AnalysisMode
import com.mjc.feature.camera.ui.AnalysisModeSelector
import com.mjc.feature.camera.ui.CameraControls
import com.mjc.feature.camera.ui.CameraPreview
import com.mjc.feature.camera.ui.CaptureSuccessScreen
import com.mjc.feature.camera.ui.ErrorScreen
import com.mjc.feature.camera.ui.LabelOverlay
import com.mjc.feature.camera.ui.PermissionRequestScreen
import com.mjc.feature.camera.ui.TextOverlay

/**
 * 相机主界面
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 绑定LifecycleOwner到ViewModel，触发相机初始化
    LaunchedEffect(lifecycleOwner) {
        viewModel.attachLifecycle(lifecycleOwner)
    }

    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val flashSupported by viewModel.flashSupported.collectAsStateWithLifecycle()
    val analysisUiState by viewModel.analysisUiState.collectAsStateWithLifecycle()
    val activeAnalysisModes by viewModel.activeAnalysisModes.collectAsStateWithLifecycle()

    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 处理权限请求结果
        viewModel.handlePermissionResult(permissions)
    }

    // 权限状态变化已由ViewModel通过PermissionController.permissionState.collect处理

    // 请求权限的函数
    val requestPermissions = remember {
        {
            val permissionsToRequest = viewModel.getPermissionsToRequest()
            if (permissionsToRequest.isNotEmpty()) {
                // 通过ViewModel发起权限请求，传递启动器和权限列表
                viewModel.requestPermissions(permissionLauncher, permissionsToRequest)
            } else {
                // 所有权限都已授予
                viewModel.onPermissionsGranted()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = cameraState) {
            is CameraViewModel.CameraState.Initializing -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is CameraViewModel.CameraState.PermissionRequired -> {
                PermissionRequestScreen(
                    onRequestPermissions = requestPermissions,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is CameraViewModel.CameraState.PermissionRequesting -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is CameraViewModel.CameraState.CameraReady -> {
                // 相机预览
                CameraPreview(
                    controller = viewModel.getCameraController(),
                    modifier = Modifier.fillMaxSize()
                )

                // 分析模式选择器 + 标签覆盖层（垂直排列）
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    AnalysisModeSelector(
                        allModes = listOf(AnalysisMode.IMAGE_LABELING),
                        activeModes = activeAnalysisModes,
                        onModeToggle = { mode, enable ->
                            if (enable) viewModel.addAnalysisProcessor(mode)
                            else viewModel.removeAnalysisProcessor(mode)
                        }
                    )

                    analysisUiState.labels?.let { labels ->
                        if (labels.isNotEmpty()) {
                            LabelOverlay(labels = labels)
                        }
                    }

                    analysisUiState.text?.let { (_, textBlocks) ->
                        TextOverlay(textBlocks = textBlocks)
                    }
                }

                // 控制按钮
                CameraControls(
                    onCapture = viewModel::captureImage,
                    onToggleFlash = null, // 阶段1不实现闪光灯
                    onSwitchCamera = null, // 阶段1不实现切换相机
                    hasFlash = flashSupported,
                    isFlashOn = false,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            is CameraViewModel.CameraState.Capturing -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            is CameraViewModel.CameraState.Success -> {
                CaptureSuccessScreen(
                    imageUri = state.imageUri,
                    onContinue = viewModel::continueToCamera,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is CameraViewModel.CameraState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = state.retryAction,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // 监听生命周期
    val lifecycle = lifecycleOwner.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // 应用进入后台时，可以暂停相机预览以节省资源
                }
                Lifecycle.Event.ON_START -> {
                    // 应用回到前台时，重新初始化相机
                }
                else -> {}
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

/**
 * 相机主题（可自定义）
 */
@Composable
fun CameraTheme(
    content: @Composable () -> Unit
) {
    // 使用应用主主题，这里可以自定义相机特定主题
    MaterialTheme(
        content = content
    )
}