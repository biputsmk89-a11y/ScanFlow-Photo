package com.scanflow.photocompressor.engine.backgroundremoval

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ColorDecontaminationTest {

    private lateinit var processor: ColorDecontaminationProcessor

    @Before
    fun setUp() {
        processor = ColorDecontaminationProcessor()
    }

    @Test
    fun `decontaminate replaces contaminated background color with adjacent foreground color`() {
        val width = 8
        val height = 4
        val srcPixels = IntArray(width * height)
        val alpha = FloatArray(width * height)

        val contaminatedBg = 0x00FFFFFF // Old white wall
        val solidHair = 0x00112233       // Subject hair color

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                when {
                    x < 3 -> {
                        srcPixels[idx] = contaminatedBg
                        alpha[idx] = 0.0f
                    }
                    x == 3 -> {
                        srcPixels[idx] = contaminatedBg // Edge carries white background
                        alpha[idx] = 0.50f              // Boundary
                    }
                    else -> {
                        srcPixels[idx] = solidHair
                        alpha[idx] = 1.0f
                    }
                }
            }
        }

        val decontaminated = processor.decontaminate(srcPixels, alpha, width, height)

        val borderIdx = 1 * width + 3
        assertEquals(
            "Border pixel color must be decontaminated to match solid foreground",
            solidHair and 0x00FFFFFF,
            decontaminated[borderIdx] and 0x00FFFFFF
        )
    }
}
