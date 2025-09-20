package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.TransformableState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberTrasformableState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Defines the scope for the content of a LazyDocumentViewer.
 * This provides a DSL for defining items within the lazy layout.
 */
interface LazyDocumentViewerScope {
    /**
     * Adds a number of items to the document viewer.
     *
     * @param count The number of items to add.
     * @param key A factory of stable and unique keys representing the item.
     * Using keys allows Compose to uniquely identify items, which is essential
     * for preserving state and improving performance with dynamic content.
     * @param itemContent The composable content for a given item index.
     */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        itemContent: @Composable (index: Int) -> Unit,
    )
}

/**
 * A data class to hold the key and composable content for a single item.
 */
private class LazyDocumentItem(
    val key: Any,
    val content: @Composable () -> Unit
)

/**
 * The default implementation of [LazyDocumentViewerScope].
 * It processes the DSL and builds a list of [LazyDocumentItem]s.
 */
private class LazyDocumentViewerScopeImpl : LazyDocumentViewerScope {
    val items = mutableListOf<LazyDocumentItem>()

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        itemContent: @Composable (index: Int) -> Unit,
    ) {
        for (i in 0 until count) {
            // Use the provided key, or fallback to the index if no key is provided.
            // Using the index as a key is better than null, but custom keys are recommended.
            val itemKey = key?.invoke(i) ?: i
            items.add(LazyDocumentItem(itemKey) { itemContent(i) })
        }
    }
}

/**
 * An implementation of [LazyLayoutItemProvider] that provides items to the [LazyLayout].
 * It's backed by a [State] object containing the list of items, so the layout
 * can react to changes in the content.
 *
 * @param itemsState A state object holding the current list of document items.
 */
private class LazyDocumentViewerProvider(
    private val itemsState: State<List<LazyDocumentItem>>
) : LazyLayoutItemProvider {
    private val items: List<LazyDocumentItem>
        get() = itemsState.value

    override val itemCount: Int
        get() = items.size

    override fun getKey(index: Int): Any = items[index].key

    @Composable
    override fun Item(index: Int, key: Any) {
        // The key is handled by the LazyLayout infrastructure. We just need to invoke
        // the composable content for the given index.
        items[index].content()
    }
}


/**
 * Creates and remembers a [LazyDocumentViewerProvider].
 *
 * This function follows a common pattern in Compose for lazy layouts. It uses [derivedStateOf]
 * to ensure that the list of items is only re-calculated when the `content` lambda actually changes.
 *
 * @param content The DSL block that defines the items in the lazy document viewer.
 * @return A remembered instance of [LazyDocumentViewerProvider].
 */
@Composable
private fun rememberLazyDocumentViewerProvider(
    content: LazyDocumentViewerScope.() -> Unit
): LazyDocumentViewerProvider {
    // rememberUpdatedState ensures that we are always using the latest version of the content lambda
    // without causing unnecessary recompositions.
    val latestContent = rememberUpdatedState(content)

    // derivedStateOf creates a state object that will only update when the result of its calculation changes.
    // This is more efficient than recalculating on every recomposition.
    val itemsState = remember {
        derivedStateOf {
            LazyDocumentViewerScopeImpl().apply(latestContent.value).items
        }
    }

    // Remember the provider itself. It will be stable across recompositions
    // unless the itemsState instance changes (which it won't).
    return remember { LazyDocumentViewerProvider(itemsState) }
}



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
    modifier: Modifier = Modifier,
    transformableState: TransformableState = rememberTrasformableState(),
    content: LazyDocumentViewerScope.() -> Unit
) {
    val itemProvider = rememberLazyDocumentViewerProvider(content)
    // NUOVO: Un CoroutineScope per lanciare le animazioni di fling e bounce
    // al di fuori della composizione.
    val scope = rememberCoroutineScope()

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
                        scope.launch {
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
                scope.launch {
                    transformableState.fling(velocity, this)
                }
            }
        }
    }

    LazyLayout(
        // Applica il modifier per i gesti insieme a quello passato dall'esterno.
        modifier = modifier.then(gestureModifier),
        itemProvider = { itemProvider }
    ) { constraints ->
        // ... (la logica di misurazione e posizionamento rimane esattamente la stessa) ...
        val scale = transformableState.scale
        val offsetX = transformableState.offset.x
        val offsetY = transformableState.offset.y

        transformableState.onLayoutSizeChanged(
            IntSize(constraints.maxWidth, constraints.maxHeight),
            scope
        )

        val itemConstraints = constraints.copy(minWidth = constraints.maxWidth, minHeight = 0)
        var totalHeight = 0
        val visiblePlaceables = mutableListOf<Pair<Placeable, IntOffset>>()

        for (index in 0 until itemProvider.itemCount) {
            val placeable = compose(index).map { it.measure(itemConstraints) }.first()
            val itemHeight = placeable.height
            val yPos = totalHeight
            val itemTopScaled = yPos * scale + offsetY
            val itemBottomScaled = (yPos + itemHeight) * scale + offsetY

            if (itemBottomScaled >= 0 && itemTopScaled <= constraints.maxHeight) {
                val finalX = offsetX.roundToInt()
                val finalY = (yPos * scale + offsetY).roundToInt()
                visiblePlaceables.add(placeable to IntOffset(finalX, finalY))
            }
            totalHeight += itemHeight
        }

        transformableState.onContentSizeChanged(
            IntSize(constraints.maxWidth, totalHeight),
            scope
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
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
        IntSize(400, 800),
        IntSize(400, 800),
        IntSize(200, 300),
        IntSize(700, 100),
        IntSize(400, 800),
        IntSize(400, 800),
        IntSize(200, 300),
        IntSize(700, 100),
    )) }

    LazyDocumentViewer {
        items(listSize.size, key = { it  }) { item ->
            Spacer(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color(0xFFCCCCCC))
                    .size(listSize[item].width.dp, listSize[item].height.dp)
            )
        }
    }
}