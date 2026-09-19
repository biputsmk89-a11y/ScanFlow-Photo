package com.scanflow.photocompressor.engine.backgroundremoval

import javax.inject.Inject

enum class MaskEditType {
    RESTORE, // Restore foreground (alpha -> 1.0)
    ERASE    // Erase background (alpha -> 0.0)
}

data class MaskStrokePoint(val x: Float, val y: Float)

data class MaskEditStroke(
    val type: MaskEditType,
    val points: List<MaskStrokePoint>,
    val radiusPx: Float
)

/**
 * Lightweight in-memory mask editor layer supporting non-destructive manual refinement.
 * Enables Keep/Restore and Remove/Erase brush tools with full Undo/Redo without
 * duplicating full Bitmaps.
 */
class MaskEditor @Inject constructor() {

    private var baseMask: AlphaMask? = null
    private val appliedStrokes = mutableListOf<MaskEditStroke>()
    private val undoneStrokes = mutableListOf<MaskEditStroke>()

    fun setBaseMask(mask: AlphaMask) {
        baseMask = mask
        appliedStrokes.clear()
        undoneStrokes.clear()
    }

    fun addStroke(stroke: MaskEditStroke) {
        appliedStrokes.add(stroke)
        undoneStrokes.clear()
    }

    fun canUndo(): Boolean = appliedStrokes.isNotEmpty()

    fun canRedo(): Boolean = undoneStrokes.isNotEmpty()

    fun undo(): Boolean {
        if (appliedStrokes.isEmpty()) return false
        val last = appliedStrokes.removeAt(appliedStrokes.size - 1)
        undoneStrokes.add(last)
        return true
    }

    fun redo(): Boolean {
        if (undoneStrokes.isEmpty()) return false
        val next = undoneStrokes.removeAt(undoneStrokes.size - 1)
        appliedStrokes.add(next)
        return true
    }

    fun resetEdits() {
        appliedStrokes.clear()
        undoneStrokes.clear()
    }

    /**
     * Renders the final combined AlphaMask (base AI mask + manual brush strokes).
     */
    fun renderFinalMask(): AlphaMask? {
        val base = baseMask ?: return null
        if (appliedStrokes.isEmpty()) return base.copy()

        val rendered = base.copy()
        val width = rendered.width
        val height = rendered.height

        for (stroke in appliedStrokes) {
            val targetAlpha = if (stroke.type == MaskEditType.RESTORE) 1.0f else 0.0f
            val r = stroke.radiusPx
            val rSq = r * r

            for (pt in stroke.points) {
                val cx = pt.x.toInt()
                val cy = pt.y.toInt()
                val minX = maxOf(0, (cx - r).toInt())
                val maxX = minOf(width - 1, (cx + r).toInt())
                val minY = maxOf(0, (cy - r).toInt())
                val maxY = minOf(height - 1, (cy + r).toInt())

                for (y in minY..maxY) {
                    val dy = y - cy
                    for (x in minX..maxX) {
                        val dx = x - cx
                        if (dx * dx + dy * dy <= rSq) {
                            rendered[x, y] = targetAlpha
                        }
                    }
                }
            }
        }

        return rendered
    }
}
