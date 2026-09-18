package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.domain.model.WatermarkConfig
import com.scanflow.photocompressor.domain.model.WatermarkPosition
import org.junit.Assert.*
import org.junit.Test

class WatermarkEngineTest {

    @Test
    fun `WatermarkConfig creates valid instance with default parameters`() {
        val config = WatermarkConfig(text = "© ScanFlow")
        assertEquals("© ScanFlow", config.text)
        assertEquals(WatermarkPosition.BOTTOM_RIGHT, config.position)
        assertEquals(0.5f, config.opacity)
        assertEquals(24f, config.fontSize)
        assertEquals(0xFFFFFFFF, config.color)
        assertEquals(0f, config.rotation)
        assertEquals(16, config.margin)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WatermarkConfig throws on blank text`() {
        WatermarkConfig(text = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WatermarkConfig throws on negative opacity`() {
        WatermarkConfig(text = "Valid", opacity = -0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WatermarkConfig throws on opacity greater than 1`() {
        WatermarkConfig(text = "Valid", opacity = 1.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WatermarkConfig throws on zero or negative font size`() {
        WatermarkConfig(text = "Valid", fontSize = 0f)
    }

    @Test
    fun `WatermarkPosition contains all 9 cardinal and center placements`() {
        val positions = WatermarkPosition.values()
        assertEquals(9, positions.size)
        assertTrue(positions.contains(WatermarkPosition.TOP_LEFT))
        assertTrue(positions.contains(WatermarkPosition.TOP_CENTER))
        assertTrue(positions.contains(WatermarkPosition.TOP_RIGHT))
        assertTrue(positions.contains(WatermarkPosition.CENTER_LEFT))
        assertTrue(positions.contains(WatermarkPosition.CENTER))
        assertTrue(positions.contains(WatermarkPosition.CENTER_RIGHT))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_LEFT))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_CENTER))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_RIGHT))
    }
}
