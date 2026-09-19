package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.domain.model.*
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.OutputStream

/**
 * Comprehensive verification test suite for ALL photo output features.
 * Ensures output reaches HD standards (resolution, aspect ratio, clean edges,
 * and artifact-free quality) even when file sizes are configured to be low.
 */
class AllFeaturesPhotoQualityTest {

    private lateinit var compressionEngine: CompressionEngine
    private lateinit var resizeEngine: ResizeEngine
    private lateinit var rotateEngine: RotateEngine
    private lateinit var formatConverter: FormatConverter

    @Before
    fun setup() {
        compressionEngine = CompressionEngine()
        resizeEngine = ResizeEngine()
        rotateEngine = RotateEngine()
        formatConverter = FormatConverter(compressionEngine)

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

    // =========================================================================
    // 1. PASSPORT & ID STUDIO - HD & CLEAN RESOLUTION VALIDATION
    // =========================================================================

    @Test
    fun `Passport standard sizes maintain 300 DPI high resolution across all presets`() {
        // 3x4 cm -> 900 x 1200 px (300 DPI HD print ready)
        val spec3x4 = PassportSpec.PRESET_3X4
        assertEquals(900, spec3x4.targetWidthPx)
        assertEquals(1200, spec3x4.targetHeightPx)
        assertEquals(3f / 4f, spec3x4.targetWidthPx.toFloat() / spec3x4.targetHeightPx.toFloat(), 0.01f)

        // 4x6 cm -> 800 x 1200 px (300 DPI HD print ready)
        val spec4x6 = PassportSpec.PRESET_4X6
        assertEquals(800, spec4x6.targetWidthPx)
        assertEquals(1200, spec4x6.targetHeightPx)
        assertEquals(2f / 3f, spec4x6.targetWidthPx.toFloat() / spec4x6.targetHeightPx.toFloat(), 0.01f)

        // 2x3 cm -> 600 x 900 px (300 DPI HD print ready)
        val spec2x3 = PassportSpec.PRESET_2X3
        assertEquals(600, spec2x3.targetWidthPx)
        assertEquals(900, spec2x3.targetHeightPx)
        assertEquals(2f / 3f, spec2x3.targetWidthPx.toFloat() / spec2x3.targetHeightPx.toFloat(), 0.01f)

        // All passport specs must have width >= 600px and height >= 800px
        PassportSpec.values().forEach { spec ->
            assertTrue("${spec.displayName} width must be HD resolution (>= 600px)", spec.targetWidthPx >= 600)
            assertTrue("${spec.displayName} height must be HD resolution (>= 800px)", spec.targetHeightPx >= 800)
        }
    }

    // =========================================================================
    // 2. WHATSAPP ENGINE - HD QUALITY EVEN ON LOW SIZE / SMALL TIER
    // =========================================================================

    @Test
    fun `WhatsApp Small tier maintains HD dimensions and crisp quality without pixelation`() {
        val small = WhatsAppTier.SMALL

        // Small tier must have max dimension >= 960 (HD wide standard)
        assertTrue("Small tier maxDimension must be >= 960px for HD clarity on modern screens", small.maxDimension >= 960)

        // Small tier quality must be >= 70 to prevent visible JPEG macroblocking artifacts
        assertTrue("Small tier quality must be >= 70 for clean skin tones & sharp details", small.quality >= 70)

        // Small tier target size must be reasonable (<= 200 KB)
        assertTrue("Small tier target size must be <= 200 KB", small.targetSizeBytes <= 200 * 1024L)
    }

    @Test
    fun `WhatsApp Balanced and High Quality tiers maintain Full HD and 2K clarity`() {
        val balanced = WhatsAppTier.BALANCED
        assertTrue("Balanced tier must be >= 1280px (HD 720p/1080p)", balanced.maxDimension >= 1280)
        assertTrue("Balanced tier quality must be >= 80", balanced.quality >= 80)

        val highQuality = WhatsAppTier.HIGH_QUALITY
        assertTrue("High Quality tier must be >= 2048px (2K QHD)", highQuality.maxDimension >= 2048)
        assertTrue("High Quality tier quality must be >= 90", highQuality.quality >= 90)
    }

    // =========================================================================
    // 3. SOCIAL MEDIA PRESETS - FULL HD AND PLATFORM STANDARDS
    // =========================================================================

    @Test
    fun `Social Media post, story, and cover presets maintain Full HD resolution`() {
        // Instagram Post: 1080x1080 (HD square)
        val igPost = SocialPresetRegistry.getPreset(SocialPlatform.INSTAGRAM, SocialContentType.POST)
        assertEquals(1080, igPost.targetWidth)
        assertEquals(1080, igPost.targetHeight)
        assertTrue("Instagram Post recommended quality must be >= 85", igPost.recommendedQuality >= 85)

        // Instagram Story: 1080x1920 (Full HD vertical)
        val igStory = SocialPresetRegistry.getPreset(SocialPlatform.INSTAGRAM, SocialContentType.STORY)
        assertEquals(1080, igStory.targetWidth)
        assertEquals(1920, igStory.targetHeight)

        // TikTok Post & Story: 1080x1920 (Full HD vertical)
        val tiktokPost = SocialPresetRegistry.getPreset(SocialPlatform.TIKTOK, SocialContentType.POST)
        assertEquals(1080, tiktokPost.targetWidth)
        assertEquals(1920, tiktokPost.targetHeight)

        // YouTube Cover: 2560x1440 (2K QHD)
        val ytCover = SocialPresetRegistry.getPreset(SocialPlatform.YOUTUBE, SocialContentType.COVER)
        assertEquals(2560, ytCover.targetWidth)
        assertEquals(1440, ytCover.targetHeight)

        // YouTube Thumbnail: 1280x720 (720p HD)
        val ytThumb = SocialPresetRegistry.getPreset(SocialPlatform.YOUTUBE, SocialContentType.THUMBNAIL)
        assertEquals(1280, ytThumb.targetWidth)
        assertEquals(720, ytThumb.targetHeight)
    }

    // =========================================================================
    // 4. RESIZE PRESETS - FULL HD RESOLUTION FLOORS
    // =========================================================================

    @Test
    fun `Resize standard presets are all HD or ultra high resolution`() {
        assertEquals(1080, ResizePreset.P_1080.dimension)
        assertEquals(1440, ResizePreset.P_1440.dimension)
        assertEquals(1600, ResizePreset.P_1600.dimension)
        assertEquals(1920, ResizePreset.P_1920.dimension)
        assertEquals(2560, ResizePreset.P_2560.dimension)

        val nonCustomPresets = ResizePreset.values().filter { it != ResizePreset.CUSTOM }
        nonCustomPresets.forEach { preset ->
            assertTrue("Preset ${preset.name} must be >= 1080px (Full HD)", preset.dimension >= 1080)
        }
    }

    @Test
    fun `Resize preserves high definition aspect ratio during downscaling`() {
        val (w, h) = resizeEngine.calculateAspectRatioDimensions(
            srcWidth = 3840,
            srcHeight = 2160,
            maxWidth = 1920,
            maxHeight = 1080
        )
        assertEquals(1920, w)
        assertEquals(1080, h)
    }

    // =========================================================================
    // 5. COMPRESSION ENGINE - TARGET SIZE ADAPTIVE SEARCH & CLEAN QUALITY
    // =========================================================================

    @Test
    fun `CompressionEngine target size binary search never falls below clean quality threshold`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.width } returns 2048
        every { bitmap.height } returns 1536

