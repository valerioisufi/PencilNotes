package com.studiomath.pencilnotes.document.compose

import android.graphics.Matrix
import androidx.compose.ui.graphics.Matrix as ComposeMatrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.studiomath.pencilnotes.document.compose.lazyDocument.LazyDocumentViewer
import com.studiomath.pencilnotes.document.compose.lazyDocument.items

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.document.compose.lazyDocument.LazyDocumentViewer
import com.studiomath.pencilnotes.document.compose.lazyDocument.items
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberLazyDocumentViewerState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberTrasformableState
import com.studiomath.pencilnotes.document.compose.lazyDocument.detectDocumentGestures
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.compose.ui.input.pointer.PointerType

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
                
                val isPanTool = drawViewModel.selectedTool == DrawViewModel.ToolUtilities.Tool.PAN ||
                        drawViewModel.selectedTool == DrawViewModel.ToolUtilities.Tool.LAZO // Assuming Lazo might be pan-like or handled differently, but for now blocking Ink? No Lazo is selection.
                
                // If it's pure Pan tool, consume immediately
                if (isPanTool) {
                     // Consume the down and subsequent events in Initial pass
                     down.consume()
                     // Continue consuming the rest of the gesture
                     while(true) {
                         val event = awaitPointerEvent(PointerEventPass.Initial)
                         event.changes.forEach { it.consume() }
                         if (event.changes.all { !it.pressed }) break
                     }
                } else {
                    // It's a drawing tool (Pen, Highlighter, Eraser)
                    // We check for Multi-touch (Pinch to Zoom)
                    // Wait for a second pointer potentially?
                    // InProgressStrokes handles single pointer.
                    // If we have 2 pointers, we want to consume to trigger Zoom.
                    
                    // Simplification: Check down count or if Touch vs Stylus?
                    // Usually: Stylus -> Always Ink (unless Pan tool).
                    // Touch -> Ink if Pen Tool, but if 2 fingers -> Zoom.
                    
                    // We can't predict 2 fingers on first down.
                    // But if 2nd finger comes down, we should consume?
                    // But InProgressStrokes might have already started.
                    // If InProgressStrokes receives a consumed event later, it cancels the stroke (according to doc).
                    
                    var isZooming = false
                    
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        
                        if (!isZooming && event.changes.size > 1) {
                            isZooming = true
                        }
                        
                        if (isZooming) {
                            // If we detected zoom intent (multitouch), consume everything to cancel generic ink
                            // and feed the gesture detector (which sees consumed events).
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
                    coroutineScope = coroutineScope
                )
        ) {
            LazyDocumentViewer(
                state = state,
                transformableState = transformableState,
                modifier = Modifier.fillMaxSize(),
                enableGestures = false // We handle gestures externally
            ) {
                items(
                    items = drawViewModel.data.pagesState,
                    key = { page -> page.index },
                    itemSize = { page -> page.dimension!! }
                ) { page ->
                    PageComposable(
                        modifier = Modifier.background(Color.White),
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
                    // Map from World coordinates "strokes" to Page coordinates
                    
                    // Iterate strokes? No, list of strokes.
                    strokes.forEach { stroke ->
                        // Determine which page this stroke belongs to.
                        // We check the first input of the stroke.
                        // Note: stroke inputs are in "World" coordinates now because we passed the matrix.
                        
                        val firstInput = stroke.inputs[0] // Scratch object
                        val strokeX = firstInput.x
                        val strokeY = firstInput.y
                        
                        // Find page containing this point
                        val pageInfo = state.layoutInfo.visibleItemsInfo.find { info ->
                            // Info.offset is in Viewport pixels? 
                            // Wait. LazyLayout places items.
                            // layoutInfo.offset is the position returned by placeRelative?
                            // In LazyDocumentViewer, we place items at:
                            // x = finalCrossPos/finalMainPos...
                            // These are SCREEN coordinates (viewport coordinates).
                            
                            // BUT, we want WORLD coordinates.
                            // The items are placed in Screen coordinates.
                            // To check hit in World coordinates, we need Page Rect in World Coordinates.
                            
                            // Screen = World * Scale + Offset.
                            // ItemScreenPos = ItemWorldPos * Scale + Offset.
                            // ItemWorldPos = (ItemScreenPos - Offset) / Scale.
                            
                            // Effectively, the Page's position in World space.
                            // However, LazyLayout logic calculates positions dynamically based on scroll.
                            // In the layout logic:
                            // currentMainPos (Unscaled) -> World Pos roughly.
                            // We construct layoutInfo with "offset" being value passed to placeRelative (Screen Pos).
                            
                            // So let's map Stroke Point (World) to Screen.
                            // P_screen = P_world * Scale + Offset.
                            // Then checks if P_screen is inside Page's Screen Rect (offset, size).
                            
                            val pageScreenX = info.offset.x
                            val pageScreenY = info.offset.y
                            val pageWidth = info.size.width
                            val pageHeight = info.size.height
                            
                            val strokeScreenX = strokeX * scale + offset.x
                            val strokeScreenY = strokeY * scale + offset.y
                            
                            strokeScreenX >= pageScreenX && strokeScreenX < pageScreenX + pageWidth &&
                            strokeScreenY >= pageScreenY && strokeScreenY < pageScreenY + pageHeight
                        }
                        
                        if (pageInfo != null) {
                            // We found the page.
                            // Now we need to transform the stroke from World to Page-Local.
                            // Page-Local = World - PageWorldPos?
                            // Or simpler: Page-Local-In-Pixels?
                            // DrawDocumentData expects strokes in... what units?
                            // DrawViewModel.addStroke converts input...
                            
                            // Checking DrawDocumentData.Stroke:
                            // It stores inputs as x,y.
                            // It corresponds to the coordinate system of the Page.
                            // The Page size is defined in mm, but drawing operations are usually in pixels (bitmap size).
                            // The PageComposables render the bitmap.
                            
                            // We need to know what "0,0" means for the Page.
                            // Usually top-left of the page.
                            
                            // So we need: P_local = P_world - PageWorldPos.
                            // We know: P_screen = P_world * Scale + Offset
                            // And: PageScreenPos = PageWorldPos * Scale + Offset
                            // So: P_screen - PageScreenPos = (P_world - PageWorldPos) * Scale
                            // P_local * Scale = P_screen - PageScreenPos
                            // P_local = (P_screen - PageScreenPos) / Scale
                             
                            // Wait, if InProgressStrokes returns World coordinates, 
                            // and we successfully mapped them.
                            // We just need to subtract the Page's World Position from Stroke World Position.
                            
                            // PageWorldPos = (PageScreenPos - Offset) / Scale.
                            
                            val pageWorldX = (pageInfo.offset.x - offset.x) / scale
                            val pageWorldY = (pageInfo.offset.y - offset.y) / scale
                            
                            // Determine transformation matrix for World -> Local
                            val toLocalMatrix = Matrix()
                            toLocalMatrix.setTranslate(-pageWorldX, -pageWorldY)
                            
                            // Apply to stroke?
                            // Stroke is immutable?
                            // We need to transform the stroke.
                            // androidx.ink.strokes.Stroke usually has a transform method or we create a new one.
                            
                            // Actually, DrawDocumentData.Stroke inputs are simple data points.
                            // We can transform the inputs manually when converting.
                            // BUT androidx.ink.strokes.Stroke is complex (native handle).
                            // Better if we transform it using Ink API if available.
                            // OR we create a new Stroke with transformed inputs.
                            
                            // Is there Stroke.transform(Matrix)?
                            // If not, we rely on the fact that we are converting it to Serialized form anyway.
                            // Wait, DrawViewModel.addStroke calls `stroke.toSerializedStroke()` which uses `stroke.inputs`.
                            // So if we pass the World-Space stroke, it saves World-Space coordinates.
                            // But we want Page-Space.
                            
                            // Ideally we use `strokeToWorldTransform` in InProgressStrokes to handle this?
                            // No, that's for viewing.
                            
                            // We need access to transform APIs.
                            // If `androidx.ink.strokes.Stroke` doesn't have transform, we have to rebuild it.
                            // It has `inputs` (MutableStrokeInputBatch?). No `inputs` is StrokeInputBatch.
                            // We can iterate inputs, transform x/y, and build new batch.
                            
                            // Let's assume we can do this in DrawViewModel or here.
                            // Since we have the math here, let's process it here or pass the offset to ViewModel.
                            
                            // Pass page index and a Transformation Matrix to ViewModel?
                            // ViewModel's addStroke can apply the matrix.
                            
                            drawViewModel.addStroke(
                                pageIndex = pageInfo.index,
                                stroke = stroke,
                                transformToPage = toLocalMatrix // We need to add this arg
                            )
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
