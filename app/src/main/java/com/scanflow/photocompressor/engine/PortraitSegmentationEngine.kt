package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 100% On-Device Offline Portrait Segmentation Engine using Google ML Kit.
 * Extracts human subjects (hair, face, clothing) and produces transparent cutouts
 * or composite studio backgrounds with feathered edges.
 */
@Singleton
class PortraitSegmentationEngine @Inject constructor() {

    private val segmenter: Segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        Segmentation.getClient(options)
    }

    /**
     * Removes the background from a portrait bitmap, returning a transparent ARGB_8888 bitmap.
     * Edge transitions are feathered for natural hair and shoulder borders.
     */
    suspend fun removeBackground(sourceBitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        runCatching {
            val workingBitmap = if (sourceBitmap.config != Bitmap.Config.ARGB_8888) {
                sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                sourceBitmap
            }

            val inputImage = InputImage.fromBitmap(workingBitmap, 0)
            val mask = Tasks.await(segmenter.process(inputImage))

            val width = workingBitmap.width
            val height = workingBitmap.height
            val maskWidth = mask.width
            val maskHeight = mask.height
            val maskBuffer = mask.buffer
            maskBuffer.rewind()

            val srcPixels = IntArray(width * height)
            workingBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val outPixels = IntArray(width * height)

            if (maskWidth == width && maskHeight == height) {
                for (i in 0 until width * height) {
                    val confidence = maskBuffer.float
                    val originalPixel = srcPixels[i]
                    val originalAlpha = (originalPixel ushr 24) and 0xFF

                    val alphaFactor = when {
                        confidence <= 0.25f -> 0f
                        confidence >= 0.75f -> 1f
                        else -> (confidence - 0.25f) / 0.5f
                    }

                    val finalAlpha = (originalAlpha * alphaFactor).toInt().coerceIn(0, 255)
                    outPixels[i] = (finalAlpha shl 24) or (originalPixel and 0x00FFFFFF)
                }
            } else {
                val maskData = FloatArray(maskWidth * maskHeight)
                maskBuffer.asFloatBuffer().get(maskData)

                for (y in 0 until height) {
                    val maskY = (y * maskHeight / height).coerceIn(0, maskHeight - 1)
                    val maskRowOffset = maskY * maskWidth
                    val imgRowOffset = y * width

                    for (x in 0 until width) {
                        val maskX = (x * maskWidth / width).coerceIn(0, maskWidth - 1)
                        val confidence = maskData[maskRowOffset + maskX]
                        val originalPixel = srcPixels[imgRowOffset + x]
                        val originalAlpha = (originalPixel ushr 24) and 0xFF

                        val alphaFactor = when {
                            confidence <= 0.25f -> 0f
                            confidence >= 0.75f -> 1f
                            else -> (confidence - 0.25f) / 0.5f
                        }

                        val finalAlpha = (originalAlpha * alphaFactor).toInt().coerceIn(0, 255)
                        outPixels[imgRowOffset + x] = (finalAlpha shl 24) or (originalPixel and 0x00FFFFFF)
                    }
                }
            }

            outputBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)

            if (workingBitmap != sourceBitmap) {
                workingBitmap.recycle()
            }

            outputBitmap
        }.getOrElse {
            // Fallback: return a copy of the source bitmap if ML Kit fails or non-portrait
            sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * Composites the portrait onto a solid background color.
     */
    suspend fun replaceBackground(sourceBitmap: Bitmap, backgroundColor: Int): Bitmap = withContext(Dispatchers.Default) {
        val cutout = removeBackground(sourceBitmap)
        val result = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(cutout, 0f, 0f, null)
        cutout.recycle()
        result
    }
}
