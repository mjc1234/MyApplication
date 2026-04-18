package com.mjc.feature.camera.utils

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.mjc.core.mlkit.util.BitmapUtils as NativeBitmapUtils
import com.mjc.core.mlkit.util.FrameMetadata
import java.nio.ByteBuffer

object BitmapUtils {
    /** Converts a YUV_420_888 image from CameraX API to a bitmap.  */
    @OptIn(ExperimentalGetImage::class)
    fun getBitmap(image: ImageProxy): Bitmap? {
        val mediaImage = image.image ?: return null
        val frameMetadata: FrameMetadata =
            FrameMetadata.Builder()
                .setWidth(image.width)
                .setHeight(image.height)
                .setRotation(image.imageInfo.rotationDegrees)
                .build()

        val nv21Buffer: ByteBuffer =
            NativeBitmapUtils.yuv420ThreePlanesToNV21(
                mediaImage.planes,
                image.width,
                image.height
            )
        return NativeBitmapUtils.getBitmap(nv21Buffer, frameMetadata)
    }
}