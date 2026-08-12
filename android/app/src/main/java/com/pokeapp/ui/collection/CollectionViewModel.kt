package com.pokeapp.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokeapp.data.repository.CollectionRepository
import com.pokeapp.domain.model.CollectionItem
import com.pokeapp.domain.model.CollectionSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: CollectionRepository,
) : ViewModel() {

    private val sortOption = MutableStateFlow(CollectionSortOption.RECENTLY_ADDED)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<CollectionUiState> = combine(
        repository.items,
        sortOption,
        selectedIds,
        isRefreshing,
    ) { items, sort, selected, refreshing ->
        CollectionUiState(
            items = sortItems(items, sort),
            sortOption = sort,
            selectedIds = selected,
            isRefreshing = refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            isRefreshing.value = true
            runCatching { repository.refreshPrices(force) }
            isRefreshing.value = false
        }
    }

    fun setSortOption(option: CollectionSortOption) {
        sortOption.value = option
    }

    fun toggleSelection(id: Long) {
        selectedIds.value = selectedIds.value.let { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteByIds(ids)
            clearSelection()
        }
    }

    private fun sortItems(items: List<CollectionItem>, sort: CollectionSortOption): List<CollectionItem> =
        when (sort) {
            CollectionSortOption.NAME -> items.sortedBy { it.cardName }
            CollectionSortOption.PRICE_ASC -> items.sortedBy { it.effectivePrice ?: 0.0 }
            CollectionSortOption.PRICE_DESC -> items.sortedByDescending { it.effectivePrice ?: 0.0 }
            CollectionSortOption.LINE_TOTAL_ASC -> items.sortedBy { it.lineTotal }
            CollectionSortOption.LINE_TOTAL_DESC -> items.sortedByDescending { it.lineTotal }
            CollectionSortOption.QUANTITY -> items.sortedByDescending { it.quantity }
            CollectionSortOption.RECENTLY_ADDED -> items.sortedByDescending { it.dateAdded }
        }
}
