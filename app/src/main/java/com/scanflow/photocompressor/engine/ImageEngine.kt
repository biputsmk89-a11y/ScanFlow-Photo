package com.scanflow.photocompressor.engine

import android.net.Uri
import com.scanflow.photocompressor.domain.model.ImageAnalysis
import com.scanflow.photocompressor.domain.model.ImagePipeline
import com.scanflow.photocompressor.domain.model.ProcessingResult

/**
 * High-level engine contract for analyzing and processing images.
 * Keeps the engine testable and isolates the UI layer from direct interaction
 * with BitmapFactory, ContentResolver, or low-level encoder logic.
 */
interface ImageEngine {

    suspend fun analyze(
        source: Uri
    ): ImageAnalysis

    suspend fun process(
        source: Uri,
        pipeline: ImagePipeline
    ): ProcessingResult
}
