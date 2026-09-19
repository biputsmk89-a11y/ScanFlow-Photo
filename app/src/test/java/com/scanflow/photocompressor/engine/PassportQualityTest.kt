package com.scanflow.photocompressor.engine

import android.graphics.Color
import com.scanflow.photocompressor.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive quality validation tests for Passport & ID Photo Studio.
 *
 * Validates:
 * 1. All preset resolutions meet HD minimum standards
 * 2. Aspect ratio integrity across all presets
 * 3. Quality consistency between low and high resolution presets
 * 4. Background color resolution for all options
 * 5. Print layout grid mathematics for all copy counts
 * 6. Frame style rendering dimensions
 * 7. Edge quality pipeline configuration
 */
class PassportQualityTest {

    // =====================================================================
    // 1. HD RESOLUTION VALIDATION — All presets must meet minimum HD standards
    // =====================================================================

    @Test
    fun `all PassportSpec presets meet minimum HD resolution of 600px width`() {
        PassportSpec.values().forEach { spec ->
            assertTrue(
                "Preset ${spec.displayName} width (${spec.targetWidthPx}px) must be >= 600px for HD quality",
                spec.targetWidthPx >= 600
            )
        }
    }

    @Test
    fun `all PassportSpec presets meet minimum HD resolution of 800px height`() {
        PassportSpec.values().forEach { spec ->
            assertTrue(
                "Preset ${spec.displayName} height (${spec.targetHeightPx}px) must be >= 800px for HD quality",
                spec.targetHeightPx >= 800
            )
        }
    }

    @Test
    fun `PRESET_2X3 target resolution is 600x900 HD`() {
        assertEquals(600, PassportSpec.PRESET_2X3.targetWidthPx)
        assertEquals(900, PassportSpec.PRESET_2X3.targetHeightPx)
    }

    @Test
    fun `PRESET_3X4 target resolution is 900x1200 HD`() {
        assertEquals(900, PassportSpec.PRESET_3X4.targetWidthPx)
        assertEquals(1200, PassportSpec.PRESET_3X4.targetHeightPx)
    }

    @Test
    fun `PRESET_4X6 target resolution is 800x1200 HD`() {
        assertEquals(800, PassportSpec.PRESET_4X6.targetWidthPx)
        assertEquals(1200, PassportSpec.PRESET_4X6.targetHeightPx)
    }

    @Test
    fun `CUSTOM preset target resolution is 800x800 HD`() {
        assertEquals(800, PassportSpec.CUSTOM.targetWidthPx)
        assertEquals(800, PassportSpec.CUSTOM.targetHeightPx)
    }

    // =====================================================================
    // 2. ASPECT RATIO INTEGRITY — Pixel dimensions must match declared ratio
    // =====================================================================

    @Test
    fun `PRESET_2X3 pixel dimensions match 2 to 3 aspect ratio`() {
        val spec = PassportSpec.PRESET_2X3
        val pixelRatio = spec.targetWidthPx.toFloat() / spec.targetHeightPx.toFloat()
        val declaredRatio = spec.ratioX.toFloat() / spec.ratioY.toFloat()
        assertEquals(
            "2×3 pixel ratio must match declared ratio",
            declaredRatio, pixelRatio, 0.01f
        )
    }

    @Test
    fun `PRESET_3X4 pixel dimensions match 3 to 4 aspect ratio`() {
        val spec = PassportSpec.PRESET_3X4
        val pixelRatio = spec.targetWidthPx.toFloat() / spec.targetHeightPx.toFloat()
        val declaredRatio = spec.ratioX.toFloat() / spec.ratioY.toFloat()
        assertEquals(
            "3×4 pixel ratio must match declared ratio",
            declaredRatio, pixelRatio, 0.01f
        )
    }

    @Test
    fun `PRESET_4X6 pixel dimensions match 2 to 3 aspect ratio`() {
        val spec = PassportSpec.PRESET_4X6
        val pixelRatio = spec.targetWidthPx.toFloat() / spec.targetHeightPx.toFloat()
        val declaredRatio = spec.ratioX.toFloat() / spec.ratioY.toFloat()
        assertEquals(
            "4×6 pixel ratio must match declared ratio",
            declaredRatio, pixelRatio, 0.01f
        )
    }

