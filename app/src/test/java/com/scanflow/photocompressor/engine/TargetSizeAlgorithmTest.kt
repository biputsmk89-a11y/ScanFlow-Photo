package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.ImageFormat
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.OutputStream

class TargetSizeAlgorithmTest {

    private lateinit var compressionEngine: CompressionEngine

    @Before
    fun setup() {
        compressionEngine = CompressionEngine()
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers {
            val w = secondArg<Int>()
            val h = thirdArg<Int>()
            val scaled = mockk<Bitmap>(relaxed = true)
            every { scaled.width } returns w
            every { scaled.height } returns h
            scaled
        }
    }

    @After
    fun teardown() {
        unmockkStatic(Bitmap::class)
    }

    @Test
    fun `binary search finds optimal quality within 7 iterations for feasible target`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 2000
        every { bitmap.height } returns 1500

        var compressCallCount = 0
        // Simulate output size that scales linearly with quality: size = quality * 1000 bytes
        // Quality 20 = 20 KB, Quality 50 = 50 KB, Quality 95 = 95 KB
        every { bitmap.compress(any(), any(), any<OutputStream>()) } answers {
            compressCallCount++
            val quality = secondArg<Int>()
            val stream = thirdArg<OutputStream>()
            val simulatedBytes = ByteArray(quality * 1000)
            stream.write(simulatedBytes)
            true
        }

        val targetBytes = 55_000L // 55 KB target -> Quality ~55 should be found
        val result = compressionEngine.compressToTargetSize(
            bitmap = bitmap,
            targetSizeBytes = targetBytes,
            format = ImageFormat.JPEG
        )

        assertTrue("Feasible target should return isFeasible = true", result.isFeasible)
        assertTrue("Output bytes must be <= targetBytes", result.bytes.size <= targetBytes)
        assertTrue("Iteration count must not exceed 7 binary search iterations", compressCallCount <= 7)
        assertTrue("Quality must be >= 20 and <= 95", result.quality in 20..95)
        assertEquals("Target reached", result.message)

        // Verify destructuring works
        val (destructuredBytes, destructuredQuality) = result
        assertEquals(result.bytes.size, destructuredBytes.size)
        assertEquals(result.quality, destructuredQuality)
    }

    @Test
    fun `dimension reduction triggers when quality reduction alone is insufficient`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 2000
        every { bitmap.height } returns 1500

        // Simulate base image where at quality 20, size is still 200 KB on original dimensions
        // Each downscale (0.8x) reduces byte size by factor of ~0.64
        every { bitmap.compress(any(), any(), any<OutputStream>()) } answers {
            val stream = thirdArg<OutputStream>()
            val simulatedBytes = ByteArray(200_000)
            stream.write(simulatedBytes)
            true
        }

        // Target is 100 KB, but original bitmap at quality 20 is 200 KB
        // When scaled bitmap is compressed, let it produce 90 KB
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers {
            val scaled = mockk<Bitmap>(relaxed = true)
            every { scaled.compress(any(), any(), any<OutputStream>()) } answers {
                val stream = thirdArg<OutputStream>()
                val simulatedBytes = ByteArray(90_000) // fits under 100 KB!
                stream.write(simulatedBytes)
                true
            }
            scaled
        }

        val targetBytes = 100_000L
        val result = compressionEngine.compressToTargetSize(
            bitmap = bitmap,
            targetSizeBytes = targetBytes,
            format = ImageFormat.JPEG
        )

        assertTrue(result.isFeasible)
        assertTrue(result.bytes.size <= targetBytes)
        assertEquals(90_000, result.bytes.size)
    }

    @Test
    fun `unfeasible target size does not crash, terminates gracefully, and sets clear user explanation`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 2000
        every { bitmap.height } returns 1500

        // Minimum possible compression is 50,000 bytes no matter what
        every { bitmap.compress(any(), any(), any<OutputStream>()) } answers {
            val stream = thirdArg<OutputStream>()
            stream.write(ByteArray(50_000))
            true
        }
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers {
            val scaled = mockk<Bitmap>(relaxed = true)
            every { scaled.compress(any(), any(), any<OutputStream>()) } answers {
                val stream = thirdArg<OutputStream>()
                stream.write(ByteArray(50_000))
                true
            }
            scaled
        }

        val impossiblySmallTarget = 500L // 500 bytes is impossibly small for 50 KB payload
        val result = compressionEngine.compressToTargetSize(
            bitmap = bitmap,
            targetSizeBytes = impossiblySmallTarget,
            format = ImageFormat.JPEG
        )

        assertFalse("Unfeasible target should mark isFeasible = false", result.isFeasible)
        assertNotNull("Should not produce null or corrupt bytes", result.bytes)
        assertTrue("Should contain output bytes", result.bytes.isNotEmpty())
        assertEquals(
            "The target size could not be reached without significantly reducing image quality or dimensions.",
            result.message
        )
    }
}
