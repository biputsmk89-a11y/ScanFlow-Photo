package com.scanflow.photocompressor.engine

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BitmapUtilsTest {

    private lateinit var context: Context
    private lateinit var bitmapUtils: BitmapUtils

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        bitmapUtils = BitmapUtils(context)
    }

    @Test
    fun `12000x9000 image with default 4096 bounds downsamples to safe inSampleSize of 4`() {
        // Raw 12000x9000 ARGB_8888 is 432 MB.
        // inSampleSize = 4 produces 3000x2250 = ~27 MB, perfectly under 64MB budget.
        val sampleSize = bitmapUtils.calculateInSampleSize(
            rawWidth = 12000,
            rawHeight = 9000,
            reqWidth = 4096,
            reqHeight = 4096
        )

        assertEquals(4, sampleSize)

        val sampledWidth = 12000 / sampleSize
        val sampledHeight = 9000 / sampleSize
        assertTrue(sampledWidth <= 4096)
        assertTrue(sampledHeight <= 4096)

        val memoryBytes = bitmapUtils.estimateBitmapMemory(sampledWidth, sampledHeight)
        assertTrue("Memory must be <= 64MB, was $memoryBytes", memoryBytes <= 64L * 1024 * 1024)
    }

    @Test
    fun `12000x9000 image with 1920x1080 target calculates inSampleSize of 4`() {
        // At inSampleSize = 4: 3000x2250, which provides >= 1920x1080 for crisp resize
        val sampleSize = bitmapUtils.calculateInSampleSize(
            rawWidth = 12000,
            rawHeight = 9000,
            reqWidth = 1920,
            reqHeight = 1080
        )

        assertEquals(4, sampleSize)
        val sampledWidth = 12000 / sampleSize
        val sampledHeight = 9000 / sampleSize
        assertTrue(sampledWidth >= 1920)
        assertTrue(sampledHeight >= 1080)
    }

    @Test
    fun `12000x9000 image with 800x600 target calculates inSampleSize of 8`() {
        // At inSampleSize = 8: 1500x1125, which uses only ~6.75MB memory
        val sampleSize = bitmapUtils.calculateInSampleSize(
            rawWidth = 12000,
            rawHeight = 9000,
            reqWidth = 800,
            reqHeight = 600
        )

        assertEquals(8, sampleSize)
        val sampledWidth = 12000 / sampleSize
        val sampledHeight = 9000 / sampleSize
        assertTrue(sampledWidth >= 800)
        assertTrue(sampledHeight >= 600)
    }

    @Test
    fun `image within safe limits returns inSampleSize of 1`() {
        val sampleSize = bitmapUtils.calculateInSampleSize(
            rawWidth = 1920,
            rawHeight = 1080,
            reqWidth = 4096,
            reqHeight = 4096
        )

        assertEquals(1, sampleSize)
    }

    @Test
    fun `estimateBitmapMemory returns accurate byte count`() {
        // 1000 x 1000 x 4 bytes = 4,000,000 bytes
        val bytes = bitmapUtils.estimateBitmapMemory(1000, 1000)
        assertEquals(4_000_000L, bytes)
    }

    @Test
    fun `getSafeMemoryBudget returns valid non-zero budget`() {
        val budget = bitmapUtils.getSafeMemoryBudget()
        assertTrue(budget > 0)
        assertTrue(budget <= BitmapUtils.MAX_BITMAP_MEMORY)
    }

    @Test
    fun `sampleSize is always a power of 2`() {
        val testCases = listOf(
            Pair(12000, 9000),
            Pair(8000, 6000),
            Pair(5000, 5000),
            Pair(16000, 12000),
            Pair(1920, 1080),
            Pair(500, 500)
        )

        for ((w, h) in testCases) {
            val sampleSize = bitmapUtils.calculateInSampleSize(w, h, 1920, 1080)
            // Power of 2 check: (n & (n - 1)) == 0
            assertTrue("Sample size $sampleSize must be power of 2", (sampleSize and (sampleSize - 1)) == 0)
        }
    }

    @Test
    fun `thumbnail sample size calculation bounds extreme raw dimensions below target`() {
        val rawWidth = 8000
        val rawHeight = 6000
        val targetDimension = 512

        var sampleSize = 1
        val maxRaw = maxOf(rawWidth, rawHeight)
        while ((maxRaw / (sampleSize * 2)) >= targetDimension) {
            sampleSize *= 2
        }

        assertEquals(8, sampleSize)
        val sampledDim = maxRaw / sampleSize
        assertTrue("Sampled dimension must be close to target", sampledDim <= targetDimension * 2)
    }
}
