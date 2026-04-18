package com.mjc.feature.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mjc.feature.camera.model.DetectedImageLabelUiModel

@Composable
fun LabelOverlay(
    labels: List<DetectedImageLabelUiModel>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEach { label ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.6f),
                contentColor = Color.White
            ) {
                Text(
                    text = "${label.text} ${(label.confidence * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TextOverlay(
    textBlocks: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White
    ) {
        Text(
            text = textBlocks.joinToString("\n"),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
