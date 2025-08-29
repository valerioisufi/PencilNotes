package com.studiomath.pencilnotes.document.compose.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Stato che gestisce la logica di trasformazione e animazione per il DocumentViewer.
 *
 * Questa classe incapsula offset, scala e le animazioni di fling e bounce.
 *
 * @param minScale Il livello minimo di zoom consentito.
 * @param maxScale Il livello massimo di zoom consentito.
 * @param flingDecay Specifica l'animazione di decadimento per l'inerzia (fling).
 */
@Stable
class DocumentViewerState(
    val minScale: Float = 0.5f,
    val maxScale: Float = 5f,
    val flingDecay: DecayAnimationSpec<Float> = exponentialDecay()
) {
    // Animatable per gestire le trasformazioni con animazioni fluide.
    private val _scale = Animatable(1f)
    private val _offset = Animatable(Offset.Zero, Offset.VectorConverter)

    val scale: Float
        get() = _scale.value

    val offset: Offset
        get() = _offset.value

    // Dimensioni del contenitore del layout e del contenuto totale.
    private var layoutSize = IntSize.Zero
    private var contentSize = IntSize.Zero

    // Limiti calcolati per l'offset, per evitare di scorrere fuori dal contenuto.
    private val minOffsetX: Float
        get() = if (contentSize.width * scale > layoutSize.width) {
            layoutSize.width - contentSize.width * scale
        } else {
            (layoutSize.width - contentSize.width * scale) / 2f
        }
    private val maxOffsetX: Float
        get() = if (contentSize.width * scale > layoutSize.width) 0f else (layoutSize.width - contentSize.width * scale) / 2f

    private val minOffsetY: Float
        get() = if (contentSize.height * scale > layoutSize.height) {
            layoutSize.height - contentSize.height * scale
        } else {
            0f
        }
    private val maxOffsetY: Float
        get() = if (contentSize.height * scale > layoutSize.height) 0f else 0f


    /**
     * Aggiorna le dimensioni del layout. Chiamato quando il componente viene misurato.
     */
    fun onLayoutSizeChanged(size: IntSize) {
        layoutSize = size
        // Forza la correzione dell'offset se i nuovi limiti vengono violati
        correctOffset()
    }

    /**
     * Aggiorna le dimensioni totali del contenuto.
     */
    fun onContentSizeChanged(size: IntSize) {
        contentSize = size
        correctOffset()
    }

    /**
     * Applica un gesto di trasformazione (pan e zoom).
     *
     * @param centroid Il punto centrale del gesto di zoom.
     * @param pan Lo spostamento (pan) richiesto.
     * @param zoom Il fattore di scala richiesto.
     */
    fun applyTransform(centroid: Offset, pan: Offset, zoom: Float, scope: CoroutineScope) {
        scope.launch {
            val newScale = (_scale.value * zoom).coerceIn(minScale, maxScale)
            val oldScale = _scale.value
            _scale.snapTo(newScale)

            // Calcola l'offset per far sì che lo zoom avvenga intorno al punto centrale (centroid).
            val newOffset = (_offset.value + pan + centroid * oldScale - centroid * newScale)
            _offset.snapTo(newOffset.coerceIn(minOffsetX, maxOffsetX, minOffsetY, maxOffsetY))
        }
    }

    /**
     * Avvia un'animazione di fling (inerzia) basata sulla velocità del gesto.
     *
     * @param velocity La velocità finale del trascinamento.
     */
    fun fling(velocity: Velocity, scope: CoroutineScope) {
        scope.launch {
            val initialOffset = _offset.value
            _offset.animateDecay(
                initialVelocity = Offset(velocity.x, velocity.y),
                animationSpec = exponentialDecay()
            ) {
                // Durante l'animazione di fling, controlla i limiti.
                // Se un limite viene superato, ferma il fling e avvia un'animazione di "bounce" (rimbalzo).
                val current = this.value
                val coerced = current.coerceIn(minOffsetX, maxOffsetX, minOffsetY, maxOffsetY)

                if (abs(coerced.x - current.x) > 0.1f || abs(coerced.y - current.y) > 0.1f) {
                    // Limite superato, ferma l'animazione di fling
                    this.cancelAnimation()
                    // Avvia l'animazione di bounce per tornare dolcemente al limite
                    _offset.animateTo(coerced, spring())
                }
            }
        }
    }

    /**
     * Corregge l'offset per assicurarsi che rientri nei limiti validi.
     * Utile dopo un cambio di zoom o di dimensioni.
     */
    private fun correctOffset() {
        val coercedOffset = offset.coerceIn(minOffsetX, maxOffsetX, minOffsetY, maxOffsetY)
        if (coercedOffset != offset) {
            // Se l'offset è fuori dai limiti, lancialo in un CoroutineScope
            // per evitare di chiamare animateTo direttamente da una funzione non-composable
            // o non-suspend. In un'app reale, questo scope dovrebbe essere gestito
            // più attentamente, ma per questo esempio è sufficiente.
            val tempScope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
            tempScope.launch {
                _offset.animateTo(coercedOffset, spring())
            }
        }
    }

    /**
     * Funzione di utility per contenere un Offset all'interno di un rettangolo di limiti.
     */
    private fun Offset.coerceIn(minX: Float, maxX: Float, minY: Float, maxY: Float): Offset =
        Offset(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
}

/**
 * Composable che crea e ricorda un'istanza di [DocumentViewerState].
 */
@Composable
fun rememberDocumentViewerState(
    minScale: Float = 0.5f,
    maxScale: Float = 5f,
    flingDecay: DecayAnimationSpec<Float> = exponentialDecay()
): DocumentViewerState {
    return remember {
        DocumentViewerState(minScale, maxScale, flingDecay)
    }
}