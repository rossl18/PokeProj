package com.pokeapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeapp.data.repository.CardRepository
import com.pokeapp.util.OcrTextMatcher
import com.pokeapp.util.ScanMatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isProcessing: Boolean = false,
    val rawOcrText: String? = null,
    val matches: List<ScanMatch> = emptyList(),
    val hasConfidentMatch: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: CardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onTextRecognized(blocks: List<String>) {
        val bestGuess = blocks.firstOrNull()?.trim().orEmpty()
        if (bestGuess.isEmpty()) {
            _uiState.value = ScanUiState(error = "No text detected — try again with better lighting.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ScanUiState(isProcessing = true, rawOcrText = bestGuess)
            runCatching { repository.search(bestGuess, limit = 15) }
                .onSuccess { candidates ->
                    val pool = if (candidates.isEmpty() && blocks.size > 1) {
                        // Best-guess line returned nothing; broaden using the next block.
                        runCatching { repository.search(blocks[1].trim(), limit = 15) }.getOrDefault(emptyList())
                    } else {
                        candidates
                    }
                    val ranked = OcrTextMatcher.rank(bestGuess, pool).take(8)
                    _uiState.value = ScanUiState(
                        rawOcrText = bestGuess,
                        matches = ranked,
                        hasConfidentMatch = OcrTextMatcher.hasConfidentMatch(ranked),
                    )
                }
                .onFailure { e ->
                    _uiState.value = ScanUiState(rawOcrText = bestGuess, error = e.message ?: "Search failed")
                }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }
}
