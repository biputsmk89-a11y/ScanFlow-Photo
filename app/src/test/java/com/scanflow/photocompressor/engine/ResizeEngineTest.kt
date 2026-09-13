package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.ResizePreset
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ResizeEngineTest {

    private lateinit var resizeEngine: ResizeEngine

    @Before
    fun setup() {
        resizeEngine = ResizeEngine()
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
    fun `calculateAspectRatioDimensions computes correct dimensions for landscape`() {
        val (w, h) = resizeEngine.calculateAspectRatioDimensions(
            srcWidth = 4000,
            srcHeight = 2000,
            maxWidth = 1000,
            maxHeight = 1000
        )
        assertEquals(1000, w)
        assertEquals(500, h)
    }

    @Test
    fun `calculateAspectRatioDimensions computes correct dimensions for portrait`() {
        val (w, h) = resizeEngine.calculateAspectRatioDimensions(
            srcWidth = 2000,
            srcHeight = 4000,
            maxWidth = 1000,
            maxHeight = 1000
        )
        assertEquals(500, w)
        assertEquals(1000, h)
    }

    @Test
    fun `resizeByPercentage calculates correct scaled dimensions`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 2000
        every { bitmap.height } returns 1000

        val result = resizeEngine.resizeByPercentage(bitmap, 0.5f)

        assertEquals(1000, result.width)
        assertEquals(500, result.height)
    }

    @Test
    fun `resizeByMaxDimension scales landscape image proportionally to max dimension`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 4000
        every { bitmap.height } returns 3000

        val result = resizeEngine.resizeByMaxDimension(bitmap, 1080, maintainAspectRatio = true)

        assertEquals(1080, result.width)
        assertEquals(810, result.height)
    }

    @Test
    fun `resizeByMaxDimension scales portrait image proportionally to max dimension`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 3000
        every { bitmap.height } returns 4000

        val result = resizeEngine.resizeByMaxDimension(bitmap, 1080, maintainAspectRatio = true)

        assertEquals(810, result.width)
        assertEquals(1080, result.height)
    }

    @Test
    fun `resizeByMaxDimension returns same bitmap if already smaller than max dimension`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 800
        every { bitmap.height } returns 600

        val result = resizeEngine.resizeByMaxDimension(bitmap, 1080)

        assertSame(bitmap, result)
    }

    @Test
    fun `resizeByPreset applies correct preset dimensions`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 3840
        every { bitmap.height } returns 2160

        // Test P_1080
        val r1080 = resizeEngine.resizeByPreset(bitmap, ResizePreset.P_1080)
        assertEquals(1080, r1080.width)
        assertEquals(607, r1080.height)

        // Test P_1440
        val r1440 = resizeEngine.resizeByPreset(bitmap, ResizePreset.P_1440)
        assertEquals(1440, r1440.width)
        assertEquals(810, r1440.height)

        // Test P_1600
        val r1600 = resizeEngine.resizeByPreset(bitmap, ResizePreset.P_1600)
        assertEquals(1600, r1600.width)
        assertEquals(900, r1600.height)

        // Test P_1920
        val r1920 = resizeEngine.resizeByPreset(bitmap, ResizePreset.P_1920)
        assertEquals(1920, r1920.width)
        assertEquals(1080, r1920.height)

        // Test P_2560
        val r2560 = resizeEngine.resizeByPreset(bitmap, ResizePreset.P_2560)
        assertEquals(2560, r2560.width)
        assertEquals(1440, r2560.height)

        // Test CUSTOM returns original bitmap unchanged
        val rCustom = resizeEngine.resizeByPreset(bitmap, ResizePreset.CUSTOM)
        assertSame(bitmap, rCustom)
    }
}
