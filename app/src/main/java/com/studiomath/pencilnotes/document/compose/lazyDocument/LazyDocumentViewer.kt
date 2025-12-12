package com.studiomath.pencilnotes.document.compose.lazyDocument

import android.util.Log
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
import androidx.compose.ui.unit.LayoutDirection
import com.studiomath.pencilnotes.document.page.mm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.LazyDocumentItemInfo
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.LazyDocumentLayoutInfo
import kotlin.math.roundToInt

class DefaultLazyDocumentLayoutInfo(
    override val visibleItemsInfo: List<LazyDocumentItemInfo>,
    override val viewportSize: IntSize
) : LazyDocumentLayoutInfo

class DefaultLazyDocumentItemInfo(
    override val index: Int,
    override val offset: IntOffset,
    override val size: IntSize,
    override val sizeMm: IntSize
) : LazyDocumentItemInfo



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

    /** Whether gestures are enabled by default by this composable */
    enableGestures: Boolean = true,

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
    // Use the extracted gesture detector
    val gestureModifier = if (enableGestures) {
        Modifier.detectDocumentGestures(
            transformableState = transformableState,
            coroutineScope = coroutineScope
        )
    } else {
        Modifier
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

            // 3. Resolve Alignment Bias & Set State
            // We need to map Alignment to a float (0..1)
            // Helper function to probe alignment
            fun getAlignmentBias(horizontal: Alignment.Horizontal?, vertical: Alignment.Vertical?): Pair<Float, Float> {
                 val hBias = if (horizontal != null) {
                     // Hack: probe logic assuming standard alignments
                     val probed = horizontal.align(0, 1000, LayoutDirection.Ltr)
                     probed / 1000f
                 } else 0.5f // Default to Center if null? Or Start? Current default in params is CenterHorizontally
                 
                 val vBias = if (vertical != null) {
                     val probed = vertical.align(0, 1000)
                     probed / 1000f
                 } else 0.5f 
                 
                 return hBias to vBias
            }
            
            val (hBias, vBias) = getAlignmentBias(horizontalAlignment, verticalAlignment)
            // Update state with alignment.
            transformableState.setAlignment(hBias, vBias)


            // 4. Current Transform State
            val currentScale = transformableState.scale
            val currentOffset = transformableState.offset
            val offsetX = currentOffset.x
            val offsetY = currentOffset.y

            // 5. Calculate Base Scale (Pixels per mm)
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

            // 6. Layout Calculation
            var totalMainAxisSize = 0f
            var maxCrossAxisSize = 0
            
            val visiblePlaceables = mutableListOf<Triple<Placeable, IntOffset, Int>>() 

            // Viewport bounds in CONTENT coordinates (scaled)
            
            // 6a. First Pass: Calculate Total Size and Bounds
            var firstVisibleIndex = -1
            var lastVisibleIndex = -1
            
            var currentMainPos = 0f
            
            val mainAxisOffset = if (isVertical) offsetY else offsetX
            val containerMainSize = if (isVertical) containerConstraints.maxHeight else containerConstraints.maxWidth

            for (i in 0 until itemProvider.itemCount) {
                val dim = itemProvider.getItemSize(i)
                val itemSizeMainMm = if (isVertical) dim.height.mm else dim.width.mm
                val itemSizeCrossMm = if (isVertical) dim.width.mm else dim.height.mm

                val itemSizeMainPx = itemSizeMainMm * pixelsPerMm
                val itemSizeCrossPx = itemSizeCrossMm * pixelsPerMm
                
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
            if (itemProvider.itemCount > 0) {
                 totalMainAxisSize -= spaceBetweenItems
            }

            // 6b. Second Pass: Measure and Place
            val rangeStart = (firstVisibleIndex - beyondBoundsItemCount).coerceAtLeast(0)
            val rangeEnd = (lastVisibleIndex + beyondBoundsItemCount).coerceAtMost(itemProvider.itemCount - 1)
            
            // Reset scan
            currentMainPos = 0f
            
            if (firstVisibleIndex != -1) {
                 for (i in 0..rangeEnd) {
                     val dim = itemProvider.getItemSize(i)
                     val mainMm = if (isVertical) dim.height.mm else dim.width.mm
                     val crossMm = if (isVertical) dim.width.mm else dim.height.mm
                     
                     val mainPx = mainMm * pixelsPerMm
                     val crossPx = crossMm * pixelsPerMm
                     
                     if (i >= rangeStart) {
                         // Measure
                         val scaledMainInt = (mainPx * currentScale).roundToInt().coerceAtLeast(1)
                         val scaledCrossInt = (crossPx * currentScale).roundToInt().coerceAtLeast(1)
                         
                         val childConstraints = Constraints.fixed(
                             width = if (isVertical) scaledCrossInt else scaledMainInt,
                             height = if (isVertical) scaledMainInt else scaledCrossInt
                         )

                         val placeables = measure(i, childConstraints) 
                         
                         // Calculate Position
                         // Main Axis Position 
                         val mainAxisScreenPos = (currentMainPos * currentScale + mainAxisOffset).roundToInt()
                         val paddingMain = if (isVertical) topPadding else startPadding
                         val finalMainPos = mainAxisScreenPos + paddingMain
                         
                         // Cross Axis Position
                         // Vertical Layout -> Cross axis is X. Use offsetX.
                         val crossAxisOffset = if (isVertical) offsetX else offsetY
                         val paddingCross = if (isVertical) startPadding else topPadding
                         
                         // Note: If widthDiff > 0, offset is already biased by TransformableState.
                         // So we just add it to padding.
                         val finalCrossPos = (crossAxisOffset + paddingCross).roundToInt()
                         
                         // We reinstate item-specific alignment relative to maxCrossAxisSize
                         placeables.forEach { p ->
                             val itemAlignOffset = if (isVertical) {
                                  val align = horizontalAlignment ?: Alignment.CenterHorizontally
                                  val scaledMaxCross = maxCrossAxisSize * currentScale
                                  align.align(p.width, scaledMaxCross.roundToInt(), layoutDirection)
                             } else {
                                  val align = verticalAlignment ?: Alignment.CenterVertically
                                  val scaledMaxCross = maxCrossAxisSize * currentScale
                                  align.align(p.height, scaledMaxCross.roundToInt())
                             }
                             
                             val x = if (isVertical) finalCrossPos + itemAlignOffset else finalMainPos
                             val y = if (isVertical) finalMainPos else finalCrossPos + itemAlignOffset
                             
                             visiblePlaceables.add(Triple(p, IntOffset(x, y), i))
                         }
                     }
                     
                     currentMainPos += mainPx + spaceBetweenItems
                 }
            }

            // 7. Report Size to State
            val paddingMainTotal = if (isVertical) verticalPadding else horizontalPadding
            val totalMainWithPadding = totalMainAxisSize + paddingMainTotal
            
            val paddingCrossTotal = if (isVertical) horizontalPadding else verticalPadding
            val totalCrossWithPadding = maxCrossAxisSize + paddingCrossTotal
            
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

            // Update Layout Info
            val layoutInfo = DefaultLazyDocumentLayoutInfo(
                visibleItemsInfo = visiblePlaceables.map { (placeable, position, index) ->
                    val dim = itemProvider.getItemSize(index)
                    DefaultLazyDocumentItemInfo(
                        index = index,
                        offset = position,
                        size = IntSize(placeable.width, placeable.height),
                        sizeMm = IntSize(dim.width.mm.roundToInt(), dim.height.mm.roundToInt())
                    )
                },
                viewportSize = IntSize(containerConstraints.maxWidth, containerConstraints.maxHeight)
            )
            state.updateLayoutInfo(layoutInfo)

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