package com.scanflow.photocompressor.engine

import android.graphics.Color
import com.scanflow.photocompressor.domain.model.PassportBackground
import com.scanflow.photocompressor.domain.model.PassportConfig
import com.scanflow.photocompressor.domain.model.PassportPrintLayout
import com.scanflow.photocompressor.domain.model.PassportSpec
import org.junit.Assert.*
import org.junit.Test

class PassportEngineTest {

    @Test
    fun `PassportSpec presets include 2x3, 3x4, 4x6, and Custom without official compliance claims`() {
        // 2 x 3
        assertEquals("2 × 3", PassportSpec.PRESET_2X3.displayName)
        assertEquals(2, PassportSpec.PRESET_2X3.ratioX)
        assertEquals(3, PassportSpec.PRESET_2X3.ratioY)

        // 3 x 4
        assertEquals("3 × 4", PassportSpec.PRESET_3X4.displayName)
        assertEquals(3, PassportSpec.PRESET_3X4.ratioX)
        assertEquals(4, PassportSpec.PRESET_3X4.ratioY)

        // 4 x 6
        assertEquals("4 × 6", PassportSpec.PRESET_4X6.displayName)
        assertEquals(2, PassportSpec.PRESET_4X6.ratioX)
        assertEquals(3, PassportSpec.PRESET_4X6.ratioY)

        // Custom
        assertEquals("Custom", PassportSpec.CUSTOM.displayName)

        // Ensure no claims of official compliance in descriptions
        PassportSpec.values().forEach { spec ->
            assertFalse(
                "Spec description should not claim official or government compliance",
                spec.description.contains("official", ignoreCase = true) ||
                    spec.description.contains("guaranteed", ignoreCase = true) ||
                    spec.description.contains("buku nikah", ignoreCase = true)
            )
        }
    }

    @Test
    fun `PassportPrintLayout defines exact copies 1, 2, 4, 6, 8, 9, and 12`() {
        val layouts = PassportPrintLayout.values()
        assertEquals(7, layouts.size)

        assertEquals("1", PassportPrintLayout.COPIES_1.displayName)
        assertEquals(1, PassportPrintLayout.COPIES_1.copies)

        assertEquals("2", PassportPrintLayout.COPIES_2.displayName)
        assertEquals(2, PassportPrintLayout.COPIES_2.copies)

        assertEquals("4", PassportPrintLayout.COPIES_4.displayName)
        assertEquals(4, PassportPrintLayout.COPIES_4.copies)

        assertEquals("6", PassportPrintLayout.COPIES_6.displayName)
        assertEquals(6, PassportPrintLayout.COPIES_6.copies)

        assertEquals("8", PassportPrintLayout.COPIES_8.displayName)
        assertEquals(8, PassportPrintLayout.COPIES_8.copies)

        assertEquals("9", PassportPrintLayout.COPIES_9.displayName)
        assertEquals(9, PassportPrintLayout.COPIES_9.copies)

        assertEquals("12", PassportPrintLayout.COPIES_12.displayName)
        assertEquals(12, PassportPrintLayout.COPIES_12.copies)
    }

    @Test
    fun `PassportConfig includes disclaimer check submission requirements before printing`() {
        val config = PassportConfig()
        assertEquals("Check the submission requirements before printing.", config.disclaimer)
        assertEquals(PassportSpec.PRESET_3X4, config.spec)
        assertEquals(PassportBackground.ORIGINAL, config.background)
        assertEquals(PassportPrintLayout.COPIES_1, config.printLayout)
        assertTrue(config.addCutMarks)
    }
}
