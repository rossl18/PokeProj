package com.pokeapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeapp.data.repository.CardRepository
import com.pokeapp.data.repository.CollectionRepository
import com.pokeapp.domain.model.Card
import com.pokeapp.domain.model.PricePoint
import com.pokeapp.domain.model.UserCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardDetailUiState(
    val isLoading: Boolean = true,
    val variants: List<Card> = emptyList(),
    val selectedVariant: String? = null,
    val history: List<PricePoint> = emptyList(),
    val error: String? = null,
    val addedToCollection: Boolean = false,
    val collections: List<UserCollection> = emptyList(),
    val defaultCollectionId: Long? = null,
) {
    val selectedCard: Card? get() = variants.firstOrNull { it.variant == selectedVariant }
}

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val collectionRepository: CollectionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val cardId: String = checkNotNull(savedStateHandle["cardId"])
    private val initialVariant: String? = savedStateHandle["variant"]

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            combine(collectionRepository.collections, collectionRepository.selectedCollectionId) { collections, selectedId ->
                collections to selectedId
            }.collect { (collections, selectedId) ->
                _uiState.value = _uiState.value.copy(collections = collections, defaultCollectionId = selectedId)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { cardRepository.getVariants(cardId) }
                .onSuccess { variants ->
                    val variant = initialVariant ?: variants.firstOrNull()?.variant
                    _uiState.value = _uiState.value.copy(variants = variants, selectedVariant = variant, isLoading = false)
                    variant?.let { loadHistory(it) }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load card")
                }
        }
    }

    fun selectVariant(variant: String) {
        _uiState.value = _uiState.value.copy(selectedVariant = variant, history = emptyList())
        loadHistory(variant)
    }

    private fun loadHistory(variant: String) {
        viewModelScope.launch {
            runCatching { cardRepository.getHistory(cardId, variant) }
                .onSuccess { history -> _uiState.value = _uiState.value.copy(history = history) }
        }
    }

    fun addToCollection(quantity: Int, collectionId: Long) {
        val card = _uiState.value.selectedCard ?: return
        viewModelScope.launch {
            collectionRepository.addOrIncrement(card, quantity, collectionId)
            _uiState.value = _uiState.value.copy(addedToCollection = true)
        }
    }

    fun consumeAddedEvent() {
        _uiState.value = _uiState.value.copy(addedToCollection = false)
    }
}
