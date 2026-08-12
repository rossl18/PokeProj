package com.pokeapp.ui.search.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokeapp.domain.model.UserCollection
import com.pokeapp.ui.search.OwnershipFilter

private fun OwnershipFilter.label(): String = when (this) {
    OwnershipFilter.ALL -> "All results"
    OwnershipFilter.OWNED -> "Owned"
    OwnershipFilter.NOT_OWNED -> "Not owned"
}

@Composable
fun OwnershipFilterMenu(
    ownershipFilter: OwnershipFilter,
    collections: List<UserCollection>,
    filterCollectionId: Long?,
    onOwnershipChange: (OwnershipFilter) -> Unit,
    onCollectionChange: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val filterCollectionName = collections.firstOrNull { it.id == filterCollectionId }?.name

    Row {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.FilterList, contentDescription = null)
            Text(
                if (ownershipFilter == OwnershipFilter.ALL) {
                    ownershipFilter.label()
                } else {
                    "${ownershipFilter.label()} (${filterCollectionName ?: "..."})"
                }
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OwnershipFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onOwnershipChange(option)
                        expanded = false
                    },
                )
            }
            if (ownershipFilter != OwnershipFilter.ALL && collections.size > 1) {
                HorizontalDivider()
                Text(
                    "In collection:",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
                collections.forEach { collection ->
                    DropdownMenuItem(
                        text = { Text(collection.name) },
                        onClick = {
                            onCollectionChange(collection.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
