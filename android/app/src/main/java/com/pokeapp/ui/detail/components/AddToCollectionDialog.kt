package com.pokeapp.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pokeapp.domain.model.Card

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionDialog(
    variants: List<Card>,
    initialVariant: String,
    onConfirm: (variant: String, quantity: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedVariant by remember { mutableStateOf(initialVariant) }
    var quantity by remember { mutableIntStateOf(1) }
    var variantMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Collection") },
        text = {
            Column {
                Row {
                    TextButton(onClick = { variantMenuExpanded = true }) {
                        Text("Variant: $selectedVariant")
                    }
                    DropdownMenu(expanded = variantMenuExpanded, onDismissRequest = { variantMenuExpanded = false }) {
                        variants.forEach { card ->
                            DropdownMenuItem(
                                text = { Text(card.variant) },
                                onClick = {
                                    selectedVariant = card.variant
                                    variantMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { if (quantity > 1) quantity-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(quantity.toString(), modifier = Modifier.padding(top = 12.dp))
                    IconButton(onClick = { quantity++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedVariant, quantity) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
