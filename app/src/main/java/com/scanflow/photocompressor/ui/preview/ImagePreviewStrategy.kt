package com.scanflow.photocompressor.ui.preview

import android.content.Context
import android.graphics.Bitmap
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.Dispatchers

/**
 * Strategy for loading UI image previews and thumbnails.
 *
 * Guarantees:
 * 1. Scaled: Never decodes at full-resolution for thumbnails and preview cards.
 * 2. Memory safe: Bounded dimensions, sub-sampled decoding via Precision.INEXACT,
 *    hardware bitmap support, and strict memory limits.
 * 3. Smooth: Crossfade transitions, background IO decoding to prevent UI frame drops.
 */
enum class PreviewTier(
    val maxDimension: Int,
    val preferredConfig: Bitmap.Config,
    val crossfadeMs: Int
) {
    /**
     * Micro thumbnail for list items, batch items, history tiles, etc.
     * Subsampled to max 384px to consume minimal memory while remaining crisp.
     */
    THUMBNAIL(
        maxDimension = 384,
        preferredConfig = Bitmap.Config.RGB_565,
        crossfadeMs = 250
    ),

    /**
     * Card preview for picker cards, side-by-side comparisons, etc.
     * Subsampled to max 720px.
     */
    CARD_PREVIEW(
        maxDimension = 720,
        preferredConfig = Bitmap.Config.ARGB_8888,
        crossfadeMs = 300
    ),

    /**
     * Full preview for detailed inspection, crop target, full-screen toggle preview.
     * Subsampled to max 1280px (never decodes 48MP/12MP raw pictures directly).
     */
    FULL_PREVIEW(
        maxDimension = 1280,
        preferredConfig = Bitmap.Config.ARGB_8888,
        crossfadeMs = 300
    )
}

object ImagePreviewStrategy {

    /**
     * Builds a memory-safe, scaled, smooth Coil [ImageRequest].
     *
     * [Precision.INEXACT] ensures Coil uses inSampleSize downsampling during BitmapFactory decode,
     * completely avoiding full-resolution bitmap allocation in memory.
     */
    fun buildPreviewRequest(
        context: Context,
        data: Any?,
        tier: PreviewTier = PreviewTier.CARD_PREVIEW,
        scale: Scale = Scale.FIT
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .size(tier.maxDimension, tier.maxDimension)
            .precision(Precision.INEXACT) // Essential: downsamples using inSampleSize, never full-res
            .bitmapConfig(tier.preferredConfig)
            .crossfade(tier.crossfadeMs)
            .scale(scale)
            .allowHardware(true)
            .dispatcher(Dispatchers.IO)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    /**
     * Calculate power-of-2 inSampleSize to guarantee no full-resolution decode occurs
     * when decoding directly from content stream.
     */
    fun calculateThumbnailSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        targetDimension: Int = PreviewTier.THUMBNAIL.maxDimension
    ): Int {
        if (rawWidth <= 0 || rawHeight <= 0) return 1
        var sampleSize = 1
        val maxDim = maxOf(rawWidth, rawHeight)
        while ((maxDim / (sampleSize * 2)) >= targetDimension) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }
}
