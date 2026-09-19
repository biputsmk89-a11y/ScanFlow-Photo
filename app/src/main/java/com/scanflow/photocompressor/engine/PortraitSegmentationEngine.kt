package com.scanflow.photocompressor.engine

import android.graphics.Bitmap
import com.scanflow.photocompressor.engine.backgroundremoval.BackgroundRemovalOptions
import com.scanflow.photocompressor.engine.backgroundremoval.ColorDecontaminationProcessor
import com.scanflow.photocompressor.engine.backgroundremoval.MaskRefinementEngine
import com.scanflow.photocompressor.engine.backgroundremoval.MlKitSegmentationEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-grade Portrait Segmentation Engine.
 * Delegates to modular [MlKitSegmentationEngine], [MaskRefinementEngine], and [ColorDecontaminationProcessor].
 * 100% on-device, offline, zero halo, zero dot pixels.
 */
@Singleton
class PortraitSegmentationEngine @Inject constructor(
    private val mlKitEngine: MlKitSegmentationEngine,
    private val refinementEngine: MaskRefinementEngine,
    private val decontaminationProcessor: ColorDecontaminationProcessor
) {

    companion object {
        const val EDGE_SMOOTH_PASSES = 2
        const val ALPHA_THRESHOLD_LOW = 0.28f
        const val ALPHA_THRESHOLD_HIGH = 0.82f
    }

    suspend fun removeBackground(sourceBitmap: Bitmap): Bitmap {
        return mlKitEngine.removeBackground(
            sourceBitmap,
            BackgroundRemovalOptions(
                lowConfidenceThreshold = ALPHA_THRESHOLD_LOW,
                highConfidenceThreshold = ALPHA_THRESHOLD_HIGH,
                edgeFeatherRadius = EDGE_SMOOTH_PASSES,
                enableColorDecontamination = true,
                enableHoleClosing = true,
                enableIslandPruning = true
            )
        )
    }

    suspend fun replaceBackground(sourceBitmap: Bitmap, backgroundColor: Int): Bitmap {
        return mlKitEngine.replaceBackground(
            sourceBitmap,
            backgroundColor,
            BackgroundRemovalOptions(
                lowConfidenceThreshold = ALPHA_THRESHOLD_LOW,
                highConfidenceThreshold = ALPHA_THRESHOLD_HIGH,
                edgeFeatherRadius = EDGE_SMOOTH_PASSES,
                enableColorDecontamination = true,
                enableHoleClosing = true,
                enableIslandPruning = true
            )
        )
    }

    // Delegators for backward compatibility and testing
    fun resampleConfidenceMask(
        maskData: FloatArray,
        maskWidth: Int,
        maskHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): FloatArray {
        return refinementEngine.resampleBilinear(maskData, maskWidth, maskHeight, targetWidth, targetHeight)
    }

    fun confidenceToAlpha(confidence: Float): Float {
        return refinementEngine.confidenceToAlpha(confidence, ALPHA_THRESHOLD_LOW, ALPHA_THRESHOLD_HIGH)
    }

    fun pruneNoiseIslands(alpha: FloatArray, width: Int, height: Int): FloatArray {
        return refinementEngine.pruneNoiseIslands(alpha, width, height)
    }

    fun closeSubjectHoles(alpha: FloatArray, width: Int, height: Int): FloatArray {
        return refinementEngine.closeSubjectHoles(alpha, width, height)
    }

    fun smoothAlphaEdges(alpha: FloatArray, width: Int, height: Int): FloatArray {
        return refinementEngine.smoothAlphaEdges(alpha, width, height)
    }

    fun decontaminateEdgeColors(
        srcPixels: IntArray,
        alpha: FloatArray,
        width: Int,
        height: Int
    ): IntArray {
        return decontaminationProcessor.decontaminate(srcPixels, alpha, width, height)
    }
}
