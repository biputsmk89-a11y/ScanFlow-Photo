package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.domain.model.CompressionPreset
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ImageInfo
import com.scanflow.photocompressor.domain.repository.PresetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PresetEngineTest {

    private lateinit var presetRepository: PresetRepository
    private lateinit var presetEngine: PresetEngine

    @Before
    fun setup() {
        presetRepository = mockk(relaxed = true)
        presetEngine = PresetEngineImpl(presetRepository)
    }

    @Test
    fun `getSystemPresets returns the 7 required system presets`() {
        val presets = presetEngine.getSystemPresets()
        assertEquals(7, presets.size)

        val names = presets.map { it.name }
        assertTrue("Contains WhatsApp", names.contains("WhatsApp"))
        assertTrue("Contains Email", names.contains("Email"))
        assertTrue("Contains Website", names.contains("Website"))
        assertTrue("Contains School Upload", names.contains("School Upload"))
        assertTrue("Contains Government Upload", names.contains("Government Upload"))
        assertTrue("Contains Social Media", names.contains("Social Media"))
        assertTrue("Contains Custom", names.contains("Custom"))
    }

    @Test
    fun `system presets have valid model parameters`() {
        val whatsapp = CompressionPreset.WHATSAPP
        assertEquals("whatsapp", whatsapp.id)
        assertEquals(70, whatsapp.quality)
        assertEquals(1600, whatsapp.maxDimension)
        assertEquals(ImageFormat.JPEG, whatsapp.format)
        assertTrue(whatsapp.removeGps)

        val email = CompressionPreset.EMAIL
        assertEquals("email", email.id)
        assertEquals(75, email.quality)
        assertEquals(1280, email.maxDimension)
        assertEquals(1_000_000L, email.targetBytes)
        assertTrue(email.removeGps)

        val website = CompressionPreset.WEBSITE
        assertEquals("website", website.id)
        assertEquals(80, website.quality)
        assertEquals(1920, website.maxDimension)
        assertEquals(ImageFormat.WEBP, website.format)
        assertTrue(website.removeGps)

        val school = CompressionPreset.SCHOOL_UPLOAD
        assertEquals("school_upload", school.id)
        assertEquals(80, school.quality)
        assertEquals(1600, school.maxDimension)
        assertEquals(500_000L, school.targetBytes)
        assertTrue(school.removeGps)

        val gov = CompressionPreset.GOVERNMENT_UPLOAD
        assertEquals("government_upload", gov.id)
        assertEquals(85, gov.quality)
        assertEquals(800, gov.maxDimension)
        assertEquals(200_000L, gov.targetBytes)
        assertTrue(gov.removeGps)

        val social = CompressionPreset.SOCIAL_MEDIA
        assertEquals("social_media", social.id)
        assertEquals(85, social.quality)
        assertEquals(1080, social.maxDimension)
        assertTrue(social.removeGps)

        val custom = CompressionPreset.CUSTOM
        assertEquals("custom", custom.id)
        assertNull(custom.quality)
        assertNull(custom.maxDimension)
        assertNull(custom.targetBytes)
        assertNull(custom.format)
        assertFalse(custom.removeGps)
    }

    @Test
    fun `recommendPreset selects expected preset for specific use cases`() {
        val dummyInfo = ImageInfo(
            uri = mockk(relaxed = true),
            fileName = "photo.jpg",
            fileSize = 5_000_000L,
            width = 4000,
            height = 3000,
            format = ImageFormat.JPEG,
            mimeType = "image/jpeg"
        )

        assertEquals(CompressionPreset.WHATSAPP, presetEngine.recommendPreset(dummyInfo, "whatsapp message"))
        assertEquals(CompressionPreset.EMAIL, presetEngine.recommendPreset(dummyInfo, "email attachment"))
        assertEquals(CompressionPreset.WEBSITE, presetEngine.recommendPreset(dummyInfo, "website banner"))
        assertEquals(CompressionPreset.SCHOOL_UPLOAD, presetEngine.recommendPreset(dummyInfo, "school upload tugas"))
        assertEquals(CompressionPreset.GOVERNMENT_UPLOAD, presetEngine.recommendPreset(dummyInfo, "government upload cpns"))
        assertEquals(CompressionPreset.SOCIAL_MEDIA, presetEngine.recommendPreset(dummyInfo, "social media instagram"))
        assertEquals(CompressionPreset.CUSTOM, presetEngine.recommendPreset(dummyInfo, "custom mode"))
    }

    @Test
    fun `calculateTargetDimensions scales proportionally preserving aspect ratio`() {
        val preset = CompressionPreset(
            id = "test_preset",
            name = "Test",
            quality = 80,
            maxDimension = 1600,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = true
        )

        // Landscape: 4000 x 3000 -> 1600 x 1200
        val (targetW, targetH) = presetEngine.calculateTargetDimensions(4000, 3000, preset)
        assertEquals(1600, targetW)
        assertEquals(1200, targetH)

        // Portrait: 3000 x 4000 -> 1200 x 1600
        val (portW, portH) = presetEngine.calculateTargetDimensions(3000, 4000, preset)
        assertEquals(1200, portW)
        assertEquals(1600, portH)
    }

    @Test
    fun `calculateTargetDimensions keeps original when image is smaller than maxDimension`() {
        val preset = CompressionPreset(
            id = "test_preset",
            name = "Test",
            quality = 80,
            maxDimension = 1920,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = true
        )

        val (targetW, targetH) = presetEngine.calculateTargetDimensions(800, 600, preset)
        assertEquals(800, targetW)
        assertEquals(600, targetH)
    }

    @Test
    fun `getAllPresets delegates to repository`() = runTest {
        val customPresets = listOf(
            CompressionPreset(id = "user_1", name = "My Preset", quality = 70, maxDimension = 1200, targetBytes = null, format = ImageFormat.JPEG, removeGps = true)
        )
        coEvery { presetRepository.getAllPresets() } returns flowOf(customPresets)

        val result = presetEngine.getAllPresets().first()
        assertEquals(1, result.size)
        assertEquals("My Preset", result[0].name)
    }

    @Test
    fun `getPresetById finds system and custom presets`() = runTest {
        val systemWhatsapp = presetEngine.getPresetById("whatsapp")
        assertNotNull(systemWhatsapp)
        assertEquals("WhatsApp", systemWhatsapp?.name)

        coEvery { presetRepository.getPresetById(123L) } returns CompressionPreset(
            id = "123",
            name = "Custom ID",
            quality = 90,
            maxDimension = 2000,
            targetBytes = null,
            format = ImageFormat.JPEG,
            removeGps = false
        )

        val customPreset = presetEngine.getPresetById("123")
        assertNotNull(customPreset)
        assertEquals("Custom ID", customPreset?.name)
    }
}
