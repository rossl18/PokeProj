package com.pokeapp.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pokeapp.ui.search.components.CameraCaptureScreen
import com.pokeapp.ui.search.components.OwnershipFilterMenu
import com.pokeapp.ui.search.components.ScanResultsSheet
import com.pokeapp.ui.search.components.SearchResultRow
import com.pokeapp.ui.search.components.SearchSortMenu

private const val TAB_SEARCH = 0
private const val TAB_SCAN = 1

@androidx.camera.core.ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onCardClick: (cardId: String, variant: String) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
    scanViewModel: ScanViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(TAB_SEARCH) }
    val searchState by searchViewModel.uiState.collectAsState()
    val scanState by scanViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Search & Scan") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                SecondaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == TAB_SEARCH,
                        onClick = { selectedTab = TAB_SEARCH },
                        text = { Text("Search") },
                    )
                    Tab(
                        selected = selectedTab == TAB_SCAN,
                        onClick = { selectedTab = TAB_SCAN },
                        text = { Text("Scan") },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            TAB_SEARCH -> Column(modifier = Modifier.padding(padding)) {
                OutlinedTextField(
                    value = searchState.query,
                    onValueChange = searchViewModel::onQueryChange,
                    label = { Text("Name, set, or card number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                if (searchState.results.isNotEmpty() || searchState.ownershipFilter != OwnershipFilter.ALL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        OwnershipFilterMenu(
                            ownershipFilter = searchState.ownershipFilter,
                            collections = searchState.collections,
                            filterCollectionId = searchState.filterCollectionId,
                            onOwnershipChange = searchViewModel::setOwnershipFilter,
                            onCollectionChange = searchViewModel::setFilterCollection,
                        )
                        SearchSortMenu(selected = searchState.sortOption, onSelect = searchViewModel::setSortOption)
                    }
                }
                if (searchState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                searchState.error?.let { Text(it, modifier = Modifier.padding(16.dp)) }
                searchState.fallbackNotice?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(searchState.results, key = { "${it.cardId}:${it.variant}" }) { card ->
                        SearchResultRow(card = card, onClick = { onCardClick(card.cardId, card.variant) })
                    }
                }
            }

            TAB_SCAN -> Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                CameraCaptureScreen(
                    onTextRecognized = { blocks -> scanViewModel.onTextRecognized(blocks) },
                    modifier = Modifier.fillMaxSize(),
                )
                if (scanState.rawOcrText != null) {
                    ScanResultsSheet(
                        matches = scanState.matches,
                        rawOcrText = scanState.rawOcrText,
                        onSelect = { match ->
                            scanViewModel.reset()
                            onCardClick(match.card.cardId, match.card.variant)
                        },
                        onManualSearch = { text ->
                            scanViewModel.reset()
                            selectedTab = TAB_SEARCH
                            searchViewModel.setPrefilledQuery(text)
                        },
                        onDismiss = { scanViewModel.reset() },
                    )
                }
            }
        }
    }
}
