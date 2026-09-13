package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scanflow.photocompressor.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePipelineTest {

    private lateinit var pipeline: ImagePipeline
    private lateinit var compressionEngine: CompressionEngine
    private lateinit var resizeEngine: ResizeEngine
    private lateinit var cropEngine: CropEngine
    private lateinit var rotateEngine: RotateEngine
    private lateinit var watermarkEngine: WatermarkEngine
    private lateinit var formatConverter: FormatConverter

    @Before
    fun setup() {
        compressionEngine = CompressionEngine()
        resizeEngine = ResizeEngine()
        cropEngine = CropEngine()
        rotateEngine = RotateEngine()
        watermarkEngine = WatermarkEngine()
        formatConverter = FormatConverter(compressionEngine)
        pipeline = ImagePipeline(
            compressionEngine, resizeEngine, cropEngine,
            rotateEngine, watermarkEngine, formatConverter
        )
    }

    @Test
    fun executeResizeThenRotate() {
        val bitmap = createTestBitmap(400, 200)

        val operations = listOf(
            ImageOperation.Resize(200, 100, maintainAspectRatio = true),
            ImageOperation.Rotate(90f)
        )

        val result = pipeline.execute(bitmap, operations)

        // After resize to 200x100 then rotate 90°, dimensions should swap
        assertTrue("Width should be around 100", result.width <= 100)
        assertTrue("Height should be around 200", result.height >= 100)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun executeCropThenWatermark() {
        val bitmap = createTestBitmap(400, 400)

        val operations = listOf(
            ImageOperation.Crop(CropRegion(50, 50, 200, 200)),
            ImageOperation.Watermark(WatermarkConfig(text = "Test", opacity = 0.5f))
        )

        val result = pipeline.execute(bitmap, operations)

        assertEquals("Width should be 200 after crop", 200, result.width)
        assertEquals("Height should be 200 after crop", 200, result.height)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun executeEmptyOperationsList() {
        val bitmap = createTestBitmap(200, 200)

        val result = pipeline.execute(bitmap, emptyList())

        assertSame("Should return same bitmap for empty ops", bitmap, result)
        bitmap.recycle()
    }

    @Test
    fun executeFlip() {
        val bitmap = createTestBitmap(200, 100)

        val operations = listOf(
            ImageOperation.Flip(horizontal = true, vertical = false)
        )

        val result = pipeline.execute(bitmap, operations)

        assertEquals(200, result.width)
        assertEquals(100, result.height)

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
