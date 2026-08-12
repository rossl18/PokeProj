package com.pokeapp.ui.search.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokeapp.util.ScanMatch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsSheet(
    matches: List<ScanMatch>,
    rawOcrText: String?,
    onSelect: (ScanMatch) -> Unit,
    onManualSearch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (matches.isEmpty()) {
                Text("No confident match found.")
                Button(
                    onClick = { onManualSearch(rawOcrText.orEmpty()) },
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                ) {
                    Text("Search manually for \"${rawOcrText.orEmpty()}\"")
                }
            } else {
                Text("Is this your card?", modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn {
                    items(matches) { match ->
                        SearchResultRow(
                            card = match.card,
                            onClick = { onSelect(match) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Button(
                    onClick = { onManualSearch(rawOcrText.orEmpty()) },
                    modifier = Modifier.padding(vertical = 16.dp),
                ) {
                    Text("Not seeing your card? Search manually")
                }
            }
        }
    }
}
