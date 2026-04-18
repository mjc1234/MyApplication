package com.mjc

import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.text.Text

/**
 * 多处理器组合结果。每个字段对应一种处理器的最新输出。
 * 字段为 null 表示该处理器未激活或尚未产出结果。
 */
data class AnalysisResultComposite(
    val imageLabels: List<ImageLabel>? = null,
    val text: Text? = null,
    // 后续按需扩展：
    // val faces: List<com.google.mlkit.vision.face.Face>? = null,
    // val barcodes: List<com.google.mlkit.vision.barcode.common.Barcode>? = null,
    // val objects: List<com.google.mlkit.vision.objects.DetectedObject>? = null,
) {
    @Suppress("UNCHECKED_CAST")
    fun withResult(mode: AnalysisMode, result: Any?): AnalysisResultComposite {
        return when (mode) {
            AnalysisMode.IMAGE_LABELING ->
                copy(imageLabels = result as? List<ImageLabel>)
            // 后续扩展其他 mode 的映射
            AnalysisMode.TEXT_RECOGNITION ->
                copy(text = result as? Text)
            else -> this
        }
    }

    fun clearResult(mode: AnalysisMode): AnalysisResultComposite {
        return when (mode) {
            AnalysisMode.IMAGE_LABELING -> copy(imageLabels = null)
            // 后续扩展其他 mode 的清空
            else -> this
        }
    }
}
