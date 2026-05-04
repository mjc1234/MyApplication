package com.mjc.feature.renderer

import android.view.Surface
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper

@Composable
fun RendererScreen(
    modifier: Modifier = Modifier,
    viewModel: RendererViewModel = viewModel()
) {
    val context = LocalContext.current

    val disPlayerHelper = remember(context) {
        DisplayHelper(context)
    }

    val uiHelper = remember {
        UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(p0: Surface?) {
                }

                override fun onDetachedFromSurface() {
                    TODO("Not yet implemented")
                }

                override fun onResized(p0: Int, p1: Int) {
                    TODO("Not yet implemented")
                }
            }
        }
    }



    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { context ->
                SurfaceView(context).apply {
                    uiHelper.attachTo(this)
                }
            },
            update = { view ->
                uiHelper.attachTo(view)
            },
        )
    }

}