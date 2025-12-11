package com.studiomath.pencilnotes.document.compose.lazyDocument.state

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Interface that provides information about the layout state of the LazyDocumentViewer.
 */
interface LazyDocumentLayoutInfo {
    /**
     * The list of items currently visible in the viewer.
     */
    val visibleItemsInfo: List<LazyDocumentItemInfo>
    
    /**
     * The size of the viewport in pixels.
     */
    val viewportSize: IntSize
}

/**
 * Information about a single visible item in the LazyDocumentViewer.
 */
interface LazyDocumentItemInfo {
    /**
     * The index of the item in the list of items.
     */
    val index: Int
    
    /**
     * The offset of the item relative to the viewport.
     */
    val offset: IntOffset
    
    /**
     * The size of the item.
     */
    val size: IntSize
    
    /**
     * The size of the item in mm.
     */
     val sizeMm: IntSize
}
