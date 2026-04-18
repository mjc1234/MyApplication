package com.mjc.feature.camera.usercase

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.mjc.AnalysisMode
import com.mjc.AnalysisResultComposite
import com.mjc.processor.VisionImageProcessor
import com.mjc.processor.VisionProcessorBase
import com.mjc.VisionProcessorFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class AnalysisUseCase(val context: Context) {

    companion object {
        private const val TAG = "AnalysisUseCase"
    }

    private val processorMap = mutableMapOf<AnalysisMode, VisionImageProcessor>()
    private val bridgeJobs = mutableMapOf<AnalysisMode, Job>()
    private var _imageAnalysis: ImageAnalysis? = null
    val imageAnalysis: ImageAnalysis? get() = _imageAnalysis
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var coroutineScope: CoroutineScope? = null

    // 组合结果状态流
    private val _compositeState = MutableStateFlow(AnalysisResultComposite())
    val compositeStateFlow: StateFlow<AnalysisResultComposite> = _compositeState.asStateFlow()

    // 当前激活的模式集合
    val activeModes: Set<AnalysisMode> get() = processorMap.keys.toSet()

    fun createAnalysisUseCase(
        initialModes: Set<AnalysisMode> = setOf(AnalysisMode.IMAGE_LABELING),
        scope: CoroutineScope
    ): ImageAnalysis? {
        this.coroutineScope = scope
        _imageAnalysis = ImageAnalysis.Builder().build()
        initialModes.forEach { mode -> addProcessor(mode) }
        enableAnalysis()
        return _imageAnalysis
    }

    /**
     * 添加一个处理器。若已存在同 mode 则跳过。
     */
    fun addProcessor(mode: AnalysisMode) {
        if (processorMap.containsKey(mode)) return
        val processor = VisionProcessorFactory.create(mode, context)
        processorMap[mode] = processor
        bridgeProcessorFlow(mode, processor)
    }

    /**
     * 移除一个处理器，停止并释放资源。
     */
    fun removeProcessor(mode: AnalysisMode) {
        bridgeJobs.remove(mode)?.cancel()
        processorMap.remove(mode)?.stop()
        _compositeState.update { it.clearResult(mode) }
    }

    /**
     * 桥接：收集 processor 的泛型 resultFlow，映射入组合状态
     */
    private fun bridgeProcessorFlow(mode: AnalysisMode, processor: VisionImageProcessor) {
        val base = processor as? VisionProcessorBase<*> ?: return
        val scope = coroutineScope ?: return
        bridgeJobs[mode] = scope.launch(Dispatchers.Default) {
            base.resultFlow.collect { result ->
                _compositeState.update { it.withResult(mode, result) }
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun enableAnalysis() {
        val activeProcessors = processorMap.values.toList()
        _imageAnalysis?.setAnalyzer(
            executor,
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    Log.w(TAG, "Received ImageProxy with null image, closing.")
                    imageProxy.close()
                    return@Analyzer
                }

                if (activeProcessors.isEmpty()) {
                    imageProxy.close()
                    return@Analyzer
                }

                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                // 跟踪所有处理器的完成情况，全部完成后关闭 imageProxy
                val remaining = AtomicInteger(activeProcessors.size)

                activeProcessors.forEach { processor ->
                    try {
                        processor.processInputImage(inputImage) {
                            if (remaining.decrementAndGet() == 0) {
                                imageProxy.close()
                            }
                        }
                    } catch (e: MlKitException) {
                        Log.e(TAG, "ML Kit processing failed for processor", e)
                        if (remaining.decrementAndGet() == 0) {
                            imageProxy.close()
                        }
                    }
                }
            },
        )
    }

    fun disableAnalysis() {
        _imageAnalysis?.clearAnalyzer()
    }

    /**
     * 释放所有处理器和资源。必须在不再需要分析时调用。
     */
    fun clear() {
        disableAnalysis()
        bridgeJobs.values.forEach { it.cancel() }
        bridgeJobs.clear()
        processorMap.values.forEach { it.stop() }
        processorMap.clear()
        _imageAnalysis = null
        coroutineScope = null
        executor.shutdown()
    }
}
