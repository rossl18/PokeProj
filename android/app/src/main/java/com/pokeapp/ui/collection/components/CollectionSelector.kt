package com.pokeapp.ui.collection.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun CollectionSelector(
    collections: List<UserCollection>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Row {
        TextButton(onClick = { menuExpanded = true }) {
            Text(collections.firstOrNull { it.id == selectedId }?.name ?: "Collection")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            collections.forEach { collection ->
                DropdownMenuItem(
                    text = { Text(collection.name) },
                    onClick = {
                        onSelect(collection.id)
                        menuExpanded = false
                    },
                    trailingIcon = {
                        if (collections.size > 1) {
                            IconButton(onClick = {
                                onDelete(collection.id)
                                menuExpanded = false
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${collection.name}")
                            }
                        }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("New collection") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    showCreateDialog = true
                },
            )
        }
    }

    if (showCreateDialog) {
        CreateCollectionDialog(
            onConfirm = { name ->
                onCreate(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }
}

@Composable
private fun CreateCollectionDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New collection") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
