package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import com.studiomath.pencilnotes.document.page.Dimension


private data class PageSizeData(val dimension: Dimension)

// Implementazione del ParentDataModifier.
// Questo modifier non disegna né cambia le dimensioni,
// allega solo i dati al Composable.
private class PageSizeModifier(
    val dimension: Dimension
) : ParentDataModifier {
    // Il metodo produce i dati che vogliamo allegare.
    override fun Density.modifyParentData(parentData: Any?): Any {
        return PageSizeData(dimension)
    }
}


interface LazyDocumentViewerItemScope {
    /**
     * Un Modifier custom per specificare le dimensioni di un item del documento in millimetri.
     */
    fun Modifier.documentSize(dimension: Dimension): Modifier
}

/**
 * The default implementation of [LazyDocumentViewerScope].
 * It processes the DSL and builds a list of [LazyDocumentItem]s.
 */
class LazyDocumentViewerItemScopeImpl : LazyDocumentViewerItemScope {
    override fun Modifier.documentSize(dimension: Dimension): Modifier {
        return this.then(PageSizeModifier(dimension))
    }

}