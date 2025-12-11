package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasurePolicy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.LazyDocumentViewerState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.TransformableState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberLazyDocumentViewerState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberTrasformableState
import com.studiomath.pencilnotes.document.page.Dimension
import com.studiomath.pencilnotes.document.page.px
import androidx.compose.ui.unit.Constraints
import com.studiomath.pencilnotes.document.page.mm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


/**
 * A lazy layout for displaying document pages vertically.
 *
 * This composable is designed for performance, only composing and laying out the items
 * that are currently visible on screen.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param transformableState The state object that can be used to control and observe the viewer's state.
 * @param content A block defining the items to be displayed.
 */
@Composable
fun LazyDocumentViewer(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    state: LazyDocumentViewerState = rememberLazyDocumentViewerState(),
    transformableState: TransformableState = rememberTrasformableState(),

    /** The inner padding to be added for the whole content(not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** The layout orientation of the list */
    isVertical: Boolean = true,
    /** Number of items to layout before and after the visible items */
    beyondBoundsItemCount: Int = 10,
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = Alignment.CenterHorizontally,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,

    /** The content of the list */
    content: LazyDocumentViewerScope.() -> Unit
) {
    val itemProviderLambda = rememberLazyDocumentViewerProvider(state, content)

    // NUOVO: Un CoroutineScope per lanciare le animazioni di fling e bounce
    // al di fuori della composizione.
    val coroutineScope = rememberCoroutineScope()
    val graphicsContext = LocalGraphicsContext.current

    // NUOVO: Il modifier per la gestione dei gesti viene applicato qui.
    val gestureModifier = Modifier.pointerInput(Unit) {
        // forEachGesture rileva l'inizio di un nuovo gesto (es. il primo dito che tocca lo schermo).
        forEachGesture {
            // awaitPointerEventScope ci permette di processare tutti gli eventi di un gesto
            // (dal primo dito giù all'ultimo dito su).
            awaitPointerEventScope {
                val velocityTracker = VelocityTracker()

                // Attendi il primo tocco
                awaitFirstDown(requireUnconsumed = false)

                // Variable to track if any significant zoom happened during the gesture
                var zoomOccurred = false

                do {
                    val event = awaitPointerEvent()

                    // Calcola lo zoom e il pan basandosi sul movimento di tutte le dita.
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()

                    // Se c'è un cambiamento, applica la trasformazione allo stato.
                    if (zoom != 1f || pan != Offset.Zero) {
                        if (zoom != 1f) zoomOccurred = true
                        coroutineScope.launch {
                            transformableState.applyTransform(event.calculateCentroid(), pan, zoom)
                        }
                    }

                    // Aggiungi gli eventi al velocity tracker per calcolare la velocità finale.
                    event.changes.forEach {
                        if (it.positionChanged()) {
                             velocityTracker.addPointerInputChange(it)
                        }
                    }

                } while (event.changes.any { it.pressed })

                // Quando l'utente solleva le dita, calcola la velocità...
                val velocity = velocityTracker.calculateVelocity()
                
                // ...e avvia l'animazione di fling SOLO se non abbiamo fatto zoom.
                // Se abbiamo fatto zoom, è probabile che il movimento delle dita abbia generato velocità spuria.
                if (!zoomOccurred) {
                    coroutineScope.launch {
                        transformableState.fling(velocity)
                    }
                } else {
                    // Se c'è stato zoom (e quindi potremmo essere fuori scala), assicuriamoci di "settle".
                     coroutineScope.launch {
                        transformableState.settle()
                    }
                }
            }
        }
    }

    val measurePolicy =
        rememberLazyDocumentViewerMeasurePolicy(
            itemProviderLambda,
            state,
            transformableState,
            contentPadding,
            isVertical,
            beyondBoundsItemCount,
            horizontalAlignment,
            verticalAlignment,
            horizontalArrangement,
            verticalArrangement,
            coroutineScope,
            graphicsContext,
        )

    LazyLayout(
        // Applica il modifier per i gesti insieme a quello passato dall'esterno.
        modifier = modifier.then(gestureModifier),
        itemProvider = itemProviderLambda,
        measurePolicy = measurePolicy,
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberLazyDocumentViewerMeasurePolicy(
    /** Items provider of the list. */
    itemProviderLambda: () -> LazyDocumentViewerProvider,
    /** The state of the list. */
    state: LazyDocumentViewerState,
    transformableState: TransformableState,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** Number of items to layout before and after the visible items */
    beyondBoundsItemCount: Int,
    /** The alignment to align items horizontally */
    horizontalAlignment: Alignment.Horizontal?,
    /** The alignment to align items vertically */
    verticalAlignment: Alignment.Vertical?,
    /** The horizontal arrangement for items */
    horizontalArrangement: Arrangement.Horizontal?,
    /** The vertical arrangement for items */
    verticalArrangement: Arrangement.Vertical?,
    /** Scope for animations */
    coroutineScope: CoroutineScope,
    /** Used for creating graphics layers */
    graphicsContext: GraphicsContext
) =
    remember(
        itemProviderLambda,
        state,
        transformableState,
        contentPadding,
        isVertical,
        beyondBoundsItemCount,
        horizontalAlignment,
        verticalAlignment,
        horizontalArrangement,
        verticalArrangement,
        coroutineScope,
        graphicsContext
    ){
        LazyLayoutMeasurePolicy { containerConstraints ->

            // 1. Resolve Padding
            // Calculate padding in px
            val startPadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
            val endPadding = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
            val topPadding = contentPadding.calculateTopPadding().roundToPx()
            val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()

            val verticalPadding = topPadding + bottomPadding
            val horizontalPadding = startPadding + endPadding

            // 2. Resolve Spacing
            val spaceBetweenItemsDp = if (isVertical) {
                verticalArrangement?.spacing ?: Arrangement.spacedBy(0.dp).spacing
            } else {
                horizontalArrangement?.spacing ?: Arrangement.spacedBy(0.dp).spacing
            }
            val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

            val itemProvider = itemProviderLambda()

            // 3. Current Transform State
            val currentScale = transformableState.scale
            val currentOffset = transformableState.offset
            val offsetX = currentOffset.x
            val offsetY = currentOffset.y

            // 4. Calculate Base Scale (Pixels per mm)
            // Determine container limits for "Fit Width" (or "Fit Height") logic
            // Constraint minus padding in the CROSS axis
            val crossAxisContainerSize = if (isVertical) {
                containerConstraints.maxWidth - horizontalPadding
            } else {
                containerConstraints.maxHeight - verticalPadding
            }
            
            val pixelsPerMm = if (itemProvider.itemCount > 0) {
                val firstItemDimension = itemProvider.getItemSize(0)
                val referenceDimensionMm = if (isVertical) firstItemDimension.width.mm else firstItemDimension.height.mm
                
                if (referenceDimensionMm > 0) {
                    crossAxisContainerSize.toFloat() / referenceDimensionMm
                } else {
                    1f
                }
            } else {
                1f
            }

            // 5. Layout Calculation
            var totalMainAxisSize = 0f
            var maxCrossAxisSize = 0
            
            // To store placeables to be placed
            val visiblePlaceables = mutableListOf<Triple<Placeable, IntOffset, Int>>() // Placeable, Position, Index

            // Viewport bounds in CONTENT coordinates (scaled)
            // The viewport is at (0,0) to (maxWidth, maxHeight) relative to the container.
            // But content is shifted by offset.
            
            // 5a. First Pass: Calculate Total Size and Bounds
            // Iterate all items to calculate positions 
            
            // We need to identify indices to measure
            var firstVisibleIndex = -1
            var lastVisibleIndex = -1
            
            // Current position in MAIN axis (unscaled pixels)
            var currentMainPos = 0f
            
            val mainAxisOffset = if (isVertical) offsetY else offsetX
            val containerMainSize = if (isVertical) containerConstraints.maxHeight else containerConstraints.maxWidth

            for (i in 0 until itemProvider.itemCount) {
                val dim = itemProvider.getItemSize(i)
                val itemSizeMainMm = if (isVertical) dim.height.mm else dim.width.mm
                val itemSizeCrossMm = if (isVertical) dim.width.mm else dim.height.mm

                val itemSizeMainPx = itemSizeMainMm * pixelsPerMm
                val itemSizeCrossPx = itemSizeCrossMm * pixelsPerMm
                
                // Track max cross axis size
                if (itemSizeCrossPx > maxCrossAxisSize) {
                    maxCrossAxisSize = itemSizeCrossPx.roundToInt()
                }

                val scaledMainSize = itemSizeMainPx * currentScale
                
                // Screen Position
                val start = currentMainPos * currentScale + mainAxisOffset
                val end = start + scaledMainSize
                
                // Visibility Check
                if (end >= -100 && start <= containerMainSize + 100) {
                    if (firstVisibleIndex == -1) firstVisibleIndex = i
                    lastVisibleIndex = i
                }
                
                currentMainPos += itemSizeMainPx + spaceBetweenItems
            }
            
            totalMainAxisSize = currentMainPos
            // Remove last spacing
            if (itemProvider.itemCount > 0) {
                 totalMainAxisSize -= spaceBetweenItems
            }

            // 5b. Second Pass: Measure and Place
            // Determine range to measure
            val rangeStart = (firstVisibleIndex - beyondBoundsItemCount).coerceAtLeast(0)
            val rangeEnd = (lastVisibleIndex + beyondBoundsItemCount).coerceAtMost(itemProvider.itemCount - 1)
            
            // Reset position for second pass
            // Optimization: We could have stored positions, but re-calculating is cheap O(N).
            // Better: Just loop from 0 to rangeEnd. 
            // If rangeStart is large, looping from 0 is wasteful?
            // Yes, but we need correct 'currentMainPos'. 
            // Unless we store "position of index i"? 
            // Let's assume linear accumulation is fast enough. 
            // (If items have different sizes, we MUST iterate or cache. Since sizes are dynamic from provider...)
            
            currentMainPos = 0f
            
            if (firstVisibleIndex != -1) {
                 for (i in 0..rangeEnd) {
                     val dim = itemProvider.getItemSize(i)
                     val mainMm = if (isVertical) dim.height.mm else dim.width.mm
                     val crossMm = if (isVertical) dim.width.mm else dim.height.mm
                     
                     val mainPx = mainMm * pixelsPerMm
                     val crossPx = crossMm * pixelsPerMm
                     
                     if (i >= rangeStart) {
                         // Measure and Place
                         val scaledMainInt = (mainPx * currentScale).roundToInt().coerceAtLeast(1)
                         val scaledCrossInt = (crossPx * currentScale).roundToInt().coerceAtLeast(1)
                         
                         val childConstraints = Constraints.fixed(
                             width = if (isVertical) scaledCrossInt else scaledMainInt,
                             height = if (isVertical) scaledMainInt else scaledCrossInt
                         )
                         
                         val placeables = measure(i, childConstraints) 
                         
                         // Calculate Position
                         // Main Axis Position (Screen coords)
                         val mainAxisScreenPos = (currentMainPos * currentScale + mainAxisOffset).roundToInt()
                         
                         // Add start padding to main axis position
                         val paddingMain = if (isVertical) topPadding else startPadding
                         val finalMainPos = mainAxisScreenPos + paddingMain
                         
                         // Cross Axis Position (Alignment)
                         // Available space in cross axis = Container - CrossPadding
                         val paddingBeforeCross = if (isVertical) startPadding else topPadding
                         val paddingAfterCross = if (isVertical) endPadding else bottomPadding
                         
                         val availableCrossSpace = if (isVertical) containerConstraints.maxWidth else containerConstraints.maxHeight
                         val actualCrossSpace = availableCrossSpace - paddingBeforeCross - paddingAfterCross
                         
                         placeables.forEach { p ->
                             // Alignment
                             val crossAxisPos = if (isVertical) {
                                 // Horizontal Alignment
                                 val align = horizontalAlignment ?: Alignment.Start
                                 val alignedX = align.align(p.width, actualCrossSpace, layoutDirection)
                                 alignedX + paddingBeforeCross
                             } else {
                                 // Vertical Alignment
                                 val align = verticalAlignment ?: Alignment.Top
                                 val alignedY = align.align(p.height, actualCrossSpace)
                                 alignedY + paddingBeforeCross
                             }
                             
                             val x = if (isVertical) crossAxisPos else finalMainPos
                             val y = if (isVertical) finalMainPos else crossAxisPos
                             
                             visiblePlaceables.add(Triple(p, IntOffset(x, y), i))
                         }
                     }
                     
                     currentMainPos += mainPx + spaceBetweenItems
                 }
            }

            // 6. Report Size to State
            // totalMainAxisSize is sum of items + spacing (unscaled base pixels).
            // We should add padding to the CONTENT SIZE? 
            // In standard views, padding is part of content size?
            // "Padding is space around content".
            // If I scroll to top, I see padding.
            // If I scroll to bottom, I see padding.
            // TransformableState manages offset bounds.
            // MinOffset = LayoutSize - ContentSize.
            // If ContentSize include padding => We can scroll further.
            
            val paddingMainTotal = if (isVertical) verticalPadding else horizontalPadding
            val totalMainWithPadding = totalMainAxisSize + paddingMainTotal
            
            // Cross axis size?
            // Should be max item size + padding? Or container size?
            // If we want to allow panning in cross axis if zoomed in?
            // Content Size width => zoomed width.
            // The item width is already calculated to fit container (pixelsPerMm).
            // So UNZOOOMED content width = container width (approx).
            // But padding adds to it? 
            // If container is 1000px, padding 100px. Item is 900px.
            // Total width 1000px.
            
            // Just use maxCrossAxisSize.
            // Wait, maxCrossAxisSize was calculated as item size.
            // We need to add cross axis padding to it?
            val paddingCrossTotal = if (isVertical) horizontalPadding else verticalPadding
            val totalCrossWithPadding = maxCrossAxisSize + paddingCrossTotal
            
            // Content Size for TransformableState.
            // Swapped if horizontal.
            // If Vertical: Width = Cross, Height = Main.
            // If Horizontal: Width = Main, Height = Cross.
            
            val contentWidth = if (isVertical) totalCrossWithPadding.toFloat() else totalMainWithPadding
            val contentHeight = if (isVertical) totalMainWithPadding else totalCrossWithPadding.toFloat()
            
            transformableState.onLayoutSizeChanged(
                IntSize(containerConstraints.maxWidth, containerConstraints.maxHeight),
                coroutineScope
            )
            transformableState.onContentSizeChanged(
                IntSize(contentWidth.roundToInt(), contentHeight.roundToInt()),
                coroutineScope
            )

            layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                visiblePlaceables.forEach { (placeable, position, _) ->
                    placeable.placeRelative(position)
                }
            }

        }
    }

@Preview
@Composable
fun LazyDocumentViewerPreview() {
    var list by remember { mutableStateOf(listOf<String>("allora", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi", "oppure", "quindi")) }

    var listSize by remember { mutableStateOf(listOf(
        IntSize(400, 200),
        IntSize(400, 100),
        IntSize(200, 50),
        IntSize(700, 100),
        IntSize(400, 10),
        IntSize(400, 800),
        IntSize(200, 300),
        IntSize(700, 100),
    )) }

    var listDimension by remember { mutableStateOf(listOf(
        Dimension(400.mm, 200.mm),
        Dimension(400.mm, 100.mm),
        Dimension(200.mm, 100.mm),
    )) }

    LazyDocumentViewer {
        items(
            items = listDimension,
            itemSize = { it }
        ) { item ->
            Spacer(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color(0xFFCCCCCC))
            )
        }
    }
}