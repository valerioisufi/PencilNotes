package com.studiomath.pencilnotes.document.compose.state

import androidx.ink.strokes.Stroke

sealed class DocumentAction {
    data class AddStrokes(val strokes: List<Stroke>) : DocumentAction()
    // Aggiungi qui altre azioni come DeletePage, AddImage, etc.
}