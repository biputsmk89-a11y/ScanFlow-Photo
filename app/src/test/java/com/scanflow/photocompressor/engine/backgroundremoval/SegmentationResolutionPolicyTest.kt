package com.scanflow.photocompressor.engine.backgroundremoval

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SegmentationResolutionPolicyTest {

    private lateinit var policy: SegmentationResolutionPolicy

    @Before
    fun setUp() {
        policy = SegmentationResolutionPolicy()
    }

    @Test
    fun `calculateWorkingDimensions preserves standard resolution`() {
        val (w, h) = policy.calculateWorkingDimensions(1080, 1920)
        assertTrue(w in 512..2048)
        assertTrue(h in 512..2048)
    }

    @Test
    fun `calculateWorkingDimensions scales down massive 8000x6000 image safely`() {
        val (w, h) = policy.calculateWorkingDimensions(8000, 6000)
        assertTrue("Max dimension should be capped at 2048", maxOf(w, h) <= 2048)
        val expectedRatio = 8000f / 6000f
        val actualRatio = w.toFloat() / h.toFloat()
        assertEquals(expectedRatio, actualRatio, 0.05f)
    }
}
