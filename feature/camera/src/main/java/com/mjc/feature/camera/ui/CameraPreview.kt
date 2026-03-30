package com.mjc.feature.camera.ui

import android.content.Context
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mjc.feature.camera.controller.CameraController

/**
 * 相机预览组件
 * 支持两种模式：传统PreviewView和CameraXViewfinder（如果可用）
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 传统PreviewView方案（兼容性更好）
    LegacyCameraPreview(
        controller = controller,
        modifier = modifier
    )

    // 注意：CameraXViewfinder是CameraX 1.6的新特性
    // 如果项目中使用了CameraX 1.6+，可以取消注释以下代码
    /*
    CameraXViewfinder(
        controller = controller.cameraController,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center
    )
    */
}

/**
 * 传统PreviewView实现（兼容旧版本CameraX）
 */
@Composable
private fun LegacyCameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // 设置预览视图参数
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                    // 将预览视图设置到控制器
                    controller.setPreviewView(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                // 更新逻辑（如果需要）
            }
        )
    }
}

/**
 * 相机预览叠加层（网格、比例线等）
 */
@Composable
fun CameraOverlay(
    showGrid: Boolean = false,
    aspectRatio: Float = 4f / 3f,
    modifier: Modifier = Modifier
) {
    // 这里可以添加网格、比例线等叠加元素
    // 为了简化阶段1实现，暂时留空
}

/**
 * 相机状态指示器
 */
@Composable
fun CameraStatusIndicator(
    isCapturing: Boolean,
    modifier: Modifier = Modifier
) {
    // 这里可以添加拍照状态指示器
    // 为了简化阶段1实现，暂时留空
}