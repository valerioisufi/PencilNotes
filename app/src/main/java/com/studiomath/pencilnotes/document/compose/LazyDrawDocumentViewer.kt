package com.studiomath.pencilnotes.document.compose

import android.graphics.Matrix
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix as ComposeMatrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.ink.authoring.compose.InProgressStrokes
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.document.compose.lazyDocument.LazyDocumentViewer
import com.studiomath.pencilnotes.document.compose.lazyDocument.detectDocumentGestures
import com.studiomath.pencilnotes.document.compose.lazyDocument.items
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberLazyDocumentViewerState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberTrasformableState

@Composable
fun LazyDrawDocumentViewer(
    modifier: Modifier = Modifier,
    drawViewModel: DrawViewModel
) {
    if (drawViewModel.data.isDocumentLoaded) {
        val state = rememberLazyDocumentViewerState()
        val transformableState = rememberTrasformableState()
        val coroutineScope = rememberCoroutineScope()

        // Arbitration Logic: Consume events in Initial pass to prevent InProgressStrokes
        // from receiving them when we want to Pan/Zoom.
        val arbitrationModifier = Modifier.pointerInput(drawViewModel.selectedTool) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                
                val isPanTool = drawViewModel.selectedTool == DrawViewModel.ToolUtilities.Tool.PAN
                
                if (isPanTool) {
                     down.consume()
                     while(true) {
                         val event = awaitPointerEvent(PointerEventPass.Initial)
                         event.changes.forEach { it.consume() }
                         if (event.changes.all { !it.pressed }) break
                     }
                } else {
                    var isZooming = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!isZooming && event.changes.size > 1) {
                            isZooming = true
                        }
                        if (isZooming) {
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .then(arbitrationModifier)
                .detectDocumentGestures(
                    transformableState = transformableState,
                    coroutineScope = coroutineScope,
                    shouldConsumeEvent = { event ->
                        val isPanTool = drawViewModel.selectedTool == DrawViewModel.ToolUtilities.Tool.PAN
                        if (isPanTool) {
                            true
                        } else {
                            event.changes.size > 1
                        }
                    }
                )
        ) {
            LazyDocumentViewer(
                state = state,
                transformableState = transformableState,
                modifier = Modifier.fillMaxSize(),
                enableGestures = false, // We handle gestures externally
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = drawViewModel.data.pagesState,
                    key = { page -> page.index },
                    itemSize = { page -> page.dimension!! }
                ) { page ->
                    PageComposable(
                        modifier = Modifier
                            .background(Color.White),
                        page = page,
                        pageMaker = drawViewModel.pageMaker
                    )
                }
            }
            
            // Calculate Matrix for InProgressStrokes
            // Screen = World * Scale + Offset
            // World = (Screen - Offset) / Scale
            val scale = transformableState.scale
            val offset = transformableState.offset
            
            // Note: transforming logic is critical.
            // Android Matrix post operations apply M' = T * M (Wait, post is M * T?)
            // setTranslate(tx, ty) -> M
            // postScale(sx, sy) -> S * M
            // We want to map P_screen to P_world.
            // P_world = (P_screen - Offset) * (1/Scale)
            // = P_screen * (1/Scale) - Offset * (1/Scale)
            // So: Translate(-Offset) -> Scale(1/Scale)
            
            val matrix = remember(scale, offset) {
                Matrix().apply {
                    postTranslate(-offset.x, -offset.y)
                    postScale(1/scale, 1/scale)
                }
            }

            InProgressStrokes(
                defaultBrush = drawViewModel.getActiveBrushForCompose(),
                pointerEventToWorldTransform = ComposeMatrix().apply { setFrom(matrix) },
                onStrokesFinished = { strokes ->
                    // Handle finished strokes
                    strokes.forEach { stroke ->
                        // Determine which page this stroke belongs to.
                        // We check the first input of the stroke.
                        // Stroke inputs are in "World" coordinates.
                        
                        val firstInput = stroke.inputs[0]
                        val strokeX = firstInput.x
                        val strokeY = firstInput.y
                        
                        // Find page containing this point
                        val pageInfo = state.layoutInfo.visibleItemsInfo.find { info ->
                            // Map Stroke Point (World) to Screen.
                            // P_screen = P_world * Scale + Offset.
                            val strokeScreenX = strokeX * scale + offset.x
                            val strokeScreenY = strokeY * scale + offset.y
                            
                            val pageScreenX = info.offset.x
                            val pageScreenY = info.offset.y
                            val pageWidth = info.size.width
                            val pageHeight = info.size.height
                            
                            strokeScreenX >= pageScreenX && strokeScreenX < pageScreenX + pageWidth &&
                            strokeScreenY >= pageScreenY && strokeScreenY < pageScreenY + pageHeight
                        }
                        
                        if (pageInfo != null) {
                            val page = drawViewModel.data.document.pages.find { it.index == pageInfo.index }
                            
                            if (page != null) {
                                // 1. Calculate Page Top-Left in World Coordinates
                                val pageWorldX = (pageInfo.offset.x - offset.x) / scale
                                val pageWorldY = (pageInfo.offset.y - offset.y) / scale
                                
                                // 2. Calculate Scale Factor between Layout (World) and Bitmap
                                // LayoutWidth (World) = ScreenWidth / Scale
                                val layoutWidthWorld = pageInfo.size.width.toFloat() / scale
                                
                                // BitmapWidth = Page Width(mm) -> Inch -> Pixels (at 150 DPI)
                                // We can use Dimension helper or manual calc.
                                // 1 Inch = 25.4mm
                                val bitmapWidth = (page.width / 25.4f * com.studiomath.pencilnotes.document.page.resolutionPxInchPageDefault).toInt()
                                
                                val scaleLayoutToBitmap = bitmapWidth.toFloat() / layoutWidthWorld
                                
                                // 3. Create Transformation Matrix
                                // World -> Local (Layout) -> Local (Bitmap)
                                val toBitmapMatrix = Matrix()
                                toBitmapMatrix.postTranslate(-pageWorldX, -pageWorldY)
                                toBitmapMatrix.postScale(scaleLayoutToBitmap, scaleLayoutToBitmap)
                                
                                drawViewModel.addStroke(
                                    pageIndex = pageInfo.index,
                                    stroke = stroke,
                                    transformToPage = toBitmapMatrix
                                )
                            }
                        }
                    }
                }
            )

        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
