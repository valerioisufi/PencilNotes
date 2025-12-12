package com.studiomath.pencilnotes.document.compose.lazyDocument

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import com.studiomath.pencilnotes.document.compose.lazyDocument.state.TransformableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Modifier.detectDocumentGestures(
    transformableState: TransformableState,
    coroutineScope: CoroutineScope,
    enabled: Boolean = true,
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    shouldConsumeEvent: (androidx.compose.ui.input.pointer.PointerEvent) -> Boolean = { true }
): Modifier = this.pointerInput(enabled, transformableState, shouldConsumeEvent) {
    if (!enabled) return@pointerInput

    forEachGesture {
        awaitPointerEventScope {
            val velocityTracker = VelocityTracker()

            // Attendi il primo tocco
            awaitFirstDown(requireUnconsumed = false)
            onGestureStart()

            // Variable to track if any significant zoom happened during the gesture
            var zoomOccurred = false

            do {
                val event = awaitPointerEvent()

                // Calcola lo zoom e il pan basandosi sul movimento di tutte le dita.
                if (shouldConsumeEvent(event)) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()

                    // Se c'è un cambiamento, applica la trasformazione allo stato.
                    if (zoom != 1f || pan != Offset.Zero) {
                        if (zoom != 1f) zoomOccurred = true
                        coroutineScope.launch {
                            transformableState.applyTransform(event.calculateCentroid(), pan, zoom)
                        }
                    }

                    // Aggiungi gli eventi al velocity tracker per calcolare la velocità finale.
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            velocityTracker.addPointerInputChange(it)
                            // Consume the change if we are handling it
                            it.consume()
                        }
                    }
                }

            } while (event.changes.any { it.pressed })

            onGestureEnd()

            // Quando l'utente solleva le dita, calcola la velocità...
            val velocity = velocityTracker.calculateVelocity()

            // ...e avvia l'animazione di fling SOLO se non abbiamo fatto zoom.
            // Se abbiamo fatto zoom, è probabile che il movimento delle dita abbia generato velocità spuria.
            if (!zoomOccurred) {
                coroutineScope.launch {
                    transformableState.fling(velocity)
                }
            } else {
                // Se c'è stato zoom (e quindi potremmo essere fuori scala), assicuriamoci di "settle".
                coroutineScope.launch {
                    transformableState.settle()
                }
            }
        }
    }
}
