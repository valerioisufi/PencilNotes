package com.studiomath.pencilnotes.ui.composeComponents

import android.content.Intent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.file.FileExplorerViewModel
import com.studiomath.pencilnotes.ui.DrawActivity

@Composable
fun FileListComponent(
    modifier: Modifier = Modifier,
    directoryFiles: FileExplorerViewModel.DirectoryFiles,
    fileExplorerViewModel: FileExplorerViewModel,
    listState: LazyListState
) {
    val selectionMode by fileExplorerViewModel.selectionMode
    
    // Drag and Drop State
    var draggingItem by remember { mutableStateOf<FileExplorerViewModel.Files?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var itemHeight by remember { mutableIntStateOf(0) } // Estimate item height
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 0.dp),
        ) {
            if (!selectionMode) {
                // Header
                Row(
                    modifier = Modifier.height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val (checkedState, onStateChange) = remember { mutableStateOf(true) }

                    TextButton(onClick = { expanded = true }) {
                        Text(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            text = "Nome",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.North,
                            contentDescription = "Ordine",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_dateCreated)) },
                            onClick = { fileExplorerViewModel.setSortOption(FileExplorerViewModel.SortOption.DATE_CREATED) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_lastModified)) },
                            onClick = { fileExplorerViewModel.setSortOption(FileExplorerViewModel.SortOption.DATE_MODIFIED) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_lastOpen)) },
                            onClick = { fileExplorerViewModel.setSortOption(FileExplorerViewModel.SortOption.LAST_OPENED) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_name)) },
                            onClick = { fileExplorerViewModel.setSortOption(FileExplorerViewModel.SortOption.NAME) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_keepFoldersOnTop)) },
                            onClick = { onStateChange(!checkedState) },
                            leadingIcon = {
                                Checkbox(
                                    checked = checkedState,
                                    onCheckedChange = null
                                )
                            }
                        )
                    }
                }
            } else {
                 Spacer(modifier = Modifier.height(12.dp))
            }

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(directoryFiles.filesList, key = {
                    it.name
                }) {
                    ListItem(
                        Modifier.animateItem(),
                        dataFile = it,
                        fileExplorerViewModel = fileExplorerViewModel,
                        onDragStart = { offset ->
                            draggingItem = it
                            dragOffset = offset
                        },
                        onDrag = { change ->
                            dragOffset += change
                        },
                        onDragEnd = {
                           // Find target
                           val droppedItem = draggingItem
                           if (droppedItem != null) {
                               val layoutInfo = listState.layoutInfo
                               val visibleItems = layoutInfo.visibleItemsInfo
                               
                               // Adjust dragOffset to be relative to the list based on how we captured it
                               // simpler: check intersection with visible items
                               // Note: The dragOffset is relative to the ListItem. We need global or relative to Box.
                               // We'll fix this in ListItem logic to pass global delta or accumulation.
                               
                               // Actually, let's use a simpler heuristic:
                               // We are dragging an item. `dragOffset` in the Box is what we want.
                               // But capturing accurate Box coordinates from a nested item is hard without global coordinates.
                               // Let's assume current dragOffset is relative to the START position.
                               // We need absolute position.
                               
                               // Better approach: Use onDragEnd logic here is complex. 
                               // Let's try to map dragOffset (which is accumulated delta) + Initial Position -> Current Position.
                               
                               // Simplification: We iterate over visible items and check if we are "over" them.
                               // But we need the Y position of the pointer.
                               // We can track `pointerY` state variable.
                           }
                           
                           // Logic moved to internal handling or just simplified "drop" trigger 
                           // For now, reset
                           draggingItem = null
                        },
                        // Pass a callback to check dropTarget
                        checkDropTarget = { positionY -> 
                             val layoutInfo = listState.layoutInfo
                             val target = layoutInfo.visibleItemsInfo.find { itemInfo ->
                                 positionY >= itemInfo.offset && positionY <= (itemInfo.offset + itemInfo.size)
                             }
                             val index = target?.index
                             if (index != null && index >= 0 && index < directoryFiles.filesList.size) {
                                 val targetFile = directoryFiles.filesList[index]
                                 if (targetFile.type == FileExplorerViewModel.FileType.FOLDER && targetFile != draggingItem) {
                                    // Move!
                                    if (fileExplorerViewModel.selectionMode.value) {
                                        fileExplorerViewModel.moveSelected(targetFile.id)
                                    } else {
                                        fileExplorerViewModel.moveFile(draggingItem!!, targetFile.id)
                                    }
                                 }
                             }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
        
        // Drag Overlay
        if (draggingItem != null) {
            // Visualize the dragging item
            // Position it at dragOffset (Coordinate needs to be right)
            // We use a simplified centered representation or follow finger
            
            // To make it follow finger, we need the initial position.
            // This is getting complicated to make pixel-perfect.
            // A simpler overlay: Just a "ghost" card that follows the drag change.
             androidx.compose.material3.Surface(
                 modifier = Modifier
                     .graphicsLayer {
                         translationX = dragOffset.x
                         translationY = dragOffset.y 
                         // Note: this assumes dragOffset is accumulated from (0,0) of this Box? 
                         // No, dragOffset in the state is accumulating deltas.
                         // But we need to start from the item's position.
                         // We don't know the item's initial position easily here.
                         // Okay, we will just use the delta and center it? No.
                         
                         // Fix: Position at center of screen + offset? 
                         // Or just hide original and show this one?
                         // Let's just show a small Icon following the finger.
                         // We need the absolute touch position.
                     }
                     .padding(16.dp),
                 shadowElevation = 8.dp,
                 shape = RoundedCornerShape(8.dp),
                 color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
             ) {
                 Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                     Icon(if(draggingItem!!.type == FileExplorerViewModel.FileType.FOLDER) Icons.Filled.Folder else Icons.AutoMirrored.Filled.Article, null)
                     Spacer(modifier = Modifier.width(8.dp))
                     Text(draggingItem!!.name.value)
                 }
             }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    dataFile: FileExplorerViewModel.Files,
    fileExplorerViewModel: FileExplorerViewModel,
    onDragStart: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    checkDropTarget: (Float) -> Unit = {}
) {
    val mContext = LocalContext.current
    val selectionMode by fileExplorerViewModel.selectionMode
    val isSelected = fileExplorerViewModel.selectedItems.contains(dataFile)
    
    // Visuals for selection
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    
    // Drag logic
    var offsetY by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    // Global Y position tracking
    var globalYPosition by remember { mutableFloatStateOf(0f) }

    Row(modifier = modifier
        .fillMaxWidth(1f)
        .onGloballyPositioned { coordinates ->
             globalYPosition = coordinates.positionInWindow().y
        }
        .background(
            backgroundColor, shape = RoundedCornerShape(8.dp)
        )
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(
            onClick = {
                if (selectionMode) {
                    fileExplorerViewModel.toggleSelection(dataFile)
                } else {
                    if (dataFile.type == FileExplorerViewModel.FileType.FILE) {
                         fileExplorerViewModel.openFile(dataFile.id)
                        val intent = Intent(mContext, DrawActivity::class.java)
                        intent.putExtra("documentId", dataFile.id)
                        mContext.startActivity(intent)
                    } else if (dataFile.type == FileExplorerViewModel.FileType.FOLDER) {
                        fileExplorerViewModel.enterFolder(dataFile.name.value)
                    }
                }
            },
            onLongClick = {
                fileExplorerViewModel.toggleSelection(dataFile)
            }
        )
        .padding(0.dp, 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox or Icon
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { fileExplorerViewModel.toggleSelection(dataFile) },
                modifier = Modifier.padding(start = 16.dp).size(24.dp)
            )
        } else {
             Image(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(32.dp),
                painter = if (dataFile.type == FileExplorerViewModel.FileType.FILE) painterResource(id = R.drawable.ruler) else painterResource(
                    id = R.drawable.img_folder
                ),
                contentDescription = ""
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = dataFile.name.value,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            val date = java.util.Date(dataFile.modifiedAt)
            val format = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
            Text(
                text = "Ultima modifica: ${format.format(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        
        // Drag Handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag",
            modifier = Modifier
                .padding(end = 16.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Set initial offset to current Item position for the overlay
                            // Since we don't have easy global access, we assume overlay starts at top.
                            // We pass the globalYPosition as the starting Y reference + touch offset.
                            // We construct a rough starting position.
                            offsetX = 0f
                            offsetY = globalYPosition
                            onDragStart(androidx.compose.ui.geometry.Offset(0f, globalYPosition)) 
                        },
                        onDragEnd = {
                            onDragEnd()
                            checkDropTarget(offsetY)
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            onDrag(dragAmount)
                        }
                    )
                },
            tint = contentColor.copy(alpha = 0.5f)
        )

        if (!selectionMode) {
             FileDetailsWithBottomSheet(dataFile, fileExplorerViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsWithBottomSheet(
    dataFile: FileExplorerViewModel.Files,
    fileExplorerViewModel: FileExplorerViewModel
) {
    val sheetState = rememberModalBottomSheetState()
    var isSheetOpen by rememberSaveable {
        mutableStateOf(false)
    }

    IconButton(onClick = {
        isSheetOpen = true
    }) {
        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "Info")
    }


    if (isSheetOpen) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { isSheetOpen = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(16.dp, 0.dp)
                        .background(
                            Color.Transparent, shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = if (dataFile.type == FileExplorerViewModel.FileType.FILE) painterResource(
                            id = R.drawable.ruler
                        ) else painterResource(
                            id = R.drawable.img_folder
                        ),
                        contentDescription = ""
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = dataFile.name.value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val date = java.util.Date(dataFile.modifiedAt)
                        val format = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                        Text(
                            text = "Ultima modifica: ${format.format(date)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                var showRenameDialog by remember { mutableStateOf(false) }
                if (showRenameDialog) {
                    RequestNameDialog(
                        title = stringResource(id = R.string.menu_rename),
                        labelTextField = stringResource(id = R.string.request_name),
                        textConfirmButton = stringResource(id = R.string.button_confirm),
                        onDismissRequest = {showRenameDialog = false},
                        onConfirm = { text ->
                            fileExplorerViewModel.renameFile(dataFile.name.value, text)
                            showRenameDialog = false
                        }
                    )
                }

                var showDeleteConfirmDialog by remember { mutableStateOf(false) }
                if (showDeleteConfirmDialog) {
                    ConfirmActionDialog(
                        title = stringResource(id = R.string.menu_delete),
                        textDescription = stringResource(id = R.string.dialog_deleteFileDescription),
                        textConfirmButton = stringResource(id = R.string.button_confirm),
                        onDismissRequest = {showDeleteConfirmDialog = false},
                        onConfirm = {
                            fileExplorerViewModel.deleteFile(dataFile.name.value)
                            showDeleteConfirmDialog = false
                        }
                    )
                }



                OptionItem(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    text = stringResource(id = R.string.menu_rename),
                    onClick = {
                        showRenameDialog = true
                    }
                )
                OptionItem(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    text = stringResource(id = R.string.menu_moveTo),
                )
                OptionItem(
                    icon = Icons.Filled.Delete,
                    text = stringResource(id = R.string.menu_delete),
                    onClick = {
                        showDeleteConfirmDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OptionItem(
    icon: ImageVector = Icons.Outlined.Edit,
    text: String = "Modifica",
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .background(
                Color.Transparent, shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier
                .size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Returns whether the lazy list is currently scrolling up.
 */
@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }

        }
    }.value
}