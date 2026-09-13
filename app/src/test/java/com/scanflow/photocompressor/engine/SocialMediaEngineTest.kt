package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.domain.model.SocialContentType
import com.scanflow.photocompressor.domain.model.SocialPlatform
import com.scanflow.photocompressor.domain.model.SocialPresetRegistry
import org.junit.Assert.*
import org.junit.Test

class SocialMediaEngineTest {

    @Test
    fun `SocialPlatform contains all required networks`() {
        val platforms = SocialPlatform.values()
        assertEquals(6, platforms.size)
        assertTrue(platforms.contains(SocialPlatform.INSTAGRAM))
        assertTrue(platforms.contains(SocialPlatform.FACEBOOK))
        assertTrue(platforms.contains(SocialPlatform.TIKTOK))
        assertTrue(platforms.contains(SocialPlatform.YOUTUBE))
        assertTrue(platforms.contains(SocialPlatform.LINKEDIN))
        assertTrue(platforms.contains(SocialPlatform.CUSTOM))
    }

    @Test
    fun `SocialContentType contains all required content types`() {
        val types = SocialContentType.values()
        assertEquals(5, types.size)
        assertTrue(types.contains(SocialContentType.POST))
        assertTrue(types.contains(SocialContentType.STORY))
        assertTrue(types.contains(SocialContentType.COVER))
        assertTrue(types.contains(SocialContentType.THUMBNAIL))
        assertTrue(types.contains(SocialContentType.PROFILE))
    }

    @Test
    fun `SocialPresetRegistry provides valid internal presets for all platform and content type combinations`() {
        SocialPlatform.values().forEach { platform ->
            SocialContentType.values().forEach { type ->
                val preset = SocialPresetRegistry.getPreset(platform, type)
                assertNotNull("Preset for $platform and $type must exist", preset)
                assertEquals(platform, preset.platform)
                assertEquals(type, preset.type)
                assertTrue("Width must be positive", preset.targetWidth > 0)
                assertTrue("Height must be positive", preset.targetHeight > 0)
                assertTrue("ratioX must be positive", preset.ratioX > 0)
                assertTrue("ratioY must be positive", preset.ratioY > 0)
            }
        }
    }

    @Test
    fun `SocialPresetRegistry maps key platform presets accurately`() {
        // Instagram Post is square (1:1)
        val igPost = SocialPresetRegistry.getPreset(SocialPlatform.INSTAGRAM, SocialContentType.POST)
        assertEquals(1, igPost.ratioX)
        assertEquals(1, igPost.ratioY)
        assertEquals(1080, igPost.targetWidth)
        assertEquals(1080, igPost.targetHeight)

        // Instagram Story is vertical (9:16)
        val igStory = SocialPresetRegistry.getPreset(SocialPlatform.INSTAGRAM, SocialContentType.STORY)
        assertEquals(9, igStory.ratioX)
        assertEquals(16, igStory.ratioY)
        assertEquals(1080, igStory.targetWidth)
        assertEquals(1920, igStory.targetHeight)

        // YouTube Thumbnail is landscape (16:9)
        val ytThumb = SocialPresetRegistry.getPreset(SocialPlatform.YOUTUBE, SocialContentType.THUMBNAIL)
        assertEquals(16, ytThumb.ratioX)
        assertEquals(9, ytThumb.ratioY)
        assertEquals(1280, ytThumb.targetWidth)
        assertEquals(720, ytThumb.targetHeight)
    }
}
