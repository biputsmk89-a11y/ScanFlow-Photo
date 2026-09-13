package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.CropRegion
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CropEngineTest {

    private lateinit var cropEngine: CropEngine

    @Before
    fun setup() {
        cropEngine = CropEngine()
        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(any<Bitmap>(), any<Int>(), any<Int>(), any<Int>(), any<Int>()) } answers {
            val w = arg<Int>(3)
            val h = arg<Int>(4)
            val cropped = mockk<Bitmap>(relaxed = true)
            every { cropped.width } returns w
            every { cropped.height } returns h
            cropped
        }
    }

    @After
    fun teardown() {
        unmockkStatic(Bitmap::class)
    }

    @Test
    fun `cropToAspectRatio crops to 1-1 square correctly`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 4000
        every { bitmap.height } returns 3000

        val result = cropEngine.cropToAspectRatio(bitmap, 1, 1)

        assertEquals(3000, result.width)
        assertEquals(3000, result.height)
    }

    @Test
    fun `cropToAspectRatio crops to 4-5 portrait correctly`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 4000
        every { bitmap.height } returns 3000

        val result = cropEngine.cropToAspectRatio(bitmap, 4, 5)

        // For height 3000, width = 3000 * 4 / 5 = 2400
        assertEquals(2400, result.width)
        assertEquals(3000, result.height)
    }

    @Test
    fun `cropToAspectRatio crops to 16-9 widescreen correctly`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 4000
        every { bitmap.height } returns 3000

        val result = cropEngine.cropToAspectRatio(bitmap, 16, 9)

        // 4000 is wider than 3000 * 16 / 9 (5333), so constrained by width:
        // height = 4000 * 9 / 16 = 2250
        assertEquals(4000, result.width)
        assertEquals(2250, result.height)
    }

    @Test
    fun `cropToAspectRatio crops to 9-16 vertical correctly`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 4000
        every { bitmap.height } returns 3000

        val result = cropEngine.cropToAspectRatio(bitmap, 9, 16)

        // Constrained by height 3000: width = 3000 * 9 / 16 = 1687
        assertEquals(1687, result.width)
        assertEquals(3000, result.height)
    }

    @Test
    fun `mapRawCropRegionToOriented and mapOrientedCropRegionToRaw are bidirectional for all EXIF angles`() {
        val rawWidth = 4000
        val rawHeight = 3000
        val region = CropRegion(x = 100, y = 200, width = 800, height = 600)

        // 0 degrees
        val oriented0 = cropEngine.mapRawCropRegionToOriented(region, rawWidth, rawHeight, 0)
        assertEquals(region, oriented0)

        // 90 degrees
        val oriented90 = cropEngine.mapRawCropRegionToOriented(region, rawWidth, rawHeight, 90)
        // Oriented dimensions: width = 3000, height = 4000
        val roundtrip90 = cropEngine.mapOrientedCropRegionToRaw(oriented90, rawHeight, rawWidth, 90)
        assertEquals("90 deg roundtrip should match original region", region, roundtrip90)

        // 180 degrees
        val oriented180 = cropEngine.mapRawCropRegionToOriented(region, rawWidth, rawHeight, 180)
        val roundtrip180 = cropEngine.mapOrientedCropRegionToRaw(oriented180, rawWidth, rawHeight, 180)
        assertEquals("180 deg roundtrip should match original region", region, roundtrip180)

        // 270 degrees
        val oriented270 = cropEngine.mapRawCropRegionToOriented(region, rawWidth, rawHeight, 270)
        val roundtrip270 = cropEngine.mapOrientedCropRegionToRaw(oriented270, rawHeight, rawWidth, 270)
        assertEquals("270 deg roundtrip should match original region", region, roundtrip270)
    }

    @Test
    fun `calculateAspectCropRegion scales with zoom and pan`() {
        val imageWidth = 4000
        val imageHeight = 3000

        // 1.0x zoom, centered 1:1
        val region1x = cropEngine.calculateAspectCropRegion(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            targetRatioX = 1,
            targetRatioY = 1,
            zoomScale = 1f
        )
        assertEquals(3000, region1x.width)
        assertEquals(3000, region1x.height)
        assertEquals(500, region1x.x) // (4000 - 3000) / 2
        assertEquals(0, region1x.y)

        // 2.0x zoom halves crop dimensions
        val region2x = cropEngine.calculateAspectCropRegion(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            targetRatioX = 1,
            targetRatioY = 1,
            zoomScale = 2f
        )
        assertEquals(1500, region2x.width)
        assertEquals(1500, region2x.height)
        assertEquals(1250, region2x.x) // (4000 - 1500) / 2
        assertEquals(750, region2x.y)  // (3000 - 1500) / 2
    }

    @Test
    fun `crop correctly scales downsampled bitmap coordinates`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        // Decoded at half resolution (2000x1500) from original 4000x3000
        every { bitmap.width } returns 2000
        every { bitmap.height } returns 1500

        val originalRegion = CropRegion(x = 1000, y = 500, width = 2000, height = 1000)
        val result = cropEngine.crop(bitmap, originalRegion, originalWidth = 4000, originalHeight = 3000)

        // Scaled region: (500, 250, 1000, 500)
        assertEquals(1000, result.width)
        assertEquals(500, result.height)
    }
}
