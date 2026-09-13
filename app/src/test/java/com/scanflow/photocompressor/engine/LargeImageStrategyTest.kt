package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.ProcessingError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeImageStrategyTest {

    private val oneMB = 1024L * 1024L

    @Test
    fun `determineTier correctly classifies 1-5MB as NORMAL`() {
        val tier1MB = LargeImageStrategy.determineTier(1L * oneMB)
        val tier3MB = LargeImageStrategy.determineTier(3L * oneMB)
        val tier5MB = LargeImageStrategy.determineTier(5L * oneMB)

        assertEquals(ImageSizeTier.NORMAL, tier1MB)
        assertEquals(ImageSizeTier.NORMAL, tier3MB)
        assertEquals(ImageSizeTier.NORMAL, tier5MB)
    }

    @Test
    fun `determineTier correctly classifies 10-30MB as LARGE`() {
        val tier10MB = LargeImageStrategy.determineTier(10L * oneMB)
        val tier20MB = LargeImageStrategy.determineTier(20L * oneMB)
        val tier30MB = LargeImageStrategy.determineTier(30L * oneMB)

        assertEquals(ImageSizeTier.LARGE, tier10MB)
        assertEquals(ImageSizeTier.LARGE, tier20MB)
        assertEquals(ImageSizeTier.LARGE, tier30MB)
    }

    @Test
    fun `determineTier correctly classifies 50-100MB as VERY_LARGE`() {
        val tier50MB = LargeImageStrategy.determineTier(50L * oneMB)
        val tier75MB = LargeImageStrategy.determineTier(75L * oneMB)
        val tier100MB = LargeImageStrategy.determineTier(100L * oneMB)

        assertEquals(ImageSizeTier.VERY_LARGE, tier50MB)
        assertEquals(ImageSizeTier.VERY_LARGE, tier75MB)
        assertEquals(ImageSizeTier.VERY_LARGE, tier100MB)
    }

    @Test
    fun `normal tier creates fast decode plan with ARGB_8888 and full bounds`() {
        val plan = LargeImageStrategy.createDecodePlan(
            fileSizeBytes = 3L * oneMB,
            rawWidth = 4000,
            rawHeight = 3000,
            availableHeapBytes = 128L * oneMB
        )

        assertEquals(ImageSizeTier.NORMAL, plan.tier)
        assertFalse(plan.shouldReject)
        assertEquals(1, plan.inSampleSize)
        assertEquals(Bitmap.Config.ARGB_8888, plan.preferredConfig)
        assertEquals(4096, plan.maxDimension)
    }

    @Test
    fun `large tier creates stable decode plan capped at 3840px`() {
        val plan = LargeImageStrategy.createDecodePlan(
            fileSizeBytes = 25L * oneMB,
            rawWidth = 8000,
            rawHeight = 6000,
            availableHeapBytes = 128L * oneMB
        )

        assertEquals(ImageSizeTier.LARGE, plan.tier)
        assertFalse(plan.shouldReject)
        assertEquals(3840, plan.maxDimension)
    }

    @Test
    fun `very large tier gracefully rejects when memory headroom is critically low`() {
        // Device has only 20MB free heap memory (< 48MB critical threshold)
        val plan = LargeImageStrategy.createDecodePlan(
            fileSizeBytes = 80L * oneMB,
            rawWidth = 12000,
            rawHeight = 9000,
            availableHeapBytes = 20L * oneMB
        )

        assertEquals(ImageSizeTier.VERY_LARGE, plan.tier)
        assertTrue("Very large image must be gracefully rejected on low memory", plan.shouldReject)
        assertTrue(plan.rejectionReason?.contains("rejected") == true)
    }

    @Test
    fun `very large tier applies safe fallback with RGB_565 when memory allows`() {
        // Device has ample free heap (256MB)
        val plan = LargeImageStrategy.createDecodePlan(
            fileSizeBytes = 60L * oneMB,
            rawWidth = 10000,
            rawHeight = 8000,
            availableHeapBytes = 256L * oneMB
        )

        assertEquals(ImageSizeTier.VERY_LARGE, plan.tier)
        assertFalse(plan.shouldReject)
        assertTrue(plan.inSampleSize >= 4)
        assertEquals(Bitmap.Config.RGB_565, plan.preferredConfig)
        assertEquals(2048, plan.maxDimension)
    }

    @Test
    fun `ProcessingError fromThrowable cleanly maps LargeImageRejectedException to FileTooLarge`() {
        val exception = LargeImageRejectedException(
            fileSizeMB = 75.5,
            rawWidth = 12000,
            rawHeight = 9000,
            message = "Processing safely rejected to prevent device freeze"
        )

        val error = ProcessingError.fromThrowable(exception)
        assertTrue(error is ProcessingError.FileTooLarge)
        val fileTooLarge = error as ProcessingError.FileTooLarge
        assertEquals(75.5, fileTooLarge.fileSizeMB, 0.01)
        assertTrue(error.userFacingMessage.contains("too large"))
    }
}
