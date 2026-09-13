package com.scanflow.photocompressor.ui.preview

import android.content.Context
import android.graphics.Bitmap
import coil.request.CachePolicy
import coil.size.Precision
import coil.size.Scale
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImagePreviewStrategyTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
    }

    @Test
    fun `PreviewTier dimensions adhere to scaled and memory safe limits`() {
        // Thumbnail tier: micro footprint for high-count lists
        assertEquals(384, PreviewTier.THUMBNAIL.maxDimension)
        assertEquals(Bitmap.Config.RGB_565, PreviewTier.THUMBNAIL.preferredConfig)
        assertEquals(250, PreviewTier.THUMBNAIL.crossfadeMs)

        // Card preview tier: card footprint
        assertEquals(720, PreviewTier.CARD_PREVIEW.maxDimension)
        assertEquals(Bitmap.Config.ARGB_8888, PreviewTier.CARD_PREVIEW.preferredConfig)
        assertEquals(300, PreviewTier.CARD_PREVIEW.crossfadeMs)

        // Full preview tier: bounded interactive crop / inspection
        assertEquals(1280, PreviewTier.FULL_PREVIEW.maxDimension)
        assertEquals(Bitmap.Config.ARGB_8888, PreviewTier.FULL_PREVIEW.preferredConfig)
        assertEquals(300, PreviewTier.FULL_PREVIEW.crossfadeMs)
    }

    @Test
    fun `calculateThumbnailSampleSize downsamples large 4000x3000 image to safe bounds`() {
        val sampleSize = ImagePreviewStrategy.calculateThumbnailSampleSize(
            rawWidth = 4000,
            rawHeight = 3000,
            targetDimension = PreviewTier.THUMBNAIL.maxDimension // 384
        )

        // 4000 / (8 * 2) = 250 < 384 -> sampleSize = 8 (4000 / 8 = 500)
        assertTrue("Sample size should be at least 8 for 4000px image, was $sampleSize", sampleSize >= 8)
        assertTrue("Sample size must be power of 2", (sampleSize and (sampleSize - 1)) == 0)
    }

    @Test
    fun `calculateThumbnailSampleSize downsamples extreme 12000x9000 photo safely`() {
        val sampleSize = ImagePreviewStrategy.calculateThumbnailSampleSize(
            rawWidth = 12000,
            rawHeight = 9000,
            targetDimension = PreviewTier.THUMBNAIL.maxDimension // 384
        )

        assertTrue("Sample size must be >= 16, was $sampleSize", sampleSize >= 16)
        assertTrue("Sample size must be power of 2", (sampleSize and (sampleSize - 1)) == 0)
    }

    @Test
    fun `calculateThumbnailSampleSize returns 1 for small image`() {
        val sampleSize = ImagePreviewStrategy.calculateThumbnailSampleSize(
            rawWidth = 200,
            rawHeight = 200,
            targetDimension = 384
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun `calculateThumbnailSampleSize handles non-positive dimensions safely`() {
        assertEquals(1, ImagePreviewStrategy.calculateThumbnailSampleSize(0, 0, 384))
        assertEquals(1, ImagePreviewStrategy.calculateThumbnailSampleSize(-100, 500, 384))
        assertEquals(1, ImagePreviewStrategy.calculateThumbnailSampleSize(500, -100, 384))
    }

    @Test
    fun `buildPreviewRequest enforces INEXACT precision to guarantee no full resolution decode`() {
        val request = ImagePreviewStrategy.buildPreviewRequest(
            context = context,
            data = "content://media/external/images/media/1",
            tier = PreviewTier.THUMBNAIL,
            scale = Scale.FIT
        )

        // Key verification: Precision MUST be INEXACT so Coil downsamples at decode stream
        assertEquals(Precision.INEXACT, request.precision)
        assertEquals(CachePolicy.ENABLED, request.memoryCachePolicy)
        assertEquals(Bitmap.Config.RGB_565, request.bitmapConfig)
    }

    @Test
    fun `buildPreviewRequest applies crossfade animation for smooth transitions`() {
        val request = ImagePreviewStrategy.buildPreviewRequest(
            context = context,
            data = "content://media/external/images/media/2",
            tier = PreviewTier.CARD_PREVIEW
        )

        assertTrue("Transition factory must be CrossfadeTransition.Factory", request.transitionFactory is coil.transition.CrossfadeTransition.Factory)
        val crossfadeFactory = request.transitionFactory as coil.transition.CrossfadeTransition.Factory
        assertEquals(300, crossfadeFactory.durationMillis)
        assertEquals(Precision.INEXACT, request.precision)
    }
}
