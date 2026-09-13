package com.scanflow.photocompressor.fixtures

import android.net.Uri
import com.scanflow.photocompressor.domain.model.ImageAnalysis
import com.scanflow.photocompressor.domain.model.ImageFormat
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Specification 81: Image Test Fixtures.
 *
 * Provides standardized, reproducible test cases covering:
 * 1. Small JPEG
 * 2. Large JPEG
 * 3. PNG
 * 4. Transparent PNG
 * 5. WebP
 * 6. Portrait
 * 7. Landscape
 * 8. Square
 * 9. Very Large
 * 10. Corrupt Image
 * 11. EXIF Image
 * 12. GPS EXIF Image
 */
object ImageTestFixtures {

    data class Fixture(
        val id: String,
        val fileName: String,
        val format: ImageFormat?,
        val mimeType: String,
        val width: Int,
        val height: Int,
        val fileSizeBytes: Long,
        val hasAlpha: Boolean,
        val orientation: Int = 0,
        val hasExif: Boolean = false,
        val hasGps: Boolean = false,
        val isCorrupt: Boolean = false,
        val isVeryLarge: Boolean = false
    ) {
        val aspectRatio: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 1f

        fun toImageAnalysis(): ImageAnalysis {
            return ImageAnalysis(
                width = width,
                height = height,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                orientation = orientation,
                hasAlpha = hasAlpha,
                estimatedMemoryBytes = width.toLong() * height.toLong() * 4L,
                exifAvailable = hasExif
            )
        }

        fun mockUri(): Uri {
            val uri = mockk<Uri>(relaxed = true)
            every { uri.toString() } returns "content://media/external/images/media/$id"
            every { uri.lastPathSegment } returns fileName
            return uri
        }

        fun createInputStream(): InputStream {
            return if (isCorrupt) {
                // Corrupt payload (truncated, non-image binary garbage)
                ByteArrayInputStream(byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55))
            } else {
                // Synthetic image header placeholder
                val magicHeader = when (format) {
                    ImageFormat.JPEG -> byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
                    ImageFormat.PNG -> byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
                    ImageFormat.WEBP, ImageFormat.WEBP_LOSSLESS -> "RIFF....WEBP".toByteArray()
                    else -> byteArrayOf(0x00)
                }
                ByteArrayInputStream(magicHeader + ByteArray(1024))
            }
        }
    }

    // 1. Small JPEG (~500 KB, standard mobile photo)
    val SMALL_JPEG = Fixture(
        id = "small_jpg_1",
        fileName = "sample_small.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 800,
        height = 600,
        fileSizeBytes = 512_000L,
        hasAlpha = false
    )

    // 2. Large JPEG (~15 MB, high-res camera shot)
    val LARGE_JPEG = Fixture(
        id = "large_jpg_2",
        fileName = "sample_large.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 4000,
        height = 3000,
        fileSizeBytes = 15_728_640L,
        hasAlpha = false
    )

    // 3. PNG (Standard opaque graphic)
    val OPAQUE_PNG = Fixture(
        id = "opaque_png_3",
        fileName = "sample_graphic.png",
        format = ImageFormat.PNG,
        mimeType = "image/png",
        width = 1200,
        height = 800,
        fileSizeBytes = 1_843_200L,
        hasAlpha = false
    )

    // 4. Transparent PNG (contains alpha channel)
    val TRANSPARENT_PNG = Fixture(
        id = "trans_png_4",
        fileName = "sample_transparent.png",
        format = ImageFormat.PNG,
        mimeType = "image/png",
        width = 1000,
        height = 1000,
        fileSizeBytes = 1_200_000L,
        hasAlpha = true
    )

    // 5. WebP (Modern efficient web format)
    val WEBP_IMAGE = Fixture(
        id = "webp_5",
        fileName = "sample_photo.webp",
        format = ImageFormat.WEBP,
        mimeType = "image/webp",
        width = 1920,
        height = 1080,
        fileSizeBytes = 650_000L,
        hasAlpha = true
    )

    // 6. Portrait (Vertical 9:16 orientation)
    val PORTRAIT = Fixture(
        id = "portrait_6",
        fileName = "sample_portrait.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 1080,
        height = 1920,
        fileSizeBytes = 2_100_000L,
        hasAlpha = false
    )

    // 7. Landscape (Horizontal 16:9 orientation)
    val LANDSCAPE = Fixture(
        id = "landscape_7",
        fileName = "sample_landscape.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 1920,
        height = 1080,
        fileSizeBytes = 2_100_000L,
        hasAlpha = false
    )

    // 8. Square (1:1 Aspect Ratio)
    val SQUARE = Fixture(
        id = "square_8",
        fileName = "sample_square.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 1080,
        height = 1080,
        fileSizeBytes = 1_500_000L,
        hasAlpha = false
    )

    // 9. Very Large (> 50 MB, 10000x8000, testing memory safety & adaptive downscale)
    val VERY_LARGE = Fixture(
        id = "very_large_9",
        fileName = "sample_very_large.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 10000,
        height = 8000,
        fileSizeBytes = 62_914_560L,
        hasAlpha = false,
        isVeryLarge = true
    )

    // 10. Corrupt Image (Invalid byte payload / non-decodable)
    val CORRUPT_IMAGE = Fixture(
        id = "corrupt_10",
        fileName = "sample_corrupt.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 0,
        height = 0,
        fileSizeBytes = 64L,
        hasAlpha = false,
        isCorrupt = true
    )

    // 11. EXIF Image (Contains camera, lens, date, and 90-degree orientation)
    val EXIF_IMAGE = Fixture(
        id = "exif_11",
        fileName = "sample_exif.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 3840,
        height = 2160,
        fileSizeBytes = 4_500_000L,
        hasAlpha = false,
        orientation = 6, // 90 CW
        hasExif = true,
        hasGps = false
    )

    // 12. GPS EXIF Image (Contains sensitive geolocation metadata)
    val GPS_EXIF_IMAGE = Fixture(
        id = "gps_exif_12",
        fileName = "sample_gps.jpg",
        format = ImageFormat.JPEG,
        mimeType = "image/jpeg",
        width = 4000,
        height = 3000,
        fileSizeBytes = 5_200_000L,
        hasAlpha = false,
        hasExif = true,
        hasGps = true
    )

    val ALL_FIXTURES: List<Fixture> = listOf(
        SMALL_JPEG,
        LARGE_JPEG,
        OPAQUE_PNG,
        TRANSPARENT_PNG,
        WEBP_IMAGE,
        PORTRAIT,
        LANDSCAPE,
        SQUARE,
        VERY_LARGE,
        CORRUPT_IMAGE,
        EXIF_IMAGE,
        GPS_EXIF_IMAGE
    )
}
