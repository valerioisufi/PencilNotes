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
    horizontalAlignment: Alignment.Horizontal? = null,
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

                do {
                    val event = awaitPointerEvent()

                    // Calcola lo zoom e il pan basandosi sul movimento di tutte le dita.
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()

                    // Se c'è un cambiamento, applica la trasformazione allo stato.
                    if (zoom != 1f || pan != Offset.Zero) {
                        coroutineScope.launch {
                            transformableState.applyTransform(event.calculateCentroid(), pan, zoom, this)
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
                // ...e avvia l'animazione di fling.
                coroutineScope.launch {
                    transformableState.fling(velocity, this)
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

            // resolve content paddings
            val startPadding =
                if (isVertical) {
                    contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateStartPadding(layoutDirection).roundToPx()
                }

            val endPadding =
                if (isVertical) {
                    contentPadding.calculateRightPadding(layoutDirection).roundToPx()
                } else {
                    // in horizontal configuration, padding is reversed by placeRelative
                    contentPadding.calculateEndPadding(layoutDirection).roundToPx()
                }
            val topPadding = contentPadding.calculateTopPadding().roundToPx()
            val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
            val totalVerticalPadding = topPadding + bottomPadding
            val totalHorizontalPadding = startPadding + endPadding
            val totalMainAxisPadding =
                if (isVertical) totalVerticalPadding else totalHorizontalPadding
            val beforeContentPadding =
                when {
                    isVertical -> topPadding
                    else -> startPadding
                }
            val afterContentPadding = totalMainAxisPadding - beforeContentPadding
            val contentConstraints =
                containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

            val spaceBetweenItemsDp =
                if (isVertical) {
                    verticalArrangement?.spacing ?: Arrangement.SpaceBetween.spacing
                } else {
                    horizontalArrangement?.spacing ?: Arrangement.SpaceBetween.spacing
                }
            val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

            val itemProvider = itemProviderLambda()

            val scale = transformableState.scale
            val offsetX = transformableState.offset.x
            val offsetY = transformableState.offset.y
            
            // Assume width is fixed to the container width (for vertical list)
            val itemWidth = contentConstraints.maxWidth
            var totalHeight = 0f
            val visiblePlaceables = mutableListOf<Pair<Placeable, IntOffset>>()

            // Iterate all items to calculate positions and finding visible ones
            // This is O(N), which is acceptable for typical document lengths.
            // A more advanced implementation could use a cached layout info structure.
            for (index in 0 until itemProvider.itemCount) {
                // Get pre-calculated size from metadata (Dimension)
                val dimension = itemProvider.getItemSize(index)
                val itemBaseHeight = dimension.calcHeightFromWidthPx(itemWidth.toFloat().px)
                
                // Calculate scaled position
                val itemHeightScaled = itemBaseHeight * scale
                val itemTopScaled = totalHeight * scale + offsetY
                val itemBottomScaled = itemTopScaled + itemHeightScaled
                
                // Check intersection with viewport
                // Viewport is 0..containerConstraints.maxHeight
                // Using a tolerance buffer
                val isVisible = itemBottomScaled >= -100 && itemTopScaled <= containerConstraints.maxHeight + 100

                if (isVisible) {
                    // Measure only if visible
                    // We measure with the SCALED constraints to ensure content (like text/strokes) renders at correct resolution
                    // We also clamp to avoid integer overflow or weird constraints if scale is huge
                    val scaledWidth = (itemWidth * scale).roundToInt().coerceAtLeast(1)
                    val scaledHeight = (itemHeightScaled).roundToInt().coerceAtLeast(1)
                    
                    val childConstraints = Constraints.fixed(scaledWidth, scaledHeight)
                    
                    val placeables = measure(index, childConstraints)
                    placeables.forEach { placeable ->
                         val finalX = offsetX.roundToInt() + startPadding // Add padding if needed
                         val finalY = itemTopScaled.roundToInt() + topPadding
                         visiblePlaceables.add(placeable to IntOffset(finalX, finalY))
                    }
                }
                
                totalHeight += itemBaseHeight + spaceBetweenItems
            }
            
            // Update state with the calculated total content size
            // Note: TransformableState expects size in base pixels (unscaled)?
            // Looking at TransformableState logic:
            // if (contentSize.width * scale > layoutSize.width) ...
            // So contentSize should be the UN-SCALED size.
            transformableState.onLayoutSizeChanged(
                IntSize(containerConstraints.maxWidth, containerConstraints.maxHeight),
                coroutineScope
            )
            transformableState.onContentSizeChanged(
                IntSize(itemWidth, totalHeight.roundToInt()),
                coroutineScope
            )

            layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                visiblePlaceables.forEach { (placeable, position) ->
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
        Dimension(200.mm, 50.mm),
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