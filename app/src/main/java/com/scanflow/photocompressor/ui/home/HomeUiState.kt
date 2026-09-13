package com.scanflow.photocompressor.ui.home

import com.scanflow.photocompressor.domain.model.ProcessingHistory

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val recentHistory: List<ProcessingHistory> = emptyList(),
    val totalSavedBytes: Long = 0,
    val totalOperations: Int = 0,
    val isLoading: Boolean = true
)
