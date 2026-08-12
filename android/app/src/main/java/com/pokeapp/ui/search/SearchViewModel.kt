package com.pokeapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeapp.data.repository.CardRepository
import com.pokeapp.data.repository.CollectionRepository
import com.pokeapp.domain.model.Card
import com.pokeapp.domain.model.CardKey
import com.pokeapp.domain.model.UserCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchSortOption { RELEVANCE, NAME, PRICE_ASC, PRICE_DESC }
enum class OwnershipFilter { ALL, OWNED, NOT_OWNED }

data class SearchUiState(
    val query: String = "",
    val results: List<Card> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val fallbackNotice: String? = null,
    val sortOption: SearchSortOption = SearchSortOption.RELEVANCE,
    val ownershipFilter: OwnershipFilter = OwnershipFilter.ALL,
    val filterCollectionId: Long? = null,
    val collections: List<UserCollection> = emptyList(),
)

private const val DEBOUNCE_MS = 300L

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: CardRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val rawResults = MutableStateFlow<List<Card>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val fallbackNotice = MutableStateFlow<String?>(null)
    private val sortOption = MutableStateFlow(SearchSortOption.RELEVANCE)
    private val ownershipFilter = MutableStateFlow(OwnershipFilter.ALL)
    private val filterCollectionId = MutableStateFlow<Long?>(null)

    private var searchJob: kotlinx.coroutines.Job? = null

    private val loadState = combine(query, isLoading, error, fallbackNotice) { q, loading, err, notice ->
        LoadState(q, loading, err, notice)
    }
    private val filterState = combine(sortOption, ownershipFilter, filterCollectionId) { sort, ownership, collectionId ->
        FilterState(sort, ownership, collectionId)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        rawResults,
        loadState,
        filterState,
        collectionRepository.ownedKeysByCollection,
        collectionRepository.collections,
    ) { results, load, filter, ownedByCollection, collections ->
        val effectiveCollectionId = filter.filterCollectionId ?: collections.firstOrNull()?.id
        val ownedKeys = ownedByCollection[effectiveCollectionId].orEmpty()

        val filtered = when (filter.ownershipFilter) {
            OwnershipFilter.ALL -> results
            OwnershipFilter.OWNED -> results.filter { CardKey(it.cardId, it.variant) in ownedKeys }
            OwnershipFilter.NOT_OWNED -> results.filter { CardKey(it.cardId, it.variant) !in ownedKeys }
        }
        val sorted = when (filter.sortOption) {
            SearchSortOption.RELEVANCE -> filtered
            SearchSortOption.NAME -> filtered.sortedBy { it.cardName }
            SearchSortOption.PRICE_ASC -> filtered.sortedBy { it.marketPrice ?: Double.MAX_VALUE }
            SearchSortOption.PRICE_DESC -> filtered.sortedByDescending { it.marketPrice ?: -1.0 }
        }

        SearchUiState(
            query = load.query,
            results = sorted,
            isLoading = load.isLoading,
            error = load.error,
            fallbackNotice = load.fallbackNotice,
            sortOption = filter.sortOption,
            ownershipFilter = filter.ownershipFilter,
            filterCollectionId = effectiveCollectionId,
            collections = collections,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            rawResults.value = emptyList()
            isLoading.value = false
            error.value = null
            fallbackNotice.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(newQuery)
        }
    }

    fun setPrefilledQuery(q: String) {
        onQueryChange(q)
    }

    fun setSortOption(option: SearchSortOption) {
        sortOption.value = option
    }

    fun setOwnershipFilter(filter: OwnershipFilter) {
        ownershipFilter.value = filter
    }

    fun setFilterCollection(collectionId: Long) {
        filterCollectionId.value = collectionId
    }

    private suspend fun runSearch(q: String) {
        isLoading.value = true
        error.value = null
        fallbackNotice.value = null

        val strict = runCatching { repository.search(q) }
        if (strict.isFailure) {
            isLoading.value = false
            error.value = strict.exceptionOrNull()?.message ?: "Search failed"
            return
        }
        val strictResults = strict.getOrDefault(emptyList())

        val tokens = q.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (strictResults.isNotEmpty() || tokens.size <= 1) {
            rawResults.value = strictResults
            isLoading.value = false
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

        rawResults.value = relaxedResults
        isLoading.value = false
        fallbackNotice.value = if (relaxedResults.isNotEmpty()) {
            "No exact match for \"$q\" — showing results for \"${keptTokens.joinToString(" ")}\""
        } else {
            null
        }
    }
}

private data class LoadState(val query: String, val isLoading: Boolean, val error: String?, val fallbackNotice: String?)
private data class FilterState(val sortOption: SearchSortOption, val ownershipFilter: OwnershipFilter, val filterCollectionId: Long?)
