package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.ImageFormat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for converting between image formats.
 * Wraps the compression engine with format-specific logic.
 */
@Singleton
class FormatConverter @Inject constructor(
    private val compressionEngine: CompressionEngine
) {
    /**
     * Flattens a bitmap with transparency onto an opaque background color (default White).
     */
    fun flattenAlpha(bitmap: Bitmap, backgroundColor: Int = android.graphics.Color.WHITE): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return output
    }

    /**
     * Convert a bitmap to a different format.
     * When converting an image with transparency to JPEG, transparent pixels
     * are flattened onto the specified background color (default White).
     *
     * @param bitmap Source bitmap.
     * @param targetFormat The target image format.
     * @param quality Compression quality for lossy formats (1-100).
     * @param backgroundColor Background color used when flattening alpha onto JPEG.
     * @return Compressed byte array in the target format.
     */
    fun convert(
        bitmap: Bitmap,
        targetFormat: ImageFormat,
        quality: Int = 90,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): ByteArray {
        val workingBitmap = if (targetFormat == ImageFormat.JPEG && bitmap.hasAlpha()) {
            flattenAlpha(bitmap, backgroundColor)
        } else {
            bitmap
        }

        val bytes = compressionEngine.compress(workingBitmap, targetFormat, quality)
        if (workingBitmap !== bitmap) {
            workingBitmap.recycle()
        }
        return bytes
    }

    /**
     * Get optimal quality setting for a format conversion.
     * PNG is lossless so quality doesn't apply (always 100).
     */
    fun getRecommendedQuality(format: ImageFormat): Int {
        return when (format) {
            ImageFormat.JPEG -> 85
            ImageFormat.PNG -> 100  // PNG is lossless
            ImageFormat.WEBP -> 80
            ImageFormat.WEBP_LOSSLESS -> 100
        }
    }

    /**
     * Check if the format supports lossy compression.
     */
    fun isLossyFormat(format: ImageFormat): Boolean {
        return when (format) {
            ImageFormat.JPEG -> true
            ImageFormat.PNG -> false
            ImageFormat.WEBP -> true
            ImageFormat.WEBP_LOSSLESS -> false
        }
    }
}
