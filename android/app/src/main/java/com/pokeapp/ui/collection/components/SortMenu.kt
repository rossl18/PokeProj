package com.pokeapp.ui.collection.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pokeapp.domain.model.CollectionSortOption

private fun CollectionSortOption.label(): String = when (this) {
    CollectionSortOption.NAME -> "Name (A-Z)"
    CollectionSortOption.PRICE_ASC -> "Price (low to high)"
    CollectionSortOption.PRICE_DESC -> "Price (high to low)"
    CollectionSortOption.LINE_TOTAL_ASC -> "Line total (low to high)"
    CollectionSortOption.LINE_TOTAL_DESC -> "Line total (high to low)"
    CollectionSortOption.QUANTITY -> "Quantity"
    CollectionSortOption.RECENTLY_ADDED -> "Recently added"
}

@Composable
fun SortMenu(
    selected: CollectionSortOption,
    onSelect: (CollectionSortOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Sort, contentDescription = "Sort by ${selected.label()}")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        CollectionSortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label()) },
                onClick = {
                    onSelect(option)
                    expanded = false
                },
            )
        }
    }
}
