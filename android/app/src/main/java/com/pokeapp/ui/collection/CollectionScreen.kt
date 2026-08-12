package com.pokeapp.ui.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pokeapp.ui.collection.components.CollectionListItem
import com.pokeapp.ui.collection.components.SelectionTopBar
import com.pokeapp.ui.collection.components.SortMenu
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onCardClick: (cardId: String, variant: String) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getCurrencyInstance()

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = state.selectedIds.size,
                    total = state.displayedTotal,
                    onClearSelection = viewModel::clearSelection,
                    onDelete = viewModel::deleteSelected,
                )
            } else {
                TopAppBar(
                    title = { Text("Collection · ${currency.format(state.displayedTotal)}") },
                    actions = {
                        SortMenu(selected = state.sortOption, onSelect = viewModel::setSortOption)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        },
    ) { padding ->
        if (state.items.isEmpty() && !state.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text(
                        "Your collection is empty",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Search or scan a card to add one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    CollectionListItem(
                        item = item,
                        isSelectionMode = state.isSelectionMode,
                        isSelected = item.id in state.selectedIds,
                        onClick = {
                            if (state.isSelectionMode) {
                                viewModel.toggleSelection(item.id)
                            } else {
                                onCardClick(item.cardId, item.variant)
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(item.id) },
                    )
                }
            }
        }
    }
}
