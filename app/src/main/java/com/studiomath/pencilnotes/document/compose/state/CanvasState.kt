package com.studiomath.pencilnotes.document.compose.state

import android.graphics.Bitmap
import androidx.ink.brush.Brush
import com.studiomath.pencilnotes.document.page.DrawDocumentData

data class CanvasState(
    val isLoading: Boolean = true,
    val pages: List<DrawDocumentData.Page> = emptyList(),

    // Mappa di cache per le bitmap delle pagine, per evitare di ricaricarle
    val pageBitmaps: Map<Int, Bitmap> = emptyMap(),
    val activeBrush: Brush? = null
)