package com.mjc.feature.camera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mjc.AnalysisMode

private val AnalysisMode.displayName: String
    get() = when (this) {
        AnalysisMode.IMAGE_LABELING -> "Labels"
        AnalysisMode.TEXT_RECOGNITION -> "Text"
        AnalysisMode.FACE_DETECTION -> "Face"
        AnalysisMode.BARCODE_SCANNING -> "Barcode"
        AnalysisMode.OBJECT_DETECTION -> "Object"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisModeSelector(
    allModes: List<AnalysisMode>,
    activeModes: Set<AnalysisMode>,
    onModeToggle: (AnalysisMode, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allModes.forEach { mode ->
            FilterChip(
                selected = mode in activeModes,
                onClick = { onModeToggle(mode, mode !in activeModes) },
                label = { Text(mode.displayName) }
            )
        }
    }
}
