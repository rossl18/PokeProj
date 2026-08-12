package com.pokeapp.ui.search.components

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
import com.pokeapp.ui.search.SearchSortOption

private fun SearchSortOption.label(): String = when (this) {
    SearchSortOption.RELEVANCE -> "Best match"
    SearchSortOption.NAME -> "Name (A-Z)"
    SearchSortOption.PRICE_ASC -> "Price (low to high)"
    SearchSortOption.PRICE_DESC -> "Price (high to low)"
}

@Composable
fun SearchSortMenu(selected: SearchSortOption, onSelect: (SearchSortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Sort, contentDescription = "Sort by ${selected.label()}")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SearchSortOption.entries.forEach { option ->
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
