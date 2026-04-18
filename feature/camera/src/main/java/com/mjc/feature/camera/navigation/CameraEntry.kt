package com.mjc.feature.camera.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.navigation.NavigationState
import com.example.scene.ListDetailSceneStrategy2
import com.mjc.feature.camera.CameraScreen
import kotlinx.serialization.Serializable

@Serializable
data object CameraKey: NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.cameraEntry() {
    entry<CameraKey>(
        metadata = ListDetailSceneStrategy2.listPane()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(color = Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            CameraScreen {  }
        }
    }
}