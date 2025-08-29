package com.studiomath.pencilnotes.document.compose

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.studiomath.pencilnotes.document.compose.state.DocumentViewerState
import com.studiomath.pencilnotes.document.compose.state.rememberDocumentViewerState

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
 * @param state The state object that can be used to control and observe the viewer's state.
 * @param content A block defining the items to be displayed.
 */
@Composable
fun LazyDocumentViewer(
    modifier: Modifier = Modifier,
    state: DocumentViewerState = rememberDocumentViewerState(),
    content: LazyDocumentViewerScope.() -> Unit
) {
    val itemProvider = rememberLazyDocumentViewerProvider(content)

    LazyLayout(
        modifier = modifier,
        itemProvider = { itemProvider }
    ){ constraints ->


        layout(constraints.maxWidth, constraints.maxHeight) {

        }
    }
}

