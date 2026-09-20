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
    val isLoading: Boolean = true,
    val userMessage: String? = null
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
                val totalSaved = list.sumOf { it.savedBytes }
                _uiState.update {
                    it.copy(
                        historyList = list,
                        totalSavedBytes = totalSaved,
                        totalOperations = list.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            getHistoryUseCase.deleteEntry(id)
            _uiState.update { it.copy(userMessage = "Record removed from history") }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            getHistoryUseCase.clearAll()
            _uiState.update { it.copy(userMessage = "All history records cleared") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
