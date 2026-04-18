package com.mjc.feature.camera.model

data class DetectedTextUiModel(
    val text: String,
    val textBlocks: List<String>,
)