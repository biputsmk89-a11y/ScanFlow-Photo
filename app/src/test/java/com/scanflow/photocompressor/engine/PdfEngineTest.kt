package com.scanflow.photocompressor.engine

import com.scanflow.photocompressor.domain.model.PdfConfig
import com.scanflow.photocompressor.domain.model.PdfMargin
import com.scanflow.photocompressor.domain.model.PdfOrientation
import com.scanflow.photocompressor.domain.model.PdfPageSize
import com.scanflow.photocompressor.domain.model.PdfQuality
import org.junit.Assert.*
import org.junit.Test

class PdfEngineTest {

    @Test
    fun `PdfPageSize defines accurate standard page presets`() {
        // A4: 595 x 842 pt
        assertEquals(595, PdfPageSize.A4.widthPt)
        assertEquals(842, PdfPageSize.A4.heightPt)

        // A5: 420 x 595 pt
        assertEquals(420, PdfPageSize.A5.widthPt)
        assertEquals(595, PdfPageSize.A5.heightPt)

        // Letter: 612 x 792 pt
        assertEquals(612, PdfPageSize.LETTER.widthPt)
        assertEquals(792, PdfPageSize.LETTER.heightPt)

        // Original: 0 x 0 pt (matches image dimension)
        assertEquals(0, PdfPageSize.ORIGINAL.widthPt)
        assertEquals(0, PdfPageSize.ORIGINAL.heightPt)

        // Custom: configurable point dimension
        assertEquals(600, PdfPageSize.CUSTOM.widthPt)
        assertEquals(800, PdfPageSize.CUSTOM.heightPt)
    }

    @Test
    fun `PdfOrientation covers Portrait, Landscape, and Auto`() {
        val orientations = PdfOrientation.values()
        assertEquals(3, orientations.size)
        assertTrue(orientations.contains(PdfOrientation.PORTRAIT))
        assertTrue(orientations.contains(PdfOrientation.LANDSCAPE))
        assertTrue(orientations.contains(PdfOrientation.AUTO))
    }

    @Test
    fun `PdfMargin defines standard margins`() {
        assertEquals(0, PdfMargin.NONE.marginPt)
        assertEquals(18, PdfMargin.SMALL.marginPt)
        assertEquals(36, PdfMargin.NORMAL.marginPt)
    }

    @Test
    fun `PdfQuality defines proper compression quality tiers`() {
        assertEquals(50, PdfQuality.LOW.compressionQuality)
        assertEquals(1080, PdfQuality.LOW.maxDimension)

        assertEquals(75, PdfQuality.MEDIUM.compressionQuality)
        assertEquals(1440, PdfQuality.MEDIUM.maxDimension)

        assertEquals(90, PdfQuality.HIGH.compressionQuality)
        assertEquals(2048, PdfQuality.HIGH.maxDimension)
    }

    @Test
    fun `PdfConfig default configuration is safe and balanced`() {
        val config = PdfConfig()
        assertEquals("document", config.title)
        assertEquals(PdfPageSize.A4, config.pageSize)
        assertEquals(PdfMargin.NORMAL, config.margin)
        assertEquals(PdfOrientation.AUTO, config.orientation)
        assertEquals(PdfQuality.MEDIUM, config.quality)
    }
}
