package com.studiomath.pencilnotes.document.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.withSave
import com.studiomath.pencilnotes.document.page.Page
import com.studiomath.pencilnotes.document.page.PageMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Rect as AndroidRect
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.RectF
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.sync.withLock
import android.graphics.Color

@Composable
fun PageComposable(
    modifier: Modifier = Modifier,
    page: Page,
    pageMaker: PageMaker,
    viewportRect: Rect = Rect.Zero,
    scale: Float = 1f
) {
    // Observe version to trigger recomposition when strokes are added
    // Observe updateTrigger to distinguish between Full and Incremental updates
    val updateTrigger = page.updateTrigger
    var bitmap by remember { mutableStateOf(page.bitmapPage) }
    // Key to force recomposition of Image even if bitmap reference stays same (for mutable bitmaps)
    var redrawKey by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    // High Def Layer State
    var highDefBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var highDefOffset by remember { mutableStateOf(IntOffset.Zero) }
    var generationPageWidth by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Logic for Cached Bitmap (Low Res)
    LaunchedEffect(page, updateTrigger) {
        when(updateTrigger) {
            is Page.UpdateTrigger.Full -> {
                 withContext(Dispatchers.Default) {
                     page.mutex.withLock {
                         if (!page.isPrepared) {
                             page.prepare()
                         }
                         val cachedBitmap = page.bitmapPage
                         if (cachedBitmap != null) {
                            val result = pageMaker.makePage(
                                bitmapRect = AndroidRect(0, 0, cachedBitmap.width, cachedBitmap.height),
                                bitmapSource = cachedBitmap,
                                page = page
                            )
                            page.bitmapPage = result
                         }
                     }
                 }
                 bitmap = page.bitmapPage
                 redrawKey++
            }
            is Page.UpdateTrigger.Incremental -> {
                // If bitmap was modified in-place, the reference might be the same.
                // We force update and rely on redrawKey to trigger recomposition.
                val newBitmap = page.bitmapPage
                if (bitmap !== newBitmap) {
                    bitmap = newBitmap
                }
                redrawKey++
            }
            Page.UpdateTrigger.None -> {
                if (bitmap == null) {
                    withContext(Dispatchers.Default) {
                         if (!page.isPrepared) {
                             page.prepare()
                         }
                         val cachedBitmap = page.bitmapPage
                          if (cachedBitmap != null) {
                            val result = pageMaker.makePage(
                                bitmapRect = AndroidRect(0, 0, cachedBitmap.width, cachedBitmap.height),
                                bitmapSource = cachedBitmap,
                                page = page
                            )
                            page.bitmapPage = result
                         }
                    }
                    bitmap = page.bitmapPage
                    redrawKey++
                }
            }
        }
    }

    // Logic for High Def Layer
    LaunchedEffect(viewportRect, scale, layoutCoordinates, updateTrigger, page) {
        // Debounce using snapshotFlow
        snapshotFlow {
            Triple(viewportRect, scale, layoutCoordinates)
        }
        .debounce(150) // Wait for scroll to settle
        .collectLatest { (viewport, currentScale, coords) ->

            if (coords == null || viewport.isEmpty) return@collectLatest
            
            // Use current size for calculations
            val pageWidthScaledPx = coords.size.width.toFloat()
            val pageHeightScaledPx = coords.size.height.toFloat()
    
            // Calculate Intersection
            val pageBoundsInWindow = coords.boundsInWindow()
            val visibleBounds = pageBoundsInWindow.intersect(viewport)
    
            if (!visibleBounds.isEmpty) {
                 val visibleSize = visibleBounds.size
    
                 // Map visibleBounds (Window Coords) to Page Local Coordinates (Px)
                 // This offset represents where the visible rect starts relative to the Page's (0,0)
                 val localTopLeft = coords.windowToLocal(visibleBounds.topLeft)
                 
                 // Setup clipRect for PageMaker
                 // We map the Page (0,0 -> W_mm, H_mm) to the Bitmap's coordinate space.
                 // We want the point `localTopLeft` on the scaled page to map to (0,0) on the bitmap.
                 // The Page is scaled to `pageWidthScaledPx`.
                 // So:
                 val dstRectF = RectF(
                     -localTopLeft.x,
                     -localTopLeft.y,
                     -localTopLeft.x + pageWidthScaledPx,
                     -localTopLeft.y + pageHeightScaledPx
                 )
                 
                 withContext(Dispatchers.Default) {
                     // Reuse existing bitmap if possible to avoid allocation churn
                     val recycleBitmap = highDefBitmap
                     
                     val hdBitmap = pageMaker.makePage(
                         bitmapRect = AndroidRect(0, 0, visibleSize.width.roundToInt(), visibleSize.height.roundToInt()),
                         bitmapSource = null,
                         page = page,
                         clipRect = dstRectF,
                         reuseBitmap = recycleBitmap
                     )
                     highDefBitmap = hdBitmap
                     highDefOffset = IntOffset(localTopLeft.x.roundToInt(), localTopLeft.y.roundToInt())
                     generationPageWidth = pageWidthScaledPx
                 }
            }
        }
    }

    Box(modifier = modifier.onGloballyPositioned { layoutCoordinates = it }) {
        // Low Res Layer
        if (bitmap != null) {
            androidx.compose.runtime.key(redrawKey) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(), 
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // High Def Layer
        if (highDefBitmap != null && layoutCoordinates != null) {
             val currentWidth = layoutCoordinates!!.size.width.toFloat()
             val relativeScale = if (generationPageWidth > 0) currentWidth / generationPageWidth else 1f
             
             HighDefLayer(
                 bitmap = highDefBitmap!!,
                 offset = highDefOffset,
                 scale = relativeScale
             )
        }
    }
}

@Composable
private fun HighDefLayer(
    bitmap: Bitmap,
    offset: IntOffset,
    scale: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
             canvas.withSave {
                 // 1. Translate to the scaled position
                 canvas.translate(offset.x * scale, offset.y * scale)
                 // 2. Scale the bitmap drawing
                 canvas.scale(scale, scale)
                 // 3. Draw bitmap at (0,0) local to the transform
                 canvas.nativeCanvas.drawBitmap(
                     bitmap,
                     0f,
                     0f,
                     null
                 )
             }
        }
    }
}
