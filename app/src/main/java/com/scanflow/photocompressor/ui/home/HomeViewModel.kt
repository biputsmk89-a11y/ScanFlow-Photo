package com.scanflow.photocompressor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.usecase.GetHistoryUseCase
import com.scanflow.photocompressor.domain.usecase.ManagePresetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val managePresetsUseCase: ManagePresetsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        seedPresetsIfNeeded()
    }

    private fun loadData() {
        // Observe recent history
        viewModelScope.launch {
            getHistoryUseCase.getRecentHistory(5).collect { history ->
                _uiState.update { it.copy(recentHistory = history) }
            }
        }

        // Load stats
        viewModelScope.launch {
            try {
                val totalSaved = getHistoryUseCase.getTotalSavedBytes()
                val totalOps = getHistoryUseCase.getTotalOperations()
                _uiState.update {
                    it.copy(
                        totalSavedBytes = totalSaved,
                        totalOperations = totalOps,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun seedPresetsIfNeeded() {
        viewModelScope.launch {
            try {
                managePresetsUseCase.seedDefaults()
            } catch (e: Exception) {
                // Non-critical, ignore
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val totalSaved = getHistoryUseCase.getTotalSavedBytes()
            val totalOps = getHistoryUseCase.getTotalOperations()
            _uiState.update {
                it.copy(
                    totalSavedBytes = totalSaved,
                    totalOperations = totalOps
                )
            }
        }
    }
}
