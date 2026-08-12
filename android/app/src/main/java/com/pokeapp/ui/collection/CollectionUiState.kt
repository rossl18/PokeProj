package com.pokeapp.ui.collection

import com.pokeapp.domain.model.CollectionItem
import com.pokeapp.domain.model.CollectionSortOption
import com.pokeapp.domain.model.UserCollection

data class CollectionUiState(
    val collections: List<UserCollection> = emptyList(),
    val selectedCollectionId: Long? = null,
    val items: List<CollectionItem> = emptyList(),
    val sortOption: CollectionSortOption = CollectionSortOption.RECENTLY_ADDED,
    val selectedIds: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()

    val selectedCollectionName: String
        get() = collections.firstOrNull { it.id == selectedCollectionId }?.name ?: "Collection"

    val displayedTotal: Double
        get() {
            val relevant = if (selectedIds.isEmpty()) items else items.filter { it.id in selectedIds }
            return relevant.sumOf { it.lineTotal }
        }
}
