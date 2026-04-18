package com.mjc.feature.camera.model

/**
 * 多处理器组合 UI 状态。每个字段对应一种处理器的 UI 数据。
 * 字段为 null 表示该处理器未激活或无结果。
 */
data class AnalysisUiComposite(
    val labels: List<DetectedImageLabelUiModel>? = null,
    val text: DetectedTextUiModel? = null,
    // 后续按需扩展：
    // val faces: List<FaceUiModel>? = null,
    // val barcodes: List<BarcodeUiModel>? = null,
    // val objects: List<ObjectUiModel>? = null,
)
