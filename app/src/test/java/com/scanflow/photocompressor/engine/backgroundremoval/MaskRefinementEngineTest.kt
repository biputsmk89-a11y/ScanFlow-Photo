package com.scanflow.photocompressor.engine.backgroundremoval

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MaskRefinementEngineTest {

    private lateinit var refinementEngine: MaskRefinementEngine

    @Before
    fun setUp() {
        refinementEngine = MaskRefinementEngine()
    }

    @Test
    fun `resampleBilinear eliminates blocky staircasing on upsampling`() {
        val maskData = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 1.0f
        )
        val resampled = refinementEngine.resampleBilinear(
            maskData = maskData,
            maskWidth = 2,
            maskHeight = 2,
            targetWidth = 4,
            targetHeight = 2
        )

        assertEquals(8, resampled.size)
        assertTrue("Sub-pixel gradient first pixel", resampled[0] < 0.35f)
        assertTrue("Sub-pixel gradient transition", resampled[1] in 0.2f..0.8f)
        assertTrue("Sub-pixel gradient second transition", resampled[2] in 0.5f..0.9f)
        assertTrue("Sub-pixel gradient final pixel", resampled[3] > 0.65f)
    }

    @Test
    fun `confidenceToAlpha enforces clean noise cutoff and solid threshold`() {
        assertEquals(0.0f, refinementEngine.confidenceToAlpha(0.10f), 0.001f)
        assertEquals(0.0f, refinementEngine.confidenceToAlpha(0.25f), 0.001f)
        assertEquals(1.0f, refinementEngine.confidenceToAlpha(0.85f), 0.001f)
        assertEquals(1.0f, refinementEngine.confidenceToAlpha(0.99f), 0.001f)
    }

    @Test
    fun `pruneNoiseIslands purges disconnected dot pixels`() {
        val width = 20
        val height = 20
        val alpha = FloatArray(width * height) { 0.0f }

        // Isolated dot pixel cluster in upper left corner
        alpha[1 * width + 1] = 0.85f
        alpha[1 * width + 2] = 0.75f

        // Main subject torso touching bottom
        for (y in 12 until 20) {
            for (x in 5 until 15) {
                alpha[y * width + x] = 1.0f
            }
        }

        val cleaned = refinementEngine.pruneNoiseIslands(alpha, width, height)

        assertEquals(0.0f, cleaned[1 * width + 1], 0.001f)
        assertEquals(0.0f, cleaned[1 * width + 2], 0.001f)
        assertEquals(1.0f, cleaned[15 * width + 10], 0.001f)
    }

    @Test
    fun `closeSubjectHoles solidifies internal pinholes inside subject`() {
        val width = 25
        val height = 25
        val alpha = FloatArray(width * height) { 0.0f }

        for (y in 5..19) {
            for (x in 5..19) {
                alpha[y * width + x] = 0.98f
            }
        }

        val holeIdx = 12 * width + 12
        alpha[holeIdx] = 0.35f

        val closed = refinementEngine.closeSubjectHoles(alpha, width, height)
        assertEquals(1.0f, closed[holeIdx], 0.001f)
    }
}
