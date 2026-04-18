package com.mjc.feature.videoplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mjc.feature.videoplayer.picker.VideoPickerState

/**
 * 紧凑型视频文件选择按钮
 */
@Composable
fun VideoPickerButton(
    pickerState: VideoPickerState,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = when (pickerState) {
        is VideoPickerState.Success -> Color.Green
        is VideoPickerState.Error -> MaterialTheme.colorScheme.error
        else -> Color.White
    }

    IconButton(
        onClick = onPickVideo,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        )
    ) {
        if (pickerState is VideoPickerState.Picking) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "选择视频",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
