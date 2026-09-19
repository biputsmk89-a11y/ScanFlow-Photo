package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.engine.backgroundremoval.ColorDecontaminationProcessor
import com.scanflow.photocompressor.engine.backgroundremoval.MaskRefinementEngine
import com.scanflow.photocompressor.engine.backgroundremoval.MlKitSegmentationEngine
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PortraitSegmentationEngineTest {

    private lateinit var engine: PortraitSegmentationEngine

    @Before
    fun setUp() {
        engine = PortraitSegmentationEngine(
            mlKitEngine = mockk<MlKitSegmentationEngine>(relaxed = true),
            refinementEngine = MaskRefinementEngine(),
            decontaminationProcessor = ColorDecontaminationProcessor()
        )
    }

    @Test
    fun `resampleConfidenceMask interpolates bilinearly without jagged steps`() {
        // 2x2 mask
        val maskData = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 1.0f
        )
        // Upsample to 4x2
        val resampled = engine.resampleConfidenceMask(
            maskData = maskData,
            maskWidth = 2,
            maskHeight = 2,
            targetWidth = 4,
            targetHeight = 2
        )

        assertEquals(8, resampled.size)
        // Check that intermediate values form a continuous gradient between 0 and 1
        assertTrue("Expected first pixel near 0", resampled[0] < 0.35f)
        assertTrue("Expected middle pixel transition", resampled[1] in 0.2f..0.8f)
        assertTrue("Expected second middle pixel transition", resampled[2] in 0.5f..0.9f)
        assertTrue("Expected last pixel near 1", resampled[3] > 0.65f)
    }

    @Test
    fun `confidenceToAlpha enforces noise cutoff and solid threshold`() {
        // Background noise (< 0.28f) must be 0.0f
        assertEquals(0.0f, engine.confidenceToAlpha(0.0f), 0.001f)
        assertEquals(0.0f, engine.confidenceToAlpha(0.15f), 0.001f)
        assertEquals(0.0f, engine.confidenceToAlpha(0.27f), 0.001f)

        // Solid subject core (>= 0.82f) must be 1.0f
        assertEquals(1.0f, engine.confidenceToAlpha(0.82f), 0.001f)
        assertEquals(1.0f, engine.confidenceToAlpha(0.95f), 0.001f)
        assertEquals(1.0f, engine.confidenceToAlpha(1.0f), 0.001f)

        // Transition zone must follow S-curve
        val midAlpha = engine.confidenceToAlpha(0.55f)
        assertTrue("Mid alpha should be in transition zone", midAlpha in 0.4f..0.6f)
    }

    @Test
    fun `pruneNoiseIslands removes isolated floating dot pixels`() {
        val width = 20
        val height = 20
        val alpha = FloatArray(width * height) { 0.0f }

        // Place an isolated dot pixel cluster in the upper corner (y=2, x=2..3)
        alpha[2 * width + 2] = 0.8f
        alpha[2 * width + 3] = 0.7f

        // Place main subject body touching the bottom (y=12..19, x=5..15)
        for (y in 12 until 20) {
            for (x in 5 until 15) {
                alpha[y * width + x] = 1.0f
            }
        }

        val cleaned = engine.pruneNoiseIslands(alpha, width, height)

        // The isolated floating dot in the upper corner should be completely wiped to 0
        assertEquals(0.0f, cleaned[2 * width + 2], 0.001f)
        assertEquals(0.0f, cleaned[2 * width + 3], 0.001f)

        // The main subject touching the bottom must remain intact
        assertEquals(1.0f, cleaned[15 * width + 10], 0.001f)
    }

    @Test
    fun `closeSubjectHoles solidifies internal pinholes inside hair or face`() {
        val width = 25
        val height = 25
        val alpha = FloatArray(width * height) { 0.0f }

        // Create a solid circular/square head region (x: 5..19, y: 5..19)
        for (y in 5..19) {
            for (x in 5..19) {
                alpha[y * width + x] = 0.98f
            }
        }

        // Introduce a hole/dip inside the center (e.g. hair reflection at x=12, y=12)
        val centerIdx = 12 * width + 12
        alpha[centerIdx] = 0.30f

        val closed = engine.closeSubjectHoles(alpha, width, height)

        // The hole must be filled to solid (1.0f)
        assertEquals(1.0f, closed[centerIdx], 0.001f)
    }

    @Test
    fun `smoothAlphaEdges smooths transition while preserving pure background and foreground`() {
        val width = 5
        val height = 5
        val alpha = FloatArray(width * height) { 0.0f }

        // Background is 0.0f
        // Set an edge transition in the middle column
        for (y in 1..3) {
            alpha[y * width + 1] = 0.0f
            alpha[y * width + 2] = 0.5f // Transition
            alpha[y * width + 3] = 1.0f // Foreground
        }

        val smoothed = engine.smoothAlphaEdges(alpha, width, height)

        // Pure background remains 0.0f
        assertEquals(0.0f, smoothed[0], 0.001f)
        // Transition pixel is smoothed
        assertTrue(smoothed[2 * width + 2] in 0.3f..0.7f)
    }

    @Test
    fun `decontaminateEdgeColors replaces edge halo with adjacent subject color`() {
        val width = 10
        val height = 5
        val srcPixels = IntArray(width * height)
        val alpha = FloatArray(width * height)

        val whiteWallColor = 0x00FFFFFF // Contaminated original background
        val blackHairColor = 0x001A1A1A // True subject hair color

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (x < 4) {
                    // Background
                    srcPixels[idx] = whiteWallColor
                    alpha[idx] = 0.0f
                } else if (x == 4) {
                    // Transition border (contaminated with white wall color)
                    srcPixels[idx] = whiteWallColor
                    alpha[idx] = 0.5f
                } else {
                    // Solid black hair
                    srcPixels[idx] = blackHairColor
                    alpha[idx] = 1.0f
                }
            }
        }

        val decontaminated = engine.decontaminateEdgeColors(srcPixels, alpha, width, height)

        // Border pixel (x=4) should have its RGB decontaminated to black hair color
        val borderIdx = 2 * width + 4
        assertEquals(
            "Border pixel color should be decontaminated to match solid foreground",
            blackHairColor and 0x00FFFFFF,
            decontaminated[borderIdx] and 0x00FFFFFF
        )
    }
}
