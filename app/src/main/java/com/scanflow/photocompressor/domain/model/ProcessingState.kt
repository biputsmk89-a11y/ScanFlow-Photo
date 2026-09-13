package com.scanflow.photocompressor.domain.model

/**
 * 54. PROCESSING STATE
 * Standardized state lifecycle for image processing operations.
 */
sealed interface ProcessingState {
    data object Idle : ProcessingState
    data object Analyzing : ProcessingState

    data class Processing(
        val progress: Float
    ) : ProcessingState

    data class Completed(
        val result: ProcessingResult
    ) : ProcessingState

    data class Failed(
        val error: ProcessingError
    ) : ProcessingState

    data object Cancelled : ProcessingState
}
