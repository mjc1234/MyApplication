package com.mjc.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 共用渐变颜色定义，供各 UI 组件复用。
 */
object GradientColors {

    /** IconButton 外框渐变的起始颜色 */
    val OutlineStart = Color(0x00000000)

    /** IconButton 外框渐变的结束颜色 */
    val OutlineEnd = Color(0xFF1A1A1A)

    /** 缓存的 SweepGradient Brush，颜色不变时可复用 */
    private val cachedOutlineBrush: Brush by lazy {
        Brush.linearGradient(colors = listOf(OutlineStart, OutlineEnd), start = Offset.Infinite.copy(x = 0f), end = Offset.Infinite.copy(y = 0f))
    }

    /**
     * 获取以 [OutlineStart] → [OutlineEnd] 为色值的 SweepGradient [Brush]。
     * 返回缓存的实例，避免重复创建。
     */
    fun outlineSweepBrush(): Brush = cachedOutlineBrush
}
