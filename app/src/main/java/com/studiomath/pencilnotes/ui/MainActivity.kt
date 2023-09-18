package com.studiomath.pencilnotes.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.file.FileExplorerViewModel
import com.studiomath.pencilnotes.ui.theme.PencilNotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        val fileExplorerViewModel = viewModels<FileExplorerViewModel> {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FileExplorerViewModel(
                        nomeFile = "fileExplorerXml.xml", filesDir = filesDir
                    ) as T
                }
            }
        }.value

        val callback = onBackPressedDispatcher.addCallback {
            if (fileExplorerViewModel.backFolder() == null) finish()
        }

        setContent {
            PencilNotesTheme {
                RootActivity(fileExplorerViewModel = fileExplorerViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootActivity(modifier: Modifier = Modifier, fileExplorerViewModel: FileExplorerViewModel) {

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        CenterAlignedTopAppBar(colors = TopAppBarDefaults.topAppBarColors(), title = {
            Text(
                stringResource(R.string.app_name),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        }, navigationIcon = {
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Localized description"
                )
            }
        }, actions = {
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Localized description"
                )
            }
        }, scrollBehavior = scrollBehavior
        )
    }, floatingActionButton = {
        val sheetState = rememberModalBottomSheetState()
        var isSheetOpen by rememberSaveable {
            mutableStateOf(false)
        }

        FloatingActionButton(onClick = {
            isSheetOpen = true
        }) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }

        var openAlertDialog by rememberSaveable { mutableStateOf(false) }
        if (openAlertDialog) {
            Dialog(onDismissRequest = { openAlertDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "This is a minimal dialog",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        var text by remember { mutableStateOf("") }
                        OutlinedTextField(value = text,
                            onValueChange = { text = it },
                            label = { Text("Label") })
                        Button(onClick = {
                            fileExplorerViewModel.createFile(
                                FileExplorerViewModel.FileType.FILE, text
                            )
                            openAlertDialog = false
                        }) {
                            Text(text = "Crea")
                        }
                    }
                }
            }
        }

        if (isSheetOpen) {
            ModalBottomSheet(sheetState = sheetState,
                onDismissRequest = { isSheetOpen = false },
                dragHandle = { BottomSheetDefaults.DragHandle() }) {

                Row {

                    Button(onClick = {
                        isSheetOpen = false
                        openAlertDialog = true
                    }) {
                        Text(text = "file")
                    }
                    Button(onClick = {
                        fileExplorerViewModel.createFile(
                            FileExplorerViewModel.FileType.FOLDER, "folder"
                        )
                    }) {
                        Text(text = "folder")
                    }

                }
            }
        }
    }) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            FileManager(
                directoryFiles = fileExplorerViewModel.currentDirectoryFiles,
                fileExplorerViewModel = fileExplorerViewModel
            )
        }


    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileManager(
    modifier: Modifier = Modifier,
    directoryFiles: FileExplorerViewModel.DirectoryFiles,
    fileExplorerViewModel: FileExplorerViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(directoryFiles.filesList, key = {
            it.name
        }) {
            ListItem(
                Modifier.animateItemPlacement(),
                dataFile = it,
                fileExplorerViewModel = fileExplorerViewModel
            )
        }
    }
}


@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    dataFile: FileExplorerViewModel.Files,
    fileExplorerViewModel: FileExplorerViewModel
) {
    val mContext = LocalContext.current
    Row(modifier = modifier
        .fillMaxWidth(1f)
        .padding(16.dp, 0.dp)
        .background(
            MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)
        )
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
        .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier.size(40.dp),
            painter = if (dataFile.type == FileExplorerViewModel.FileType.FILE) painterResource(id = R.drawable.ruler) else painterResource(
                id = R.drawable.img_folder
            ),
            contentDescription = ""
        )
        Text(
            text = dataFile.name.value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
        Icon(Icons.Default.MoreVert, contentDescription = "Info")
    }
}