package com.scanflow.photocompressor.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.photocompressor.domain.model.ProcessingHistory
import com.scanflow.photocompressor.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val historyList: List<ProcessingHistory> = emptyList(),
    val totalSavedBytes: Long = 0,
    val totalOperations: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getHistoryUseCase.getAllHistory().collect { list ->
                _uiState.update { it.copy(historyList = list, isLoading = false) }
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    totalSavedBytes = getHistoryUseCase.getTotalSavedBytes(),
                    totalOperations = getHistoryUseCase.getTotalOperations()
                )
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch { getHistoryUseCase.deleteEntry(id) }
    }

    fun clearAll() {
        viewModelScope.launch { getHistoryUseCase.clearAll() }
    }
}
