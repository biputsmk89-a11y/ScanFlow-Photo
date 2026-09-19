package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.scanflow.photocompressor.domain.model.ImageFormat
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FormatConverterTest {

    private lateinit var compressionEngine: CompressionEngine
    private lateinit var formatConverter: FormatConverter

    @Before
    fun setup() {
        compressionEngine = mockk(relaxed = true)
        formatConverter = FormatConverter(compressionEngine)

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } answers {
            val w = firstArg<Int>()
            val h = secondArg<Int>()
            val bmp = mockk<Bitmap>(relaxed = true)
            every { bmp.width } returns w
            every { bmp.height } returns h
            every { bmp.hasAlpha() } returns false
            bmp
        }
    }

    @After
    fun teardown() {
        unmockkStatic(Bitmap::class)
    }

    @Test
    fun `flattenAlpha flattens alpha-bearing bitmap onto background color`() {
        val alphaBitmap = mockk<Bitmap>(relaxed = true)
        every { alphaBitmap.width } returns 800
        every { alphaBitmap.height } returns 600
        every { alphaBitmap.hasAlpha() } returns true

        mockkConstructor(Canvas::class)
        every { anyConstructed<Canvas>().drawColor(any<Int>()) } just Runs
        every { anyConstructed<Canvas>().drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any()) } just Runs

        val result = formatConverter.flattenAlpha(alphaBitmap, Color.WHITE)

        assertNotNull(result)
        assertEquals(800, result.width)
        assertEquals(600, result.height)
        verify { anyConstructed<Canvas>().drawColor(Color.WHITE) }
        verify { anyConstructed<Canvas>().drawBitmap(alphaBitmap, 0f, 0f, any()) }
    }

    @Test
    fun `flattenAlpha returns same bitmap if it has no alpha`() {
        val opaqueBitmap = mockk<Bitmap>(relaxed = true)
        every { opaqueBitmap.hasAlpha() } returns false

        val result = formatConverter.flattenAlpha(opaqueBitmap, Color.WHITE)

        assertSame(opaqueBitmap, result)
    }

    @Test
    fun `convert flattens alpha before passing to compressionEngine when target is JPEG`() {
        val alphaBitmap = mockk<Bitmap>(relaxed = true)
        every { alphaBitmap.width } returns 500
        every { alphaBitmap.height } returns 500
        every { alphaBitmap.hasAlpha() } returns true

        mockkConstructor(Canvas::class)
        every { anyConstructed<Canvas>().drawColor(any<Int>()) } just Runs
        every { anyConstructed<Canvas>().drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any()) } just Runs

        val expectedBytes = byteArrayOf(1, 2, 3)
        every { compressionEngine.compress(any(), ImageFormat.JPEG, 85) } returns expectedBytes

        val bytes = formatConverter.convert(
            bitmap = alphaBitmap,
            targetFormat = ImageFormat.JPEG,
            quality = 85,
            backgroundColor = Color.BLACK
        )

        assertArrayEquals(expectedBytes, bytes)
        verify { anyConstructed<Canvas>().drawColor(Color.BLACK) }
        verify { compressionEngine.compress(any(), ImageFormat.JPEG, 85) }
    }

    @Test
    fun `convert does not flatten alpha when target is PNG or WEBP`() {
        val alphaBitmap = mockk<Bitmap>(relaxed = true)
        every { alphaBitmap.hasAlpha() } returns true

        val expectedBytes = byteArrayOf(4, 5, 6)
        every { compressionEngine.compress(alphaBitmap, ImageFormat.PNG, 100) } returns expectedBytes

        val bytes = formatConverter.convert(
            bitmap = alphaBitmap,
            targetFormat = ImageFormat.PNG,
            quality = 100
        )

        assertArrayEquals(expectedBytes, bytes)
        verify(exactly = 1) { compressionEngine.compress(alphaBitmap, ImageFormat.PNG, 100) }
    }

    @Test
    fun `ImageFormat metadata strictly matches format rules`() {
        // JPEG: photography, small output, no transparency
        assertFalse(ImageFormat.JPEG.supportsAlpha)
        assertTrue(ImageFormat.JPEG.bestFor.contains("Photography", ignoreCase = true))
        assertTrue(ImageFormat.JPEG.bestFor.contains("No transparency", ignoreCase = true))

        // PNG: transparency, graphics, lossless
        assertTrue(ImageFormat.PNG.supportsAlpha)
        assertTrue(ImageFormat.PNG.bestFor.contains("Transparency", ignoreCase = true))
        assertTrue(ImageFormat.PNG.bestFor.contains("Lossless", ignoreCase = true))

        // WEBP: modern compressed format, web delivery
        assertTrue(ImageFormat.WEBP.supportsAlpha)
        assertTrue(ImageFormat.WEBP.bestFor.contains("web", ignoreCase = true))
    }
}
