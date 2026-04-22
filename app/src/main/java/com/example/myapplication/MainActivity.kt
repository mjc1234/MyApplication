package com.example.myapplication

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.ui.theme.AppTheme
import com.example.navigation.rememberNavigationState
import com.example.scene.rememberListDetailSceneStrategy2
import com.mjc.feature.camera.navigation.CameraKey
import com.mjc.feature.camera.navigation.cameraEntry
import com.mjc.feature.download.navigation.DownloadKey
import com.mjc.feature.download.navigation.downloadEntry
import com.mjc.feature.videoplayer.navigation.VideoPlayerKey
import com.mjc.feature.videoplayer.navigation.videoPlayerEntry
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 边缘到边缘显示
        enableEdgeToEdge()

        // Android 11+ 适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        setContent {
            AppTheme {
                val navState = rememberNavigationState(DownloadKey(""))
                val listDetailStrategy = rememberListDetailSceneStrategy2<NavKey>()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.padding(innerPadding),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        backStack = navState.backStack,
                        entryProvider = entryProvider {
                            cameraEntry()
                            videoPlayerEntry()
                            downloadEntry()
                        },
                        sceneStrategy = listDetailStrategy
                    )
                }
            }
        }
    }
}