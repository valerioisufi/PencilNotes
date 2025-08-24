package com.studiomath.pencilnotes.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.waterfall
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studiomath.pencilnotes.document.DrawComponent
import com.studiomath.pencilnotes.document.DrawManager
import com.studiomath.pencilnotes.document.DrawManager.DrawAttachments
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.document.DrawViewModel.ToolUtilities
import com.studiomath.pencilnotes.document.page.Dimension
import com.studiomath.pencilnotes.document.page.DrawDocumentData.Page
import com.studiomath.pencilnotes.document.page.pt
import com.studiomath.pencilnotes.ui.composeComponents.ColorWheel
import com.studiomath.pencilnotes.ui.composeComponents.SizeSlider
import com.studiomath.pencilnotes.ui.theme.PencilNotesTheme
import com.studiomath.pencilnotes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.apply
import kotlin.let

class DrawActivity : ComponentActivity() {
    private lateinit var inProgressStrokesView: InProgressStrokesView
    private lateinit var drawViewModel: DrawViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Get the WindowInsetsControllerCompat
        var windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure behavior and visibility
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val intent = intent
        val filePath = intent.getStringExtra("filePath")

        drawViewModel = viewModels<DrawViewModel> {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DrawViewModel(
                        filePath = "$filePath",
                        filesDir = filesDir,
                        displayMetrics = resources.displayMetrics,
                        configuration = ViewConfiguration.get(this@DrawActivity),
                        context = this@DrawActivity
                    ) as T
                }
            }
        }.value

        inProgressStrokesView = InProgressStrokesView(this)
        inProgressStrokesView.addFinishedStrokesListener(drawViewModel.drawManager)
        inProgressStrokesView.eagerInit()


        setContent {
            PencilNotesTheme {
                DrawActivity(drawViewModel = drawViewModel, inProgressStrokesView = inProgressStrokesView, windowInsetsController = windowInsetsController)
            }
        }

        drawViewModel.finishActivity = { finish() }
    }

    override fun onPause() {
        super.onPause()

        drawViewModel.data.saveDocument()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun DrawActivity(
    modifier: Modifier = Modifier,
    drawViewModel: DrawViewModel,
    inProgressStrokesView: InProgressStrokesView,
    windowInsetsController: WindowInsetsControllerCompat? = null
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        Surface(
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                    ) {
                        ToolButton(
                            onClick = {
                                drawViewModel.finishActivity?.let { it() }
                            }
                        ){
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                        ToolButton{
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = "Grid View",
                            )
                        }
                    }

                    Row (
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 4.dp)
                            .height(36.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            modifier = Modifier,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 1.dp),
                            onClick = {}
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(end = 8.dp),
                                text = "documento di prova",
                                style = MaterialTheme.typography.titleMedium,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier
                                    .requiredSize(20.dp)
                            )

                        }
                    }

                    Row(
                        modifier = Modifier
                    ) {
                        ToolButton{
                            Icon(
                                imageVector = Icons.Outlined.Draw,
                                contentDescription = "Draw",
                            )
                        }
                        ToolButton{
                            Icon(
                                imageVector = Icons.Outlined.MoreHoriz,
                                contentDescription = "More options",
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolButton(
                        onClick = {
                            drawViewModel.data.removePage()
                        }

                    ){
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Undo",
                        )
                    }
                    ToolButton(
                        onClick = {
                            drawViewModel.data.addPage(
                                Page(1).apply {
                                    dimension = Dimension.A4()
                                    width = dimension!!.width.mm
                                    height = dimension!!.height.mm
                                }
                            )

                        }

                    ){
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = "Redo",
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .padding(8.dp),
                        thickness = 2.dp
                    )

                    var penSettingsExpanded by remember { mutableStateOf(false) }
                    ToolButton(
                        onClick = {
                            if (drawViewModel.selectedTool == ToolUtilities.Tool.INK_PEN) {
                                penSettingsExpanded = true
                            } else {
                                drawViewModel.activeBrush = drawViewModel.penTool.getBrush(0)
                                drawViewModel.selectedTool = ToolUtilities.Tool.INK_PEN
                            }
                        },
                        onLongClick = {
                            drawViewModel.activeBrush = drawViewModel.penTool.getBrush(0)
                            drawViewModel.selectedTool = ToolUtilities.Tool.INK_PEN
                            penSettingsExpanded = true
                        },
                        selected = drawViewModel.selectedTool == ToolUtilities.Tool.INK_PEN,
                        dropDownMenu = {
                            var size by remember { mutableFloatStateOf(drawViewModel.activeBrush.size) }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Seleziona il colore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                ColorWheel(
                                    color = Color(drawViewModel.activeBrush.colorIntArgb),
                                    onColorChanged = {
                                        drawViewModel.activeBrush = drawViewModel.activeBrush.copyWithColorIntArgb(
                                            colorIntArgb = it.toArgb()
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(text = "Dimensione pennello", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                SizeSlider(
                                    modifier = Modifier.padding(8.dp),
                                    size = size.pt,
                                    onSizeChanged = {
                                        size = it.pt
                                        drawViewModel.activeBrush = drawViewModel.activeBrush.copy(
                                            size = it.pt
                                        )
                                    }
                                )
                            }
                        },
                        expanded = penSettingsExpanded,
                        onDismissRequest = { penSettingsExpanded = false },
                        windowInsetsController = windowInsetsController
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_ink_pen),
                            contentDescription = "Ink Pen",
                        )
                    }

                    var highlighterSettingsExpanded by remember { mutableStateOf(false) }
                    ToolButton(
                        onClick = {
                            if (drawViewModel.selectedTool == ToolUtilities.Tool.INK_HIGHLIGHTER) {
                                highlighterSettingsExpanded = true
                            } else {
                                drawViewModel.activeBrush = drawViewModel.highlighterTool.getBrush(0)
                                drawViewModel.selectedTool = ToolUtilities.Tool.INK_HIGHLIGHTER
                            }
                        },
                        onLongClick = {
                            drawViewModel.activeBrush = drawViewModel.highlighterTool.getBrush(0)
                            drawViewModel.selectedTool = ToolUtilities.Tool.INK_HIGHLIGHTER
                            highlighterSettingsExpanded = true
                        },
                        selected = drawViewModel.selectedTool == ToolUtilities.Tool.INK_HIGHLIGHTER,
                        dropDownMenu = {
                            var size by remember { mutableFloatStateOf(drawViewModel.activeBrush.size) }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Seleziona il colore evidenziatore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                ColorWheel(
                                    color = Color(drawViewModel.activeBrush.colorIntArgb),
                                    onColorChanged = {
                                        drawViewModel.activeBrush = drawViewModel.activeBrush.copyWithColorIntArgb(
                                            colorIntArgb = it.toArgb()
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(text = "Dimensione evidenziatore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                SizeSlider(
                                    modifier = Modifier.padding(8.dp),
                                    size = size.pt,
                                    onSizeChanged = {
                                        size = it.pt
                                        drawViewModel.activeBrush = drawViewModel.activeBrush.copy(
                                            size = it.pt
                                        )
                                    }
                                )
                            }
                        },
                        expanded = highlighterSettingsExpanded,
                        onDismissRequest = { highlighterSettingsExpanded = false },
                        windowInsetsController = windowInsetsController
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_ink_highlighter),
                            contentDescription = "Ink Highlighter",
                        )
                    }

                    var eraserSettingsExpanded by remember { mutableStateOf(false) }
                    ToolButton(
                        onClick = {
                            if (drawViewModel.selectedTool == ToolUtilities.Tool.ERASER) {
                                eraserSettingsExpanded = true
                            } else {
                                drawViewModel.activeBrush = drawViewModel.eraserTool.getBrush(0)
                                drawViewModel.selectedTool = ToolUtilities.Tool.ERASER
                            }
                        },
                        onLongClick = {
                            drawViewModel.activeBrush = drawViewModel.eraserTool.getBrush(0)
                            drawViewModel.selectedTool = ToolUtilities.Tool.ERASER
                            eraserSettingsExpanded = true
                        },
                        selected = drawViewModel.selectedTool == ToolUtilities.Tool.ERASER,
                        dropDownMenu = {
                            var size by remember { mutableFloatStateOf(drawViewModel.activeBrush.size) }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Dimensione gomma", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                SizeSlider(
                                    modifier = Modifier.padding(8.dp),
                                    size = size.pt,
                                    onSizeChanged = {
                                        size = it.pt
                                        drawViewModel.activeBrush = drawViewModel.activeBrush.copy(
                                            size = it.pt
                                        )
                                    }
                                )
                            }
                        },
                        expanded = eraserSettingsExpanded,
                        onDismissRequest = { eraserSettingsExpanded = false },
                        windowInsetsController = windowInsetsController
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_ink_eraser),
                            contentDescription = "Eraser",
                        )
                    }

                    ToolButton(
                        onClick = {
                            drawViewModel.activeBrush = drawViewModel.lazoTool.getBrush(0)
                            drawViewModel.selectedTool = ToolUtilities.Tool.LAZO
                        },
                        selected = drawViewModel.selectedTool == ToolUtilities.Tool.LAZO
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_lasso_select),
                            contentDescription = "Lasso Select",
                        )
                    }

                    ToolButton(
                        onClick = {
                            drawViewModel.selectedTool = ToolUtilities.Tool.TEXT
                        },
                        selected = drawViewModel.selectedTool == ToolUtilities.Tool.TEXT
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_text_fields),
                            contentDescription = "Text Field",
                        )
                    }

                    ToolButton(
                        onClick = {
                            drawViewModel.selectedTool = ToolUtilities.Tool.PAN
                        },
                        selected = drawViewModel.selectedTool == ToolUtilities.Tool.PAN
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_pan_tool),
                            contentDescription = "Pan Tool",
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .padding(8.dp),
                        thickness = 2.dp
                    )


                }

                HorizontalDivider()
            }


        }


        DrawComponent(
            drawViewModel = drawViewModel,
            inProgressStrokesView = inProgressStrokesView
        )
    }

}


@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun ToolButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit) = {},
    selected: Boolean = false,
    enabled: Boolean = true,
    dropDownMenu: @Composable() () -> Unit = {},
    expanded: Boolean = false,
    onDismissRequest: () -> Unit = {},
    windowInsetsController: WindowInsetsControllerCompat? = null,
    content: @Composable() RowScope.() -> Unit = {}
){
    Box{
        val selectedModifier = if (selected) {
            modifier.background(MaterialTheme.colorScheme.primaryContainer)
        } else {
            modifier
        }
        Row (
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { onLongClick() },
                    enabled = enabled,
                    role = Role.Button,
                )
                .then(selectedModifier)
                .padding(8.dp),
        ){
            content()
        }
        DropdownMenu(
            modifier = Modifier
                .width(300.dp),
            expanded = expanded,
            onDismissRequest = { onDismissRequest() }
        ) {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            dropDownMenu()
        }

    }

}