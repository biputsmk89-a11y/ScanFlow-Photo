package com.scanflow.photocompressor.engine.backgroundremoval

import android.graphics.Bitmap

/**
 * Clean architectural abstraction decoupling AI segmentation models from domain/presentation layers.
 */
interface BackgroundRemovalEngine {

    /**
     * Performs AI inference on the input image to extract human subject mask.
     */
    suspend fun segment(
        image: Bitmap,
        options: BackgroundRemovalOptions = BackgroundRemovalOptions()
    ): SegmentationResult

    /**
     * Removes the background from a portrait bitmap, returning a transparent ARGB_8888 bitmap.
     */
    suspend fun removeBackground(
        image: Bitmap,
        options: BackgroundRemovalOptions = BackgroundRemovalOptions()
    ): Bitmap

    /**
     * Replaces the background with a uniform studio color (e.g. Blue, Red, White, Gray).
     */
    suspend fun replaceBackground(
        image: Bitmap,
        backgroundColor: Int,
        options: BackgroundRemovalOptions = BackgroundRemovalOptions()
    ): Bitmap
}
