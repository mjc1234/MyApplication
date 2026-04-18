package com.mjc.feature.camera.usercase

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter

/**
 * 相机预览用例配置
 */
class CameraPreviewUseCase(private val context: Context) {

    private var _preview: Preview? = null
    val preview: Preview? get() = _preview

    /**
     * 创建并配置预览用例
     */
    fun createPreviewUseCase(): Preview {
        // 创建分辨率选择器
        val resolutionSelector = ResolutionSelector.Builder()
            // 设置首选分辨率策略
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1920, 1080),  // 首选分辨率
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            // 设置宽高比策略（支持16:9, 4:3, 1:1）
            .setAspectRatioStrategy(
                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            )
            // 可选：添加自定义分辨率过滤器
            .setResolutionFilter { resolutions, _ ->
                resolutions.sortedByDescending { it.width * it.height }
            }
            .build()

        // 构建预览用例
        _preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        return _preview!!
    }

    /**
     * 设置预览表面提供者
     */
    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        _preview?.surfaceProvider = surfaceProvider
    }

    /**
     * 清理预览用例
     */
    fun clear() {
        _preview?.surfaceProvider = null
        _preview = null
    }

    /**
     * 获取推荐的预览分辨率
     */
    fun getRecommendedResolution(): Size {
        // 根据设备屏幕大小返回推荐分辨率
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        return when {
            screenWidth >= 1920 && screenHeight >= 1080 -> Size(1920, 1080)
            screenWidth >= 1280 && screenHeight >= 720 -> Size(1280, 720)
            else -> Size(720, 480)
        }
    }
}