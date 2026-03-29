package com.studiomath.pencilnotes.ui.composeComponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.studiomath.drawview.data.repository.FileRepository
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.file.FileExplorerViewModel

@Composable
fun RequestNameDialog(
    title: String = "",
    labelTextField: String = "",
    textConfirmButton: String = "",
    onDismissRequest: () -> Unit,
    onConfirm: (inputText: String) -> Unit,
    isAllowedInput: (inputText: String) -> Int? = { null }
){
    var errorResId by remember { mutableStateOf<Int?>(null) }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errorResId = isAllowedInput(it)
                    },
                    label = { Text(labelTextField) },
                    singleLine = true,
                    isError = errorResId != null,
                    supportingText = {
                        errorResId?.let { resId ->
                            Text(
                                text = stringResource(resId),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && errorResId == null
            ) {
                Text(text = textConfirmButton)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.menu_cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}


@Composable
fun ConfirmActionDialog(
    title: String = "",
    textDescription: String = "",
    textConfirmButton: String = stringResource(id = R.string.button_confirm),
    textCancelButton: String = stringResource(id = R.string.menu_cancel),
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = { Text(text = textDescription) },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(text = textConfirmButton, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(text = textCancelButton)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveFileDialog(
    // SOSTITUIAMO IL VIEWMODEL CON LE LAMBDA (State Hoisting)
    getSubFolders: suspend (Int?) -> List<FileRepository.FileItem>,
    isFolderSelected: (folderId: Int) -> Boolean,
    isValidMove: (targetFolderId: Int?) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (targetFolderId: Int?) -> Unit
) {
    val rootName = stringResource(id = R.string.button_home)
    // Navigation state inside the dialog
    var currentFolderId by remember { mutableStateOf<Int?>(null) } // null = root
    var breadcrumbs by remember(rootName) { mutableStateOf(listOf<Pair<String, Int?>>(rootName to null)) }

    var subFolders by remember { mutableStateOf(emptyList<FileRepository.FileItem>()) }

    // Ricarica le cartelle ogni volta che navighiamo
    LaunchedEffect(currentFolderId) {
        subFolders = getSubFolders(currentFolderId)
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with navigation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    if (currentFolderId != null) {
                        IconButton(onClick = {
                            // Go back
                            if (breadcrumbs.size > 1) {
                                breadcrumbs = breadcrumbs.dropLast(1)
                                currentFolderId = breadcrumbs.last().second
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.studiomath.drawview.R.string.common_action_back))
                        }
                    }

                    Text(
                        text = breadcrumbs.last().first,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    )
                }

                HorizontalDivider()

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    items(items = subFolders, key = { it.id }) { folder ->

                        // Usiamo la funzione passata dall'esterno per controllare se è selezionata
                        val isSelected = isFolderSelected(folder.id)
                        val isEnabled = !isSelected

                        ListItem(
                            headlineContent = { Text(folder.name) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable(enabled = isEnabled) {
                                    currentFolderId = folder.id
                                    breadcrumbs = breadcrumbs + (folder.name to folder.id)
                                }
                        )
                    }

                    if (subFolders.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.move_dialog_empty_state), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.menu_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        // Usiamo la funzione passata dall'esterno per la validazione
                        onClick = {
                            if (isValidMove(currentFolderId)) {
                                onConfirm(currentFolderId)
                            }
                        },
                        enabled = isValidMove(currentFolderId)
                    ) {
                        Text(stringResource(R.string.move_dialog_action_move_here))
                    }
                }
            }
        }
    }
}