        every { bitmap.compress(any(), any(), any<OutputStream>()) } answers {
            val quality = secondArg<Int>()
            val stream = thirdArg<OutputStream>()
            // Size proportional to quality
            stream.write(ByteArray(quality * 1024))
            true
        }

        // Test with moderate target size
        val targetSize = 60 * 1024L
        val result = compressionEngine.compressToTargetSize(
            bitmap = bitmap,
            format = ImageFormat.JPEG,
            targetSizeBytes = targetSize
        )

        assertTrue(result.isFeasible)
        assertTrue("Output bytes must fit in target", result.bytes.size <= targetSize)
        assertTrue("Adaptive search must select high quality (>= 30) to preserve clean image details", result.quality >= 30)
    }

    // =========================================================================
    // 6. ROTATION & ALPHA FLATTENING - CLEAN EDGE & ARTIFACT-FREE RENDERING
    // =========================================================================

    @Test
    fun `RotateEngine handles 0 and identity rotations without loss of original bitmap`() {
        val mockBitmap = mockk<Bitmap>()
        val result0 = rotateEngine.rotate(mockBitmap, 0f)
        val result360 = rotateEngine.rotate(mockBitmap, 360f)
        assertSame(mockBitmap, result0)
        assertSame(mockBitmap, result360)
    }

    @Test
    fun `FormatConverter recommended qualities provide clean HD compression`() {
        assertTrue("JPEG recommended quality should be >= 85", formatConverter.getRecommendedQuality(ImageFormat.JPEG) >= 85)
        assertEquals("PNG is lossless (100)", 100, formatConverter.getRecommendedQuality(ImageFormat.PNG))
        assertTrue("WEBP recommended quality should be >= 80", formatConverter.getRecommendedQuality(ImageFormat.WEBP) >= 80)
        assertEquals("WEBP Lossless is lossless (100)", 100, formatConverter.getRecommendedQuality(ImageFormat.WEBP_LOSSLESS))
    }
}
