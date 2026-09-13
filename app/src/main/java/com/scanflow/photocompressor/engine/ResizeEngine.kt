package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for resizing bitmaps with aspect ratio control.
 * Uses Bitmap.createScaledBitmap with bilinear filtering.
 */
@Singleton
class ResizeEngine @Inject constructor() {

    /**
     * Resize a bitmap to the target dimensions.
     *
     * @param bitmap Source bitmap.
     * @param targetWidth Target width in pixels. 0 = auto-calculate from height.
     * @param targetHeight Target height in pixels. 0 = auto-calculate from width.
     * @param maintainAspectRatio If true, scales proportionally to fit within target dimensions.
     * @return Resized bitmap (may be the same instance if no resize needed).
     */
    fun resize(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        maintainAspectRatio: Boolean = true
    ): Bitmap {
        if (targetWidth <= 0 && targetHeight <= 0) return bitmap

        val (newWidth, newHeight) = if (maintainAspectRatio) {
            calculateAspectRatioDimensions(
                bitmap.width, bitmap.height,
                targetWidth, targetHeight
            )
        } else {
            val w = if (targetWidth > 0) targetWidth else bitmap.width
            val h = if (targetHeight > 0) targetHeight else bitmap.height
            Pair(w, h)
        }

        // Don't upscale if already smaller
        if (newWidth >= bitmap.width && newHeight >= bitmap.height) return bitmap

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Resize a bitmap by percentage.
     *
     * @param bitmap Source bitmap.
     * @param percentage Scale percentage (0.01 to 1.0 for downscale, > 1.0 for upscale).
     * @return Resized bitmap.
     */
    fun resizeByPercentage(bitmap: Bitmap, percentage: Float): Bitmap {
        require(percentage > 0f) { "Percentage must be > 0" }

        val newWidth = (bitmap.width * percentage).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * percentage).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Resize a bitmap by constraining its maximum dimension (width or height).
     *
     * @param bitmap Source bitmap.
     * @param maxDimension The maximum allowed dimension for both width and height.
     * @param maintainAspectRatio If true (default), scales proportionally so neither width nor height exceeds maxDimension.
     * @return Resized bitmap (or original if already within bounds).
     */
    fun resizeByMaxDimension(
        bitmap: Bitmap,
        maxDimension: Int,
        maintainAspectRatio: Boolean = true
    ): Bitmap {
        if (maxDimension <= 0) return bitmap
        if (bitmap.width <= maxDimension && bitmap.height <= maxDimension) return bitmap

        return if (maintainAspectRatio) {
            val (newWidth, newHeight) = calculateAspectRatioDimensions(
                bitmap.width,
                bitmap.height,
                maxDimension,
                maxDimension
            )
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            // Non-aspect ratio: constrain the larger side to maxDimension
            val newWidth = if (bitmap.width > maxDimension) maxDimension else bitmap.width
            val newHeight = if (bitmap.height > maxDimension) maxDimension else bitmap.height
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }

    /**
     * Resize a bitmap according to a standard resolution preset.
     * Presets supported: 1080, 1440, 1600, 1920, 2560, Custom.
     *
     * @param bitmap Source bitmap.
     * @param preset The target ResizePreset.
     * @param maintainAspectRatio If true (default), scales proportionally.
     * @return Resized bitmap.
     */
    fun resizeByPreset(
        bitmap: Bitmap,
        preset: com.scanflow.photocompressor.domain.model.ResizePreset,
        maintainAspectRatio: Boolean = true
    ): Bitmap {
        if (preset == com.scanflow.photocompressor.domain.model.ResizePreset.CUSTOM || preset.dimension <= 0) {
            return bitmap
        }
        return resizeByMaxDimension(bitmap, preset.dimension, maintainAspectRatio)
    }

    /**
     * Calculate dimensions that fit within max bounds while maintaining aspect ratio.
     */
    fun calculateAspectRatioDimensions(
        srcWidth: Int,
        srcHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val effectiveMaxWidth = if (maxWidth > 0) maxWidth else srcWidth
        val effectiveMaxHeight = if (maxHeight > 0) maxHeight else srcHeight

        val widthRatio = effectiveMaxWidth.toFloat() / srcWidth
        val heightRatio = effectiveMaxHeight.toFloat() / srcHeight

        // Use the smaller ratio to ensure the image fits within bounds
        val ratio = minOf(widthRatio, heightRatio)

        val newWidth = (srcWidth * ratio).toInt().coerceAtLeast(1)
        val newHeight = (srcHeight * ratio).toInt().coerceAtLeast(1)

        return Pair(newWidth, newHeight)
    }

    /**
     * Calculate dimensions for a specific aspect ratio within given bounds.
     */
    fun calculateDimensionsForAspectRatio(
        srcWidth: Int,
        srcHeight: Int,
        aspectRatioX: Int,
        aspectRatioY: Int
    ): Pair<Int, Int> {
        val targetRatio = aspectRatioX.toFloat() / aspectRatioY.toFloat()
        val srcRatio = srcWidth.toFloat() / srcHeight.toFloat()

        return if (srcRatio > targetRatio) {
            // Source is wider, constrain by height
            val newWidth = (srcHeight * targetRatio).toInt()
            Pair(newWidth, srcHeight)
        } else {
            // Source is taller, constrain by width
            val newHeight = (srcWidth / targetRatio).toInt()
            Pair(srcWidth, newHeight)
        }
    }
}
