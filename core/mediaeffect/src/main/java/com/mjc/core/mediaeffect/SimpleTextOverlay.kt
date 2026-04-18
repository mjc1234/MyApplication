package com.mjc.core.mediaeffect

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.SpannableString
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.TextOverlay
import androidx.core.graphics.createBitmap

/**
 * A concrete [TextOverlay] that renders configurable text onto a video frame.
 *
 * @param textProvider a lambda that returns the [SpannableString] to display for
 *   the given presentation time (in microseconds). Use this for time-varying
 *   overlays such as subtitles or timestamps.
 * @param textSizePx text size in pixels. Defaults to [TextOverlay.TEXT_SIZE_PIXELS].
 * @param textColor text color. Defaults to white.
 * @param backgroundColor optional background color behind the text.
 *   Pass `null` (default) for a transparent background.
 * @param overlaySettings optional per-frame [OverlaySettings]. When `null` the
 *   overlay is drawn with default alpha and anchor settings.
 */
@UnstableApi
class SimpleTextOverlay(
    private val textProvider: (presentationTimeUs: Long) -> SpannableString,
    private val textSizePx: Int = TEXT_SIZE_PIXELS,
    private val textColor: Int = Color.WHITE,
    private val backgroundColor: Int? = null,
    private val overlaySettings: OverlaySettings? = null,
) : TextOverlay() {

    override fun getText(presentationTimeUs: Long): SpannableString {
        return textProvider(presentationTimeUs)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val text = getText(presentationTimeUs)

        val textPaint = Paint().apply {
            this.textSize = textSizePx.toFloat()
            color = textColor
            isAntiAlias = true
        }

        val fontMetrics = textPaint.fontMetrics
        val textWidth = textPaint.measureText(text.toString())
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        val bgPadding = if (backgroundColor != null) (textSizePx * 0.2f).toInt() else 0

        val bitmapWidth = (textWidth + 2 * bgPadding).toInt()
        val bitmapHeight = (textHeight + 2 * bgPadding).toInt()

        val bitmap = createBitmap(bitmapWidth.coerceAtLeast(1), bitmapHeight.coerceAtLeast(1))

        val canvas = Canvas(bitmap)

        if (backgroundColor != null) {
            canvas.drawColor(backgroundColor)
        }

        canvas.drawText(
            text.toString(),
            bgPadding.toFloat(),
            bgPadding.toFloat() - fontMetrics.ascent,
            textPaint,
        )

        return bitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        return overlaySettings ?: super.getOverlaySettings(presentationTimeUs)
    }

    companion object
}
