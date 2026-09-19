package com.scanflow.photocompressor.engine.backgroundremoval

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MaskEditorTest {

    private lateinit var maskEditor: MaskEditor

    @Before
    fun setUp() {
        maskEditor = MaskEditor()
    }

    @Test
    fun `maskEditor applies brush strokes with undo and redo correctly`() {
        val width = 10
        val height = 10
        val baseBuffer = FloatArray(width * height) { 0.0f }
        val baseMask = AlphaMask(width, height, baseBuffer)

        maskEditor.setBaseMask(baseMask)
        assertFalse(maskEditor.canUndo())
        assertFalse(maskEditor.canRedo())

        // Add restore stroke at (5, 5) with radius 2
        val stroke = MaskEditStroke(
            type = MaskEditType.RESTORE,
            points = listOf(MaskStrokePoint(5f, 5f)),
            radiusPx = 2f
        )
        maskEditor.addStroke(stroke)
        assertTrue(maskEditor.canUndo())

        val rendered = maskEditor.renderFinalMask()
        assertNotNull(rendered)
        assertEquals(1.0f, rendered!![5, 5], 0.001f)

        // Undo
        assertTrue(maskEditor.undo())
        val undoneRender = maskEditor.renderFinalMask()
        assertNotNull(undoneRender)
        assertEquals(0.0f, undoneRender!![5, 5], 0.001f)

        // Redo
        assertTrue(maskEditor.canRedo())
        assertTrue(maskEditor.redo())
        val redoneRender = maskEditor.renderFinalMask()
        assertNotNull(redoneRender)
        assertEquals(1.0f, redoneRender!![5, 5], 0.001f)
    }
}
