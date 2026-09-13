package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ResizeEngineTest {

    private lateinit var engine: ResizeEngine

    @Before
    fun setup() {
        engine = ResizeEngine()
    }

    @Test
    fun `calculateAspectRatioDimensions maintains ratio when constrained by width`() {
        val (newW, newH) = engine.calculateAspectRatioDimensions(1000, 500, 400, 400)
        assertEquals(400, newW)
        assertEquals(200, newH) // Maintains 2:1 ratio
    }

    @Test
    fun `calculateAspectRatioDimensions maintains ratio when constrained by height`() {
        val (newW, newH) = engine.calculateAspectRatioDimensions(500, 1000, 400, 400)
        assertEquals(200, newW)
        assertEquals(400, newH) // Maintains 1:2 ratio
    }

    @Test
    fun `calculateAspectRatioDimensions handles square`() {
        val (newW, newH) = engine.calculateAspectRatioDimensions(1000, 1000, 500, 500)
        assertEquals(500, newW)
        assertEquals(500, newH)
    }

    @Test
    fun `calculateDimensionsForAspectRatio produces correct 16_9`() {
        val (w, h) = engine.calculateDimensionsForAspectRatio(1000, 1000, 16, 9)
        assertEquals(1000, w)
        assertEquals(562, h) // 1000 / (16/9) ≈ 562
    }

    @Test
    fun `resize returns same bitmap when already smaller`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = engine.resize(bitmap, 200, 200, true)
        assertSame("Should return same bitmap when already smaller", bitmap, result)
        bitmap.recycle()
    }

    @Test
    fun `resize downscales correctly`() {
        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val result = engine.resize(bitmap, 200, 200, maintainAspectRatio = true)

        assertEquals(200, result.width)
        assertEquals(100, result.height) // Maintains 2:1 ratio

        bitmap.recycle()
        if (result !== bitmap) result.recycle()
    }

    @Test
    fun `resizeByPercentage works correctly`() {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val result = engine.resizeByPercentage(bitmap, 0.5f)

        assertEquals(100, result.width)
        assertEquals(50, result.height)

        bitmap.recycle()
        result.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resizeByPercentage throws for zero percentage`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        try {
            engine.resizeByPercentage(bitmap, 0f)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun `resize returns original when target is zero`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = engine.resize(bitmap, 0, 0)
        assertSame(bitmap, result)
        bitmap.recycle()
    }
}
