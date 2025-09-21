package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.runtime.Composable


/**
 * A data class to hold the key and composable content for a single item.
 */
class LazyDocumentItem(
    val key: Any,
    val content: @Composable () -> Unit
)
