package com.studiomath.pencilnotes.document.compose.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.Matrix
import androidx.compose.runtime.mutableStateOf

class TransformableState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
) {
    var viewMatrix by mutableStateOf(Matrix().apply {
        setScale(initialScale, initialScale)
        postTranslate(initialOffsetX, initialOffsetY)
    })
}

@Composable
fun rememberTransformableState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
): TransformableState = remember {
    TransformableState(initialScale, initialOffsetX, initialOffsetY)
}