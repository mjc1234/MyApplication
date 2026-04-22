package com.mjc.feature.download.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.scene.ListDetailSceneStrategy2
import com.mjc.feature.download.DownloadScreen
import kotlinx.serialization.Serializable

@Serializable
data class DownloadKey(val url: String): NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.downloadEntry() {
    entry<DownloadKey>(
        metadata = ListDetailSceneStrategy2.detailPane()
    ) { key ->
        DownloadScreen {  }
    }
}
