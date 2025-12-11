package com.studiomath.pencilnotes.document.compose.lazyDocument.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Stato che gestisce la logica di trasformazione e animazione per il DocumentViewer.
 *
 * Include supporto per:
 * - Rubber-banding (resistenza) sui bordi e sullo zoom.
 * - Fling con decadimento naturale.
 * - Bounce-back ai limiti (effetto molla).
 *
 * @param minScale Il livello minimo di zoom consentito.
 * @param maxScale Il livello massimo di zoom consentito.
 * @param flingDecay Specifica l'animazione di decadimento per l'inerzia (fling).
 */
@Stable
class TransformableState(
    val minScale: Float = 0.5f,
    val maxScale: Float = 5f,
    val flingDecay: DecayAnimationSpec<Offset> = exponentialDecay()
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

    // I limiti rigidi (hard bounds) per l'offset.
    private val minOffsetX: Float
        get() {
            val scaledWidth = contentSize.width * scale
            val widthDiff = layoutSize.width - scaledWidth
            return if (widthDiff > 0) widthDiff / 2f else widthDiff
        }

    private val maxOffsetX: Float
        get() {
            val scaledWidth = contentSize.width * scale
            val widthDiff = layoutSize.width - scaledWidth
            return if (widthDiff > 0) widthDiff / 2f else 0f
        }

    private val minOffsetY: Float
        get() {
            val scaledHeight = contentSize.height * scale
            val heightDiff = layoutSize.height - scaledHeight
            return if (heightDiff > 0) heightDiff / 2f else heightDiff
        }

    private val maxOffsetY: Float
        get() {
            val scaledHeight = contentSize.height * scale
            val heightDiff = layoutSize.height - scaledHeight
            return if (heightDiff > 0) heightDiff / 2f else 0f
        }

    /**
     * Aggiorna le dimensioni del layout.
     */
    fun onLayoutSizeChanged(size: IntSize, scope: CoroutineScope) {
        if (layoutSize != size) {
            layoutSize = size
            scope.launch { settle() }
        }
    }

    /**
     * Aggiorna le dimensioni totali del contenuto.
     */
    fun onContentSizeChanged(size: IntSize, scope: CoroutineScope) {
        if (contentSize != size) {
            contentSize = size
            scope.launch { settle() }
        }
    }

    /**
     * Applica traslazione e zoom immediati (da gesture) con effetto resistenza (rubber band).
     */
    suspend fun applyTransform(centroid: Offset, panChange: Offset, zoomChange: Float) {
        val currentScale = _scale.value
        val currentOffset = _offset.value

        // 1. Calcolo nuova scala raw (senza limiti)
        val rawScale = currentScale * zoomChange
        
        // 2. Applica resistenza alla scala se fuori dai limiti
        val dampedScale = if (rawScale < minScale) {
            // Resistenza logaritmica per scalare sotto il minimo.
            // Impedisce di avvicinarsi a 0 troppo velocemente e mai < 0.
            val diff = minScale - rawScale
            val damp = diff / (diff + minScale) // 0..1 curve
            minScale * (1f - damp * 0.5f) 
            // Oppure semplificato:
            // scale = minScale - (minScale - rawScale) * damping
            // Ma per evitare negativi: se raw -> 0, damped -> minScale/2 ?
            // Usiamo formula geometrica:
            // Delta extra = minScale - rawScale.
            // damped = minScale - applyResistance(Delta extra)
            val resist = applyScaleResistance(minScale - rawScale)
            minScale - resist
        } else if (rawScale > maxScale) {
            maxScale + applyScaleResistance(rawScale - maxScale)
        } else {
            rawScale
        }
        
        _scale.snapTo(dampedScale)

        // 3. Calcolo nuovo offset
        val effectiveZoomChange = dampedScale / currentScale
        
        val simpleOffsetChange = (centroid - currentOffset) * (1f - effectiveZoomChange)
        
        var targetX = currentOffset.x + simpleOffsetChange.x + panChange.x
        var targetY = currentOffset.y + simpleOffsetChange.y + panChange.y

        // 4. Applica resistenza all'offset se fuori dai limiti
        val minX = minOffsetX
        val maxX = maxOffsetX
        val minY = minOffsetY
        val maxY = maxOffsetY

        if (targetX < minX) {
            targetX = minX - applyResistance(minX - targetX)
        } else if (targetX > maxX) {
            targetX = maxX + applyResistance(targetX - maxX)
        }

        if (targetY < minY) {
             targetY = minY - applyResistance(minY - targetY)
        } else if (targetY > maxY) {
             targetY = maxY + applyResistance(targetY - maxY)
        }

        _offset.snapTo(Offset(targetX, targetY))
    }
    
    private fun applyScaleResistance(overshoot: Float): Float {
        // Scala resistance needs to be much gentler and bounded
        return if (overshoot > 0) kotlin.math.ln(overshoot + 1f) * 0.5f else 0f
    }
    
    // Funzione "sqrt" per resistenza rubber-band offset
    private fun applyResistance(overshoot: Float): Float {
        // Simple sqrt resistance
        return if (overshoot > 0) kotlin.math.sqrt(overshoot) * 3f else 0f
    }

    /**
     * Gestisce il fling (inerzia) al rilascio del tocco.
     * Decelera e, se tocca i bordi, rimbalza (tramite settle).
     */
    suspend fun fling(velocity: Velocity) {
        val decay = flingDecay
        
        // Se siamo già fuori dai bordi (es. overscroll trascinato e rilasciato), niente fling.
        if (isOutOfBounds()) {
            settle()
            return
        }
        
        try {
            _offset.animateDecay(
                initialVelocity = Offset(velocity.x, velocity.y),
                animationSpec = decay
            ) {
                 // Controlla limiti durante l'animazione
                 val current = this.value
                 val minX = minOffsetX
                 val maxX = maxOffsetX
                 val minY = minOffsetY
                 val maxY = maxOffsetY
                 
                 // Se attraversiamo il confine, fermiamoci AL confine (o poco oltre)
                 // e usciamo.
                 if (current.x < minX || current.x > maxX || current.y < minY || current.y > maxY) {
                     // Annulla animazione lanciando un'eccezione
                     throw CancellationException("Hit boundary")
                 }
            }
        } catch (e: CancellationException) {
            // Ignora
        }
        
        settle()
    }

    /**
     * Riporta lo stato entro i limiti validi con un'animazione a molla (Spring).
     */
    suspend fun settle() {
        val currentScale = _scale.value
        val targetScale = currentScale.coerceIn(minScale, maxScale)
        
        if (targetScale != currentScale) {
             _scale.animateTo(
                 targetValue = targetScale,
                 animationSpec = spring(stiffness = Spring.StiffnessLow)
             )
        }
        
        val minX = minOffsetX
        val maxX = maxOffsetX
        val minY = minOffsetY
        val maxY = maxOffsetY
        
        val currentOffset = _offset.value
        val targetX = currentOffset.x.coerceIn(minX, maxX)
        val targetY = currentOffset.y.coerceIn(minY, maxY)
        
        if (targetX != currentOffset.x || targetY != currentOffset.y) {
             _offset.animateTo(
                 targetValue = Offset(targetX, targetY),
                 animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
             )
        }
    }
    
    private fun isOutOfBounds(): Boolean {
        if (scale < minScale || scale > maxScale) return true
        val current = offset
        // Tolerance
        return current.x < minOffsetX - 1f || current.x > maxOffsetX + 1f ||
               current.y < minOffsetY - 1f || current.y > maxOffsetY + 1f
    }
}

/**
 * Composable che crea e ricorda un'istanza di [TransformableState].
 */
@Composable
fun rememberTrasformableState(
    minScale: Float = 0.5f,
    maxScale: Float = 5f,
    flingDecay: DecayAnimationSpec<Offset> = exponentialDecay()
): TransformableState {
    return remember {
        TransformableState(minScale, maxScale, flingDecay)
    }
}