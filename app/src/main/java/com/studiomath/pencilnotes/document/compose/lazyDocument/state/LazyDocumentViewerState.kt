package com.studiomath.pencilnotes.document.compose.lazyDocument.state

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

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
    
    private var _layoutInfo: LazyDocumentLayoutInfo by mutableStateOf(EmptyLazyDocumentLayoutInfo)
    
    val layoutInfo: LazyDocumentLayoutInfo
        get() = _layoutInfo
        
    internal fun updateLayoutInfo(info: LazyDocumentLayoutInfo) {
        _layoutInfo = info
    }

}

private object EmptyLazyDocumentLayoutInfo : LazyDocumentLayoutInfo {
    override val visibleItemsInfo: List<LazyDocumentItemInfo> = emptyList()
    override val viewportSize: IntSize = IntSize.Zero
}