package com.mjc.feature.camera.usercase

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import com.mjc.feature.camera.utils.MediaStoreHelper

/**
 * 图像捕获用例
 */
class ImageCaptureUseCase(private val context: Context) {

    private var _imageCapture: ImageCapture? = null
    val imageCapture: ImageCapture? get() = _imageCapture

    private lateinit var executor: Executor

    /**
     * 创建并配置图像捕获用例
     */
    fun createImageCaptureUseCase(): ImageCapture {
        executor = ContextCompat.getMainExecutor(context)

        // 创建分辨率选择器
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(4032, 3024),  // 12MP 4:3
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        // 构建图像捕获用例
        _imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(85)  // 85% JPEG质量
            .build()

        return _imageCapture!!
    }

    /**
     * 捕获图像并保存到MediaStore PICTURES目录
     * @return 保存的图像URI（内容URI），如果失败则返回null
     */
    suspend fun captureImage(): Uri? = withContext(Dispatchers.IO) {
        val imageCapture = _imageCapture ?: return@withContext null

        // 生成文件名
        val fileName = MediaStoreHelper.generateImageFileName()
        val contentValues = MediaStoreHelper.createImageContentValues(fileName)

        // 创建CameraX输出选项
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStoreHelper.getImagesContentUri(),
            contentValues
        ).build()

        // 使用CompletableDeferred等待异步结果
        val deferred = CompletableDeferred<Uri?>()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    deferred.complete(output.savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    deferred.complete(null)
                }
            }
        )

        return@withContext deferred.await()
    }


    /**
     * 获取可用的分辨率列表
     */
    fun getAvailableResolutions(): List<Size> {
        return listOf(
            Size(4032, 3024),  // 12MP 4:3
            Size(3264, 2448),  // 8MP 4:3
            Size(1920, 1080),  // Full HD 16:9
            Size(1280, 720),   // HD 16:9
        )
    }

    /**
     * 设置JPEG质量 (1-100)
     */
    fun setJpegQuality(quality: Int) {
//        _imageCapture?.jpegQuality = quality.coerceIn(1, 100)
    }

    /**
     * 设置闪光灯模式
     */
    fun setFlashMode(flashMode: Int) {
        _imageCapture?.flashMode = flashMode
    }

    /**
     * 清理图像捕获用例
     */
    fun clear() {
        _imageCapture = null
    }
}