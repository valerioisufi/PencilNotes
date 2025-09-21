package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.LazyDocumentViewerState

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
fun rememberLazyDocumentViewerProvider(
    state: LazyDocumentViewerState,
    content: LazyDocumentViewerScope.() -> Unit
): LazyDocumentViewerProvider {
    // rememberUpdatedState ensures that we are always using the latest version of the content lambda
    // without causing unnecessary recompositions.
    val latestContent = rememberUpdatedState(content)

    // Remember the provider itself. It will be stable across recompositions
    // unless the state instance changes (which it won't).
    return remember(state) {
        val scope = LazyDocumentViewerItemScopeImpl()
        val intervalContentState =
            derivedStateOf(policy = referentialEqualityPolicy()) {
                LazyDocumentIntervalContent(latestContent.value)
            }
        val itemProviderState =
            derivedStateOf(referentialEqualityPolicy()) {
                val intervalContent = intervalContentState.value
                val map = LazyLayoutKeyIndexMap(state.nearestRange, intervalContent)
                LazyDocumentViewerProvider(
                    state = state,
                    intervalContent = intervalContent,
                    itemScope = scope,
                    keyIndexMap = map,
                )
            }
        itemProviderState.value
    }
}


/**
 * An implementation of [LazyLayoutItemProvider] that provides items to the [LazyLayout].
 * It's backed by a [State] object containing the list of items, so the layout
 * can react to changes in the content.
 *
 * @param state A state object holding the current list of document items.
 */
class LazyDocumentViewerProvider(
    val state: LazyDocumentViewerState,
    val intervalContent: LazyDocumentIntervalContent,
    val itemScope: LazyDocumentViewerItemScopeImpl,
    val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyLayoutItemProvider {

    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedItems) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(itemScope, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getContentType(index: Int): Any? = intervalContent.getContentType(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyDocumentViewerProvider) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}
