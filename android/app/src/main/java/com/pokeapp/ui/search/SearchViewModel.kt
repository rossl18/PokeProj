package com.pokeapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeapp.data.repository.CardRepository
import com.pokeapp.domain.model.Card
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Card> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val fallbackNotice: String? = null,
)

private const val DEBOUNCE_MS = 300L

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false, error = null, fallbackNotice = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query)
        }
    }

    fun setPrefilledQuery(query: String) {
        onQueryChange(query)
    }

    private suspend fun runSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, fallbackNotice = null)

        val strict = runCatching { repository.search(query) }
        if (strict.isFailure) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = strict.exceptionOrNull()?.message ?: "Search failed")
            return
        }
        val strictResults = strict.getOrDefault(emptyList())

        val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (strictResults.isNotEmpty() || tokens.size <= 1) {
            _uiState.value = _uiState.value.copy(results = strictResults, isLoading = false)
            return
        }

        // Nothing matched every token — progressively relax by dropping
        // tokens, numeric/card-number-looking ones first, since a guessed or
        // wrong card number is the most common reason a real card goes
        // unmatched (e.g. "darkrai ex 129" with no #129 should still surface
        // other Darkrai EX cards).
        val ordered = tokens.sortedBy { token -> if (token.trimStart('#').all(Char::isDigit)) 0 else 1 }
        var relaxedResults = emptyList<Card>()
        var keptTokens = tokens

        for (dropCount in 1 until tokens.size) {
            val remaining = ordered.drop(dropCount)
            if (remaining.isEmpty()) break
            val relaxedQuery = remaining.joinToString(" ")
            val attempt = runCatching { repository.search(relaxedQuery) }.getOrDefault(emptyList())
            if (attempt.isNotEmpty()) {
                relaxedResults = attempt
                keptTokens = remaining
                break
            }
        }

        val notice = if (relaxedResults.isNotEmpty()) {
            "No exact match for \"$query\" — showing results for \"${keptTokens.joinToString(" ")}\""
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(results = relaxedResults, isLoading = false, fallbackNotice = notice)
    }
}
