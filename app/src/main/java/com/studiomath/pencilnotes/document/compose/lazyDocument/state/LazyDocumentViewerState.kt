package com.studiomath.pencilnotes.document.compose.lazyDocument.state

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun rememberLazyDocumentViewerState(): LazyDocumentViewerState {
    return remember {
        LazyDocumentViewerState()
    }
}

@Stable
class LazyDocumentViewerState {

    /** Stores currently pinned items which are always composed. */
    internal val pinnedItems = LazyLayoutPinnedItemList()
    internal val nearestRange: IntRange = 0..0

}