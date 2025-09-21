package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.LazyDocumentViewerState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.TransformableState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberLazyDocumentViewerState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.rememberTrasformableState
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
    modifier: Modifier = Modifier,
    state: LazyDocumentViewerState = rememberLazyDocumentViewerState(),
    transformableState: TransformableState = rememberTrasformableState(),
    content: LazyDocumentViewerScope.() -> Unit
) {
    val itemProvider = rememberLazyDocumentViewerProvider(state, content)
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
        IntSize(400, 200),
        IntSize(400, 100),
        IntSize(200, 50),
        IntSize(700, 100),
        IntSize(400, 10),
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