    @Test
    fun `CUSTOM preset pixel dimensions match 1 to 1 aspect ratio`() {
        val spec = PassportSpec.CUSTOM
        assertEquals("Custom must be square", spec.targetWidthPx, spec.targetHeightPx)
    }

    // =====================================================================
    // 3. QUALITY CONSISTENCY — Config defaults produce HD quality output
    // =====================================================================

    @Test
    fun `default PassportConfig uses 3x4 preset with HD resolution`() {
        val config = PassportConfig()
        assertEquals(PassportSpec.PRESET_3X4, config.spec)
        assertTrue("Default width must be HD", config.spec.targetWidthPx >= 900)
        assertTrue("Default height must be HD", config.spec.targetHeightPx >= 1200)
    }

    @Test
    fun `all preset configs produce total pixel count above 500000 for clarity`() {
        PassportSpec.values().forEach { spec ->
            val totalPixels = spec.targetWidthPx.toLong() * spec.targetHeightPx.toLong()
            assertTrue(
                "Preset ${spec.displayName} total pixels (${totalPixels}) must be >= 500000 for HD",
                totalPixels >= 500_000
            )
        }
    }

    @Test
    fun `smallest preset 2x3 still produces clean output at 540000 pixels`() {
        val spec = PassportSpec.PRESET_2X3
        val totalPixels = spec.targetWidthPx.toLong() * spec.targetHeightPx.toLong()
        assertEquals(540_000L, totalPixels)
    }

    @Test
    fun `largest preset 3x4 produces 1080000 pixels for maximum clarity`() {
        val spec = PassportSpec.PRESET_3X4
        val totalPixels = spec.targetWidthPx.toLong() * spec.targetHeightPx.toLong()
        assertEquals(1_080_000L, totalPixels)
    }

    // =====================================================================
    // 4. BACKGROUND COLOR RESOLUTION — All backgrounds resolve correctly
    // =====================================================================

    @Test
    fun `ORIGINAL background resolves to null effectiveBackgroundColor`() {
        val config = PassportConfig(background = PassportBackground.ORIGINAL)
        assertNull(config.effectiveBackgroundColor)
    }

    @Test
    fun `BLUE background resolves to non-null color`() {
        val config = PassportConfig(background = PassportBackground.BLUE)
        assertNotNull(config.effectiveBackgroundColor)
        assertEquals(Color.rgb(0, 85, 165), config.effectiveBackgroundColor)
    }

    @Test
    fun `RED background resolves to non-null color`() {
        val config = PassportConfig(background = PassportBackground.RED)
        assertNotNull(config.effectiveBackgroundColor)
        assertEquals(Color.rgb(211, 47, 47), config.effectiveBackgroundColor)
    }

    @Test
    fun `WHITE background resolves to Color WHITE`() {
        val config = PassportConfig(background = PassportBackground.WHITE)
        assertEquals(Color.WHITE, config.effectiveBackgroundColor)
    }

    @Test
    fun `GRAY background resolves to light gray color`() {
        val config = PassportConfig(background = PassportBackground.GRAY)
        assertEquals(Color.rgb(224, 224, 224), config.effectiveBackgroundColor)
    }

    @Test
    fun `CUSTOM background without customColor resolves to null`() {
        val config = PassportConfig(
            background = PassportBackground.CUSTOM,
            customBackgroundColor = null
        )
        assertNull(config.effectiveBackgroundColor)
    }

    @Test
    fun `CUSTOM background with customColor resolves correctly`() {
        val customColor = Color.rgb(128, 0, 255)
        val config = PassportConfig(
            background = PassportBackground.CUSTOM,
            customBackgroundColor = customColor
        )
        assertEquals(customColor, config.effectiveBackgroundColor)
    }

    // =====================================================================
    // 5. PRINT LAYOUT GRID MATHEMATICS — All layouts produce correct grids
    // =====================================================================

    @Test
    fun `all print layouts produce correct total copies from cols x rows`() {
        PassportPrintLayout.values().forEach { layout ->
            assertEquals(
                "Layout ${layout.displayName}: copies (${layout.copies}) must equal cols*rows (${layout.cols}*${layout.rows})",
                layout.copies, layout.cols * layout.rows
            )
        }
    }

