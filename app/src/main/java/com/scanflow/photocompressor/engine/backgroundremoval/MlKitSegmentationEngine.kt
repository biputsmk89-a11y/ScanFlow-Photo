package com.scanflow.photocompressor.engine.backgroundremoval

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [BackgroundRemovalEngine] powered by Google ML Kit Selfie Segmentation.
 * 100% on-device, zero cloud dependency, immediately available offline.
 */
@Singleton
class MlKitSegmentationEngine @Inject constructor(
    private val modelManager: SegmentationModelManager,
    private val refinementEngine: MaskRefinementEngine,
    private val colorDecontaminationProcessor: ColorDecontaminationProcessor,
    private val compositingEngine: CompositingEngine,
    private val resolutionPolicy: SegmentationResolutionPolicy
) : BackgroundRemovalEngine {

    override suspend fun segment(
        image: Bitmap,
        options: BackgroundRemovalOptions
    ): SegmentationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        val workingBitmap = if (image.config != Bitmap.Config.ARGB_8888) {
            image.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            image
        }

        try {
            val segmenter = modelManager.getSegmenter()
            val inputImage = InputImage.fromBitmap(workingBitmap, 0)
            val mask = Tasks.await(segmenter.process(inputImage))

            val width = workingBitmap.width
            val height = workingBitmap.height
            val maskWidth = mask.width
            val maskHeight = mask.height
            val maskBuffer = mask.buffer
            maskBuffer.rewind()

            val rawMaskData = FloatArray(maskWidth * maskHeight)
            maskBuffer.asFloatBuffer().get(rawMaskData)

            // Calculate overall confidence metric
            var sumConfidence = 0.0f
            for (v in rawMaskData) sumConfidence += v
            val avgConfidence = if (rawMaskData.isNotEmpty()) sumConfidence / rawMaskData.size else 0f

            // Refine into clean, subpixel continuous alpha mask
            val refinedMask = refinementEngine.refine(
                rawMask = rawMaskData,
                maskWidth = maskWidth,
                maskHeight = maskHeight,
                targetWidth = width,
                targetHeight = height,
                options = options
            )

            val processingTime = System.currentTimeMillis() - startTime

            SegmentationResult(
                mask = refinedMask,
                confidence = avgConfidence,
                processingTimeMs = processingTime,
                inputWidth = width,
                inputHeight = height
            )
        } finally {
            if (workingBitmap != image) {
                workingBitmap.recycle()
            }
        }
    }

    override suspend fun removeBackground(
        image: Bitmap,
        options: BackgroundRemovalOptions
    ): Bitmap = withContext(Dispatchers.Default) {
        val segmentationResult = segment(image, options)
        val mask = segmentationResult.mask
        val width = mask.width
        val height = mask.height

        val srcPixels = IntArray(width * height)
        image.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val decontaminated = if (options.enableColorDecontamination) {
            colorDecontaminationProcessor.decontaminate(srcPixels, mask.alphaBuffer, width, height)
        } else {
            srcPixels
        }

        compositingEngine.createCutoutBitmap(decontaminated, mask, srcPixels)
    }

    override suspend fun replaceBackground(
        image: Bitmap,
        backgroundColor: Int,
        options: BackgroundRemovalOptions
    ): Bitmap = withContext(Dispatchers.Default) {
        val segmentationResult = segment(image, options)
        val mask = segmentationResult.mask
        val width = mask.width
        val height = mask.height

        val srcPixels = IntArray(width * height)
        image.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val decontaminated = if (options.enableColorDecontamination) {
            colorDecontaminationProcessor.decontaminate(srcPixels, mask.alphaBuffer, width, height)
        } else {
            srcPixels
        }

        compositingEngine.compositeSolidBackground(decontaminated, mask, backgroundColor)
    }
}
