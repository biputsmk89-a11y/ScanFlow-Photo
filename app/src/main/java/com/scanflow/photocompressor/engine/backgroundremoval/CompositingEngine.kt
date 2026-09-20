package com.scanflow.photocompressor.engine.backgroundremoval

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure 32-bit linear alpha compositing engine.
 * Renders transparent cutouts and uniform solid studio backgrounds with zero dithering noise.
 */
@Singleton
class CompositingEngine @Inject constructor() {

    /**
     * Builds a transparent ARGB_8888 cutout Bitmap from source pixels and refined alpha mask.
     */
    fun createCutoutBitmap(
        decontaminatedPixels: IntArray,
        mask: AlphaMask,
        sourceOriginalAlpha: IntArray? = null
    ): Bitmap {
        val width = mask.width
        val height = mask.height
        val outPixels = IntArray(width * height)
        val alphaBuffer = mask.alphaBuffer

        for (i in 0 until width * height) {
            val originalAlpha = if (sourceOriginalAlpha != null) {
                (sourceOriginalAlpha[i] ushr 24) and 0xFF
            } else {
                (decontaminatedPixels[i] ushr 24) and 0xFF
            }
            val finalAlpha = (originalAlpha * alphaBuffer[i] + 0.5f).toInt().coerceIn(0, 255)
            val rgb = decontaminatedPixels[i] and 0x00FFFFFF
            outPixels[i] = (finalAlpha shl 24) or rgb
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Composites foreground onto a solid studio background color (e.g. Blue, Red, White, Gray).
     */
    fun compositeSolidBackground(
        decontaminatedPixels: IntArray,
        mask: AlphaMask,
        backgroundColor: Int
    ): Bitmap {
        val width = mask.width
        val height = mask.height
        val alphaBuffer = mask.alphaBuffer
        val compositePixels = IntArray(width * height)

        val bgR = (backgroundColor shr 16) and 0xFF
        val bgG = (backgroundColor shr 8) and 0xFF
        val bgB = backgroundColor and 0xFF

        for (i in 0 until width * height) {
            val aFloat = alphaBuffer[i]

            if (aFloat <= 0.001f) {
                compositePixels[i] = (0xFF shl 24) or (bgR shl 16) or (bgG shl 8) or bgB
            } else if (aFloat >= 0.999f) {
                compositePixels[i] = (0xFF shl 24) or (decontaminatedPixels[i] and 0x00FFFFFF)
            } else {
                val a = (aFloat * 255f + 0.5f).toInt().coerceIn(0, 255)
                val pixel = decontaminatedPixels[i]
                val fgR = (pixel shr 16) and 0xFF
                val fgG = (pixel shr 8) and 0xFF
                val fgB = pixel and 0xFF

                val outR = ((fgR * a + bgR * (255 - a) + 127) / 255).coerceIn(0, 255)
                val outG = ((fgG * a + bgG * (255 - a) + 127) / 255).coerceIn(0, 255)
                val outB = ((fgB * a + bgB * (255 - a) + 127) / 255).coerceIn(0, 255)

                compositePixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(compositePixels, 0, width, 0, 0, width, height)
        return result
    }
}
