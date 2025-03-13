package com.studiomath.pencilnotes.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
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
import com.studiomath.pencilnotes.ui.composeComponents.FileListComponent
import com.studiomath.pencilnotes.ui.theme.PencilNotesTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.studiomath.pencilnotes.ui.composeComponents.RequestNameDialog
import com.studiomath.pencilnotes.ui.composeComponents.isScrollingUp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the splash screen transition.
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

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

enum class NavigationBarItem(val value: Int) {
    HOME(0), SHARED(-1), FILE(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootActivity(modifier: Modifier = Modifier, fileExplorerViewModel: FileExplorerViewModel) {
    val mContext = LocalContext.current
//    val drawerState = rememberDrawerState(DrawerValue.Closed)
//    val scope = rememberCoroutineScope()
//
//    val items = listOf("Account", "Impostazioni")
//    val selectedItem = remember { mutableStateOf(items[0]) }

    val listState = rememberLazyListState()

    ModalNavigationDrawer(
//        drawerState = drawerState,
        drawerContent = {
//            ModalDrawerSheet(
//                drawerState = drawerState
//            ) {
//                /* Drawer content */
//                Column(Modifier.verticalScroll(rememberScrollState())) {
//                    Spacer(Modifier.height(12.dp))
//                    Text("PencilNotes", modifier = Modifier.padding(16.dp))
//                    items.forEach { item ->
//                        NavigationDrawerItem(
//                            icon = {
//                                when(item){
//                                    "Account" -> Icon(Icons.Filled.AccountCircle, contentDescription = null)
//                                    "Impostazioni" -> Icon(Icons.Filled.Settings, contentDescription = null)
//                                }
//                            },
//                            label = { Text(item) },
//                            selected = item == selectedItem.value,
//                            onClick = {
//                                scope.launch { drawerState.close() }
//                                selectedItem.value = item
//                            },
//                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
//                        )
//                    }
//                    HorizontalDivider()
//                }
//            }

        },
        gesturesEnabled = false
    ) {
        var navigationBarSelectedItem by remember { mutableIntStateOf(NavigationBarItem.FILE.value) }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AnimatedContent(
                    targetState = fileExplorerViewModel.currentDirectoryPath.value
                ){targetState ->
                    when(targetState){
                        "/" ->
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(),
                                title = {
                                    Row (
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp),
                                    ){
                                        Image(
                                            painter = painterResource(R.drawable.logo),
                                            contentDescription = "Localized description",
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .size(32.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            modifier = Modifier
                                                .padding(8.dp, 0.dp, 16.dp, 0.dp)
                                                .align(Alignment.CenterVertically),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontFamily = FontFamily(Font(resId = R.font.limelight, weight = FontWeight.W400, style = FontStyle.Normal))
                                        )
                                    }

                                },
                                navigationIcon = {
                                },
                                actions = {
                                    IconButton(onClick = {
                                        val intent = Intent(mContext, SettingsActivity::class.java)
                                        mContext.startActivity(intent)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                },
//                          scrollBehavior = scrollBehavior
                            )

                        else ->
                            TopAppBar(
                                title = {
                                    Text(
                                        text = if(fileExplorerViewModel.directorySequence.isNotEmpty()) fileExplorerViewModel.directorySequence.last() else "",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        fileExplorerViewModel.backFolder()
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                },
                                actions = {
                                }
                            )
                    }
                }



            },
            bottomBar = {
                val items = listOf(
                    stringResource(id = R.string.button_home),
//                    stringResource(id = R.string.button_shared),
                    stringResource(id = R.string.button_file)
                )

                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    when (index) {
                                        NavigationBarItem.HOME.value -> Icons.Filled.Home
                                        NavigationBarItem.SHARED.value -> Icons.Filled.Group
                                        NavigationBarItem.FILE.value -> Icons.Filled.Folder
                                        else -> Icons.Filled.Favorite
                                    },
                                    contentDescription = item
                                )
                            },
                            label = { Text(item) },
                            selected = navigationBarSelectedItem == index,
                            onClick = { navigationBarSelectedItem = index }
                        )
                    }
                }
            },
            floatingActionButton = {
                val sheetState = rememberModalBottomSheetState()
                var isSheetOpen by rememberSaveable {
                    mutableStateOf(false)
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        isSheetOpen = true
                    },
                    expanded = listState.isScrollingUp(),
                    icon = { Icon(Icons.Filled.Add, stringResource(id = R.string.button_new)) },
                    text = { Text(text = stringResource(id = R.string.button_new)) }
                )


                var openDialogNewFile by rememberSaveable { mutableStateOf(false) }
                if (openDialogNewFile) {
                    RequestNameDialog(
                        title = stringResource(id = R.string.button_newNote),
                        labelTextField = stringResource(id = R.string.request_name),
                        textConfirmButton = stringResource(id = R.string.button_confirm),
                        onDismissRequest = {openDialogNewFile = false},
                        onConfirm = { text ->
                            fileExplorerViewModel.createFile(
                                FileExplorerViewModel.FileType.FILE, text
                            )

                            openDialogNewFile = false
                        }
                    )
                }
                var openDialogNewFolder by rememberSaveable { mutableStateOf(false) }
                if (openDialogNewFolder) {
                    RequestNameDialog(
                        title = stringResource(id = R.string.menu_createFolder),
                        labelTextField = stringResource(id = R.string.request_name),
                        textConfirmButton = stringResource(id = R.string.button_confirm),
                        onDismissRequest = {openDialogNewFolder = false},
                        onConfirm = { text ->
                            fileExplorerViewModel.createFile(
                                FileExplorerViewModel.FileType.FOLDER, text
                            )
                            openDialogNewFolder = false
                        }
                    )
                }

                if (isSheetOpen) {
                    ModalBottomSheet(
                        sheetState = sheetState,
                        onDismissRequest = { isSheetOpen = false },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                Button(
                                    modifier = Modifier
                                        .height(64.dp),
                                    onClick = {
                                        isSheetOpen = false
                                        openDialogNewFile = true
                                    }
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Article, contentDescription = "")
                                    Text(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp),
                                        text = stringResource(id = R.string.button_file)
                                    )
                                }
                                OutlinedButton(
                                    modifier = Modifier
                                        .height(64.dp),
                                    onClick = {
                                        isSheetOpen = false
                                        openDialogNewFolder = true
                                    }
                                ) {
                                    Icon(imageVector = Icons.Filled.Folder, contentDescription = "")
                                    Text(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp),
                                        text = stringResource(id = R.string.button_folder)
                                    )
                                }

                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }


                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End,
        )
        { innerPadding ->
            Surface(modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding)
            ) {
                // Screen content
                AnimatedContent(
                    targetState = navigationBarSelectedItem
                ) { targetState ->
                    when (targetState) {
                        NavigationBarItem.HOME.value -> Text(text = "Home")
                        NavigationBarItem.SHARED.value -> Text(text = "Condivisi")
                        NavigationBarItem.FILE.value -> FileListComponent(
                            directoryFiles = fileExplorerViewModel.currentDirectoryFiles,
                            fileExplorerViewModel = fileExplorerViewModel,
                            listState = listState
                        )

                    }
                }
            }

        }
    }

}

