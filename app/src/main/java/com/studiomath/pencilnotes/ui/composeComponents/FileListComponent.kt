package com.studiomath.pencilnotes.ui.composeComponents

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp, 0.dp),
    ) {
        Row(
            modifier = Modifier
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expanded by remember { mutableStateOf(false) }
            val (checkedState, onStateChange) = remember { mutableStateOf(true) }

            TextButton(onClick = { expanded = true }) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp),
                    text = "Nome",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = Icons.Default.North,
                    contentDescription = "Ordine",
                    modifier = Modifier
                        .size(16.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_dateCreated)) },
                    onClick = { /* Handle edit! */ },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_lastModified)) },
                    onClick = { /* Handle settings! */ }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_lastOpen)) },
                    onClick = { /* Handle edit! */ },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_name)) },
                    onClick = { /* Handle settings! */ }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.menu_keepFoldersOnTop)) },
                    onClick = { onStateChange(!checkedState) },
                    leadingIcon = {
                        Checkbox(
                            checked = checkedState,
                            onCheckedChange = null // null recommended for accessibility with screenreaders
                        )
                    }
                )
            }
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
                    fileExplorerViewModel = fileExplorerViewModel
                )
            }
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }

        }

    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    dataFile: FileExplorerViewModel.Files,
    fileExplorerViewModel: FileExplorerViewModel,
) {
    val mContext = LocalContext.current
    Row(modifier = modifier
        .fillMaxWidth(1f)
        .background(
            Color.Transparent, shape = RoundedCornerShape(8.dp)
        )
        .clip(RoundedCornerShape(8.dp))
        .clickable {
            if (dataFile.type == FileExplorerViewModel.FileType.FILE) {
                val intent = Intent(mContext, DrawActivity::class.java)
                intent.putExtra(
                    "filePath",
                    "/documenti/${fileExplorerViewModel.currentDirectoryPath.value}${dataFile.name.value}.json"
                )
                mContext.startActivity(intent)
            } else if (dataFile.type == FileExplorerViewModel.FileType.FOLDER) {
                fileExplorerViewModel.enterFolder(dataFile.name.value)
            }
        }
        .padding(0.dp, 12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(32.dp),
            painter = if (dataFile.type == FileExplorerViewModel.FileType.FILE) painterResource(id = R.drawable.ruler) else painterResource(
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
            Text(
                text = "Ultima modifica: 12/12/2023",
                style = MaterialTheme.typography.bodySmall
            )
        }

        FileDetailsWithBottomSheet(dataFile, fileExplorerViewModel)


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
                        Text(
                            text = "Ultima modifica: 12/12/2023",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                OptionItem(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    text = stringResource(id = R.string.menu_rename),
                )
                OptionItem(
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    text = stringResource(id = R.string.menu_moveTo),
                )
                OptionItem(
                    icon = Icons.Filled.Delete,
                    text = stringResource(id = R.string.menu_delete),
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