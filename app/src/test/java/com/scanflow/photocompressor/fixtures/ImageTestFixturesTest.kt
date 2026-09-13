package com.scanflow.photocompressor.fixtures

import com.scanflow.photocompressor.domain.model.ImageFormat
import org.junit.Assert.*
import org.junit.Test

class ImageTestFixturesTest {

    @Test
    fun `all 12 required test image fixtures are defined and distinct`() {
        assertEquals("Must contain exactly 12 required test fixtures", 12, ImageTestFixtures.ALL_FIXTURES.size)
        val uniqueIds = ImageTestFixtures.ALL_FIXTURES.map { it.id }.toSet()
        assertEquals("All fixture IDs must be unique", 12, uniqueIds.size)
    }

    @Test
    fun `small and large JPEG fixtures have valid JPEG configurations`() {
        val small = ImageTestFixtures.SMALL_JPEG
        assertEquals(ImageFormat.JPEG, small.format)
        assertTrue(small.fileSizeBytes in 100_000L..1_000_000L)
        assertFalse(small.hasAlpha)

        val large = ImageTestFixtures.LARGE_JPEG
        assertEquals(ImageFormat.JPEG, large.format)
        assertTrue(large.fileSizeBytes > 10_000_000L)
        assertEquals(4000, large.width)
        assertEquals(3000, large.height)
    }

    @Test
    fun `PNG and transparent PNG fixtures correctly model alpha channels`() {
        val opaque = ImageTestFixtures.OPAQUE_PNG
        assertEquals(ImageFormat.PNG, opaque.format)
        assertFalse(opaque.hasAlpha)

        val transparent = ImageTestFixtures.TRANSPARENT_PNG
        assertEquals(ImageFormat.PNG, transparent.format)
        assertTrue("Transparent PNG must have alpha = true", transparent.hasAlpha)
    }

    @Test
    fun `WebP fixture models WebP format and mimeType`() {
        val webp = ImageTestFixtures.WEBP_IMAGE
        assertEquals(ImageFormat.WEBP, webp.format)
        assertEquals("image/webp", webp.mimeType)
    }

    @Test
    fun `orientation fixtures correctly differentiate portrait, landscape, and square`() {
        val portrait = ImageTestFixtures.PORTRAIT
        assertTrue("Portrait height > width", portrait.height > portrait.width)
        assertTrue(portrait.aspectRatio < 1f)

        val landscape = ImageTestFixtures.LANDSCAPE
        assertTrue("Landscape width > height", landscape.width > landscape.height)
        assertTrue(landscape.aspectRatio > 1f)

        val square = ImageTestFixtures.SQUARE
        assertEquals("Square width == height", square.width, square.height)
        assertEquals(1f, square.aspectRatio, 0.001f)
    }

    @Test
    fun `very large fixture has memory threshold requirements`() {
        val veryLarge = ImageTestFixtures.VERY_LARGE
        assertTrue(veryLarge.isVeryLarge)
        assertTrue("Very large file size > 50 MB", veryLarge.fileSizeBytes > 50_000_000L)
        val analysis = veryLarge.toImageAnalysis()
        assertTrue("Raw memory estimate > 100 MB", analysis.estimatedMemoryBytes > 100_000_000L)
    }

    @Test
    fun `corrupt image fixture models invalid payload`() {
        val corrupt = ImageTestFixtures.CORRUPT_IMAGE
        assertTrue(corrupt.isCorrupt)
        val stream = corrupt.createInputStream()
        val bytes = stream.readBytes()
        assertTrue(bytes.size < 100)
    }

    @Test
    fun `EXIF and GPS EXIF fixtures correctly specify metadata flags`() {
        val exif = ImageTestFixtures.EXIF_IMAGE
        assertTrue(exif.hasExif)
        assertFalse(exif.hasGps)
        assertEquals(6, exif.orientation) // 90 deg rotation tag

        val gpsExif = ImageTestFixtures.GPS_EXIF_IMAGE
        assertTrue(gpsExif.hasExif)
        assertTrue("GPS EXIF must have GPS flag set", gpsExif.hasGps)
    }
}
