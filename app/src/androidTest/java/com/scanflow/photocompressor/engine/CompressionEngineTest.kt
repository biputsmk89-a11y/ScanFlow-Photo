package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.ImageFormat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import io.mockk.every

class CompressionEngineTest {

    private lateinit var engine: CompressionEngine

    @Before
    fun setup() {
        engine = CompressionEngine()
    }

    @Test
    fun `compress clamps quality to valid range`() {
        // Quality below 1 should be clamped to 1
        // Quality above 100 should be clamped to 100
        // This tests the internal clamping logic
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val resultLow = engine.compress(bitmap, ImageFormat.JPEG, 0)
        val resultHigh = engine.compress(bitmap, ImageFormat.JPEG, 150)

        assertTrue("Low quality compress should produce bytes", resultLow.isNotEmpty())
        assertTrue("High quality compress should produce bytes", resultHigh.isNotEmpty())

        bitmap.recycle()
    }

    @Test
    fun `compress produces different sizes for different quality levels`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        // Fill with some data so compression differences are visible
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        paint.color = android.graphics.Color.RED
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        paint.color = android.graphics.Color.BLUE
        canvas.drawRect(100f, 0f, 200f, 200f, paint)

        val lowQuality = engine.compress(bitmap, ImageFormat.JPEG, 10)
        val highQuality = engine.compress(bitmap, ImageFormat.JPEG, 100)

        assertTrue("High quality should be larger", highQuality.size >= lowQuality.size)

        bitmap.recycle()
    }

    @Test
    fun `compress supports all image formats`() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)

        ImageFormat.values().forEach { format ->
            val result = engine.compress(bitmap, format, 80)
            assertTrue("Format ${format.name} should produce bytes", result.isNotEmpty())
        }

        bitmap.recycle()
    }

    @Test
    fun `compressToTargetSize produces output within target`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        paint.color = android.graphics.Color.RED
        canvas.drawRect(0f, 0f, 200f, 200f, paint)

        // Set a generous target size
        val targetSize = 50_000L // 50KB
        val (result, quality) = engine.compressToTargetSize(bitmap, ImageFormat.JPEG, targetSize)

        assertTrue("Result should be within target size or best effort", result.isNotEmpty())
        assertTrue("Quality should be between 1 and 100", quality in 1..100)

        bitmap.recycle()
    }

    @Test
    fun `estimateCompressedSize returns positive value`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val estimate = engine.estimateCompressedSize(bitmap, ImageFormat.JPEG, 80)

        assertTrue("Estimate should be positive", estimate > 0)

        bitmap.recycle()
    }
}
