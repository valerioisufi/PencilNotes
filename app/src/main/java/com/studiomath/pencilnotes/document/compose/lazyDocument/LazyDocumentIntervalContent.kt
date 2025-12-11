package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable
import com.studiomath.pencilnotes.document.page.Dimension
class LazyDocumentIntervalContent (content: LazyDocumentViewerScope.() -> Unit) :
    LazyLayoutIntervalContent<LazyDocumentItemInterval>(), LazyDocumentViewerScope {

    override val intervals: MutableIntervalList<LazyDocumentItemInterval> = MutableIntervalList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemSize: (index: Int) -> Dimension,
        itemContent: @Composable LazyDocumentViewerItemScope.(index: Int) -> Unit,
    ) {
        intervals.addInterval(
            count,
            LazyDocumentItemInterval(
                key = key,
                type = contentType,
                item = itemContent,
                itemSize = itemSize
            ),
        )
    }

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyDocumentViewerItemScope.() -> Unit) {
         error("NOT IMPLEMENTED: item() without size")
    }

}

class LazyDocumentItemInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable LazyDocumentViewerItemScope.(index: Int) -> Unit,
    val itemSize: (index: Int) -> Dimension,
) : LazyLayoutIntervalContent.Interval