package com.scanflow.photocompressor.domain.usecase

import android.net.Uri
import com.scanflow.photocompressor.domain.model.ImageAnalysis
import com.scanflow.photocompressor.engine.ImageAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 56. USE CASE: AnalyzeImageUseCase
 * Inspects image metadata, resolution, format, and aspect ratio without loading full pixels into memory.
 */
class AnalyzeImageUseCase @Inject constructor(
    private val imageAnalyzer: ImageAnalyzer
) {
    suspend operator fun invoke(uri: Uri): Result<ImageAnalysis> = withContext(Dispatchers.IO) {
        imageAnalyzer.analyze(uri)
    }
}
