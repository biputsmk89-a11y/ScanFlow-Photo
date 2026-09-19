package com.scanflow.photocompressor.engine

import android.graphics.*
import com.scanflow.photocompressor.domain.model.WatermarkConfig
import com.scanflow.photocompressor.domain.model.WatermarkPosition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for adding text watermarks to bitmaps.
 * Uses Canvas and Paint for rendering text overlay.
 */
@Singleton
class WatermarkEngine @Inject constructor() {

    /**
     * Add a text watermark to a bitmap.
     *
     * @param bitmap Source bitmap (will NOT be modified; a copy is created).
     * @param config Watermark configuration.
     * @return New bitmap with watermark applied.
     */
    fun addTextWatermark(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
        // Create a mutable copy
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Scale font size relative to image dimensions
        val scaleFactor = minOf(bitmap.width, bitmap.height) / 1000f
        val scaledFontSize = config.fontSize * scaleFactor.coerceAtLeast(1f)
        val scaledMargin = config.margin * scaleFactor.coerceAtLeast(1f)

        // Create paint for watermark text
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isSubpixelText = true
            color = config.color.toInt()
            alpha = (config.opacity * 255).toInt()
            textSize = scaledFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * scaleFactor, 2f * scaleFactor, 2f * scaleFactor, Color.argb(128, 0, 0, 0))
        }

        // Measure text bounds
        val textBounds = Rect()
        paint.getTextBounds(config.text, 0, config.text.length, textBounds)

        // Calculate position
        val (x, y) = calculatePosition(
            config.position,
            bitmap.width,
            bitmap.height,
            textBounds.width(),
            textBounds.height(),
            scaledMargin
        )

        // Apply rotation if specified
        if (config.rotation != 0f) {
            canvas.save()
            canvas.rotate(config.rotation, x + textBounds.width() / 2f, y)
            canvas.drawText(config.text, x, y, paint)
            canvas.restore()
        } else {
            canvas.drawText(config.text, x, y, paint)
        }

        return result
    }

    /**
     * Calculate the (x, y) position for the watermark text.
     */
    private fun calculatePosition(
        position: WatermarkPosition,
        imageWidth: Int,
        imageHeight: Int,
        textWidth: Int,
        textHeight: Int,
        margin: Float
    ): Pair<Float, Float> {
        val x = when (position) {
            WatermarkPosition.TOP_LEFT,
            WatermarkPosition.CENTER_LEFT,
            WatermarkPosition.BOTTOM_LEFT -> margin

            WatermarkPosition.TOP_CENTER,
            WatermarkPosition.CENTER,
            WatermarkPosition.BOTTOM_CENTER -> (imageWidth - textWidth) / 2f

            WatermarkPosition.TOP_RIGHT,
            WatermarkPosition.CENTER_RIGHT,
            WatermarkPosition.BOTTOM_RIGHT -> imageWidth - textWidth - margin
        }

        val y = when (position) {
            WatermarkPosition.TOP_LEFT,
            WatermarkPosition.TOP_CENTER,
            WatermarkPosition.TOP_RIGHT -> textHeight + margin

            WatermarkPosition.CENTER_LEFT,
            WatermarkPosition.CENTER,
            WatermarkPosition.CENTER_RIGHT -> (imageHeight + textHeight) / 2f

            WatermarkPosition.BOTTOM_LEFT,
            WatermarkPosition.BOTTOM_CENTER,
            WatermarkPosition.BOTTOM_RIGHT -> imageHeight - margin
        }

        return Pair(x, y)
    }
}