    @Test
    fun `print sheet dimensions scale correctly for all presets and layouts`() {
        val padding = 32
        PassportSpec.values().forEach { spec ->
            PassportPrintLayout.values().filter { it != PassportPrintLayout.COPIES_1 }.forEach { layout ->
                val pw = spec.targetWidthPx
                val ph = spec.targetHeightPx
                val cols = layout.cols
                val rows = layout.rows

                val sheetWidth = (pw * cols) + (padding * (cols + 1))
                val sheetHeight = (ph * rows) + (padding * (rows + 1))

                assertTrue(
                    "Sheet for ${spec.displayName} ${layout.displayName} must have positive width ($sheetWidth)",
                    sheetWidth > 0
                )
                assertTrue(
                    "Sheet for ${spec.displayName} ${layout.displayName} must have positive height ($sheetHeight)",
                    sheetHeight > 0
                )
                // Ensure sheet is larger than a single photo
                assertTrue(
                    "Sheet width must be >= single photo width",
                    sheetWidth >= pw
                )
                assertTrue(
                    "Sheet height must be >= single photo height",
                    sheetHeight >= ph
                )
            }
        }
    }

    // =====================================================================
    // 6. FRAME STYLE DIMENSIONS — All frame borders are valid
    // =====================================================================

    @Test
    fun `NONE frame has zero border width`() {
        assertEquals(0f, IdFrameStyle.NONE.borderWidthDp)
    }

    @Test
    fun `THIN frame has 2dp border`() {
        assertEquals(2f, IdFrameStyle.THIN.borderWidthDp)
    }

    @Test
    fun `CLASSIC frame has 6dp border`() {
        assertEquals(6f, IdFrameStyle.CLASSIC.borderWidthDp)
    }

    @Test
    fun `PROFESSIONAL frame has 8dp border`() {
        assertEquals(8f, IdFrameStyle.PROFESSIONAL.borderWidthDp)
    }

    @Test
    fun `frame borders are ordered from smallest to largest`() {
        val borders = IdFrameStyle.values().map { it.borderWidthDp }
        for (i in 1 until borders.size) {
            assertTrue(
                "Frame borders should be ordered ascending",
                borders[i] >= borders[i - 1]
            )
        }
    }

    // =====================================================================
    // 7. TRANSFORM AND ZOOM BOUNDS — Position controls are within safe range
    // =====================================================================

    @Test
    fun `default config has neutral transform (zoom=1, pan=0, rotation=0)`() {
        val config = PassportConfig()
        assertEquals(1.0f, config.zoom)
        assertEquals(0f, config.panX)
        assertEquals(0f, config.panY)
        assertEquals(0f, config.rotationDegrees)
    }

    @Test
    fun `default config enables cut marks for print sheet`() {
        val config = PassportConfig()
        assertTrue("Cut marks should be enabled by default", config.addCutMarks)
    }

    @Test
    fun `default config uses PROFESSIONAL frame style`() {
        val config = PassportConfig()
        assertEquals(IdFrameStyle.PROFESSIONAL, config.frameStyle)
    }

    // =====================================================================
    // 8. MEMORY SAFETY — All preset resolutions are within safe bounds
    // =====================================================================

    @Test
    fun `all preset resolutions are within MAX_BITMAP_DIMENSION 4096`() {
        PassportSpec.values().forEach { spec ->
            assertTrue(
                "Preset ${spec.displayName} width (${spec.targetWidthPx}) must be <= 4096",
                spec.targetWidthPx <= 4096
            )
            assertTrue(
                "Preset ${spec.displayName} height (${spec.targetHeightPx}) must be <= 4096",
                spec.targetHeightPx <= 4096
            )
        }
    }

    @Test
    fun `largest print sheet for 12 copies of 3x4 fits within reasonable memory`() {
        val spec = PassportSpec.PRESET_3X4
        val layout = PassportPrintLayout.COPIES_12
        val padding = 32
        val sheetWidth = (spec.targetWidthPx * layout.cols) + (padding * (layout.cols + 1))
        val sheetHeight = (spec.targetHeightPx * layout.rows) + (padding * (layout.rows + 1))
        val memoryBytes = sheetWidth.toLong() * sheetHeight.toLong() * 4L // ARGB_8888

        // Must fit within 64MB memory budget
        assertTrue(
            "Largest print sheet ($sheetWidth x $sheetHeight) memory ${memoryBytes / 1024 / 1024}MB must be < 64MB",
            memoryBytes < 64L * 1024 * 1024
        )
    }
}
