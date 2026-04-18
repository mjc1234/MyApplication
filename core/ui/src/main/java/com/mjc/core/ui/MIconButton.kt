package com.mjc.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement

/** 渐变边框宽度 */
private val OutlineWidth = 1.dp

/**
 * 带有 SweepGradient 渐变描边的圆形图标按钮。
 *
 * @param icon              需要展示的图标，以 [ImageVector] 形式注入。
 * @param onClick           按钮点击回调。
 * @param contentDescription 用于无障碍的描述文本（必需）。
 * @param modifier          可选的 [Modifier]。
 * @param size              按钮整体尺寸，默认 96.dp。
 * @param iconSize          图标框架尺寸，默认 58.dp。
 */
@Composable
fun MIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit = {},
    size: Dp = 96.dp,
    iconSize: Dp = 58.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconButtonScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = 2.dp, shape = CircleShape)
            .background(color = MaterialTheme.colorScheme.onBackground, shape = CircleShape)
            .border(
                width = OutlineWidth,
                brush = GradientColors.outlineSweepBrush(),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(name = "MIconButton - Default", showBackground = true, backgroundColor = 0xFF1A1A1A)
@Preview(name = "MIconButton - Small", showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun MIconButtonPreview() {
    MaterialTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            MIconButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "播放"
            )
            MIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "设置",
                size = 48.dp,
                iconSize = 24.dp,
            )
        }
    }
}
