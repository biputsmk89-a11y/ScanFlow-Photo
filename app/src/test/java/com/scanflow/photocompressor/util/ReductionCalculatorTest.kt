package com.scanflow.photocompressor.util

import android.net.Uri
import com.scanflow.photocompressor.domain.model.CompressionResult
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.OperationType
import com.scanflow.photocompressor.domain.model.ProcessingHistory
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ReductionCalculatorTest {

    @Test
    fun `calculateSavedBytes calculates originalBytes minus outputBytes`() {
        val originalBytes = 1_000_000L
        val outputBytes = 350_000L
        val savedBytes = ReductionCalculator.calculateSavedBytes(originalBytes, outputBytes)

        assertEquals(650_000L, savedBytes)
    }

    @Test
    fun `calculateSavedBytes correctly handles when output is larger than original`() {
        val originalBytes = 200_000L
        val outputBytes = 250_000L
        val savedBytes = ReductionCalculator.calculateSavedBytes(originalBytes, outputBytes)

        assertEquals(-50_000L, savedBytes)
    }

    @Test
    fun `calculateReductionPercent computes exact reduction percentage`() {
        val originalBytes = 1_000_000L
        val outputBytes = 250_000L
        // ((1_000_000 - 250_000) / 1_000_000) * 100 = 75.0%
        val reduction = ReductionCalculator.calculateReductionPercent(originalBytes, outputBytes)

        assertEquals(75.0, reduction, 0.001)
    }

    @Test
    fun `calculateReductionPercent safely handles originalBytes equal to zero`() {
        val originalBytes = 0L
        val outputBytes = 100L
        val reduction = ReductionCalculator.calculateReductionPercent(originalBytes, outputBytes)

        assertEquals(0.0, reduction, 0.0)
    }

    @Test
    fun `calculateReductionPercent safely handles originalBytes less than zero`() {
        val originalBytes = -500L
        val outputBytes = 100L
        val reduction = ReductionCalculator.calculateReductionPercent(originalBytes, outputBytes)

        assertEquals(0.0, reduction, 0.0)
    }

    @Test
    fun `calculateReductionPercentFloat computes exact float reduction and handles non-positive originalBytes safely`() {
        val original = 2_000L
        val output = 1_200L
        val floatReduction = ReductionCalculator.calculateReductionPercentFloat(original, output)
        assertEquals(40.0f, floatReduction, 0.001f)

        assertEquals(0f, ReductionCalculator.calculateReductionPercentFloat(0L, 100L), 0.0f)
        assertEquals(0f, ReductionCalculator.calculateReductionPercentFloat(-100L, 0L), 0.0f)
    }

    @Test
    fun `CompressionResult reductionPercent and savedBytes match standard formulas safely`() {
        val mockUri = mockk<Uri>()
        val result = CompressionResult(
            originalSize = 800_000L,
            compressedSize = 200_000L,
            outputUri = mockUri,
            outputFileName = "IMG_test_compressed.jpg",
            width = 1920,
            height = 1080,
            format = ImageFormat.JPEG,
            quality = 80,
            durationMs = 120L
        )

        assertEquals(600_000L, result.savedBytes)
        assertEquals(75.0, result.reductionPercent, 0.001)
        assertEquals(75.0, result.savedPercentage, 0.001)

        val zeroResult = CompressionResult(
            originalSize = 0L,
            compressedSize = 0L,
            outputUri = mockUri,
            outputFileName = "zero.jpg",
            width = 0,
            height = 0,
            format = ImageFormat.JPEG,
            quality = 80,
            durationMs = 0L
        )

        assertEquals(0L, zeroResult.savedBytes)
        assertEquals(0.0, zeroResult.reductionPercent, 0.0)
    }

    @Test
    fun `ProcessingHistory reduction calculations handle originalSize zero safely`() {
        val history = ProcessingHistory(
            operation = OperationType.COMPRESS,
            originalSize = 1_000L,
            resultSize = 600L
        )
        assertEquals(400L, history.savedBytes)
        assertEquals(40.0f, history.reductionPercent ?: 0f, 0.001f)

        val zeroHistory = ProcessingHistory(
            operation = OperationType.COMPRESS,
            originalSize = 0L,
            resultSize = 0L
        )
        assertEquals(0L, zeroHistory.savedBytes)
        assertEquals(0.0f, zeroHistory.reductionPercent ?: 0f, 0.0f)
    }
}
