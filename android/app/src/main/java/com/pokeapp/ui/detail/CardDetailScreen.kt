package com.pokeapp.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pokeapp.ui.detail.components.AddToCollectionDialog
import com.pokeapp.ui.detail.components.PriceHistoryChart
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val currency = NumberFormat.getCurrencyInstance()
    var showAddDialog by remember { mutableStateOf(false) }
    var variantMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.addedToCollection) {
        if (state.addedToCollection) {
            snackbarHostState.showSnackbar("Added to collection")
            viewModel.consumeAddedEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedCard?.cardName ?: "Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(padding).padding(32.dp))
            return@Scaffold
        }
        state.error?.let {
            Text(it, modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        val card = state.selectedCard ?: return@Scaffold

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            AsyncImage(
                model = card.fullImageUrl,
                contentDescription = card.cardName,
                modifier = Modifier.fillMaxWidth(),
            )

            card.setLabel?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }

            Row(modifier = Modifier.padding(top = 12.dp)) {
                TextButton(onClick = { variantMenuExpanded = true }) {
                    Text("Variant: ${card.variant}")
                }
                DropdownMenu(expanded = variantMenuExpanded, onDismissRequest = { variantMenuExpanded = false }) {
                    state.variants.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.variant) },
                            onClick = {
                                viewModel.selectVariant(v.variant)
                                variantMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Text(
                "Market: ${card.marketPrice?.let { currency.format(it) } ?: "—"}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Low ${card.lowPrice?.let { currency.format(it) } ?: "—"} · " +
                    "Mid ${card.midPrice?.let { currency.format(it) } ?: "—"} · " +
                    "High ${card.highPrice?.let { currency.format(it) } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text("Price History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            PriceHistoryChart(history = state.history)

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text("Add to Collection")
            }
        }

        if (showAddDialog && state.defaultCollectionId != null) {
            AddToCollectionDialog(
                variants = state.variants,
                initialVariant = card.variant,
                collections = state.collections,
                initialCollectionId = state.defaultCollectionId!!,
                onConfirm = { variant, quantity, collectionId ->
                    if (variant != card.variant) viewModel.selectVariant(variant)
                    viewModel.addToCollection(quantity, collectionId)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false },
            )
        }
    }
}
