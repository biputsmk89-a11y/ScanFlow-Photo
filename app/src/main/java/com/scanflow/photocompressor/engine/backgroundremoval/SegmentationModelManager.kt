package com.scanflow.photocompressor.engine.backgroundremoval

import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle, configuration, and offline readiness of the bundled ML Kit Selfie Segmenter.
 */
@Singleton
class SegmentationModelManager @Inject constructor() {

    private var activeSegmenter: Segmenter? = null

    /**
     * Obtains or creates the singleton Segmenter configured for high-quality SINGLE_IMAGE_MODE.
     */
    @Synchronized
    fun getSegmenter(): Segmenter {
        return activeSegmenter ?: run {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .enableRawSizeMask()
                .build()
            val segmenter = Segmentation.getClient(options)
            activeSegmenter = segmenter
            segmenter
        }
    }

    /**
     * Bundled Selfie Segmenter is immediately available offline upon installation.
     */
    fun isAvailableOffline(): Boolean = true

    /**
     * Closes the active segmenter to free native ML Kit resources if necessary.
     */
    @Synchronized
    fun close() {
        try {
            activeSegmenter?.close()
        } catch (_: Exception) {}
        activeSegmenter = null
    }
}
