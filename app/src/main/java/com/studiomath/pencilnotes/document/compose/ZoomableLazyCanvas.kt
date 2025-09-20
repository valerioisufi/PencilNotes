//package com.studiomath.pencilnotes.document.compose
//
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.layout.Placeable
//import androidx.compose.foundation.lazy.layout.LazyLayout
//import androidx.compose.runtime.remember
//import com.studiomath.pencilnotes.document.compose.lazyDocument.state.TransformableState
//import kotlin.math.roundToInt
//import androidx.compose.ui.unit.dp
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun ZoomableLazyCanvas(
//    modifier: Modifier = Modifier,
//    state: TransformableState,
//    pageCount: Int,
//    pageContent: @Composable (index: Int) -> Unit
//) {
//    val itemProvider = remember(pageContent) {
//        // ... (ItemProvider come nell'esempio precedente) ...
//    }
//
//    LazyLayout(
//        modifier = modifier,
//        itemProvider = itemProvider
//    ) { constraints ->
//        // Calcola l'altezza di un item (assumendo che abbiano tutti la stessa larghezza)
//        // Questa è una semplificazione, in un caso reale dovresti misurare il primo
//        // item per avere una stima.
//        val itemWidth = constraints.maxWidth
//        val itemHeight = (itemWidth * (297f / 210f)).roundToInt() // A4 ratio example
//        val pageSpacing = 16.dp.toPx()
//
//        // Calcola l'altezza totale del contenuto
//        val totalContentHeight = (pageCount * itemHeight + (pageCount - 1) * pageSpacing) * state.scale
//
//        // Logica di virtualizzazione: quali pagine sono visibili?
//        val firstVisibleItemIndex = ((-state.offsetY / state.scale) / (itemHeight + pageSpacing)).toInt().coerceAtLeast(0)
//        val visibleItemCount = ((constraints.maxHeight / state.scale) / (itemHeight + pageSpacing)).toInt() + 2
//        val lastVisibleItemIndex = (firstVisibleItemIndex + visibleItemCount).coerceAtMost(pageCount - 1)
//
//        val visibleItems = mutableMapOf<Int, Placeable>()
//        if (firstVisibleItemIndex <= lastVisibleItemIndex) {
//            for (index in firstVisibleItemIndex..lastVisibleItemIndex) {
//                visibleItems[index] = compose(index).map { it.measure(constraints) }.first()
//            }
//        }
//
//        layout(constraints.maxWidth, constraints.maxHeight) {
//            visibleItems.forEach { (index, placeable) ->
//                val itemY = (index * (itemHeight + pageSpacing) * state.scale + state.offsetY).roundToInt()
//                placeable.place(
//                    x = state.offsetX.roundToInt(),
//                    y = itemY
//                )
//            }
//        }
//    }
//}