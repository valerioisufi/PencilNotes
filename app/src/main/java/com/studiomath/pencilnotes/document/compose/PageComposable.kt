package com.studiomath.pencilnotes.document.compose

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.studiomath.pencilnotes.document.page.Page
import com.studiomath.pencilnotes.document.page.PageMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import android.graphics.Rect as AndroidRect
import androidx.compose.runtime.snapshotFlow
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import java.util.concurrent.ConcurrentHashMap

private const val TILE_SIZE = 512

data class TileKey(val col: Int, val row: Int, val discreteScale: Float)

fun calculateDiscreteScale(scale: Float): Float {
    return when {
         scale <= 0.25f -> 0.25f
         scale <= 0.5f -> 0.5f
         scale <= 1.0f -> 1.0f
         scale <= 2.0f -> 2.0f
         scale <= 4.0f -> 4.0f
         scale <= 8.0f -> 8.0f
         else -> 16.0f
    }
}

@Composable
fun PageComposable(
    modifier: Modifier = Modifier,
    page: Page,
    pageMaker: PageMaker,
    viewportRect: Rect = Rect.Zero,
    scale: Float = 1f
) {
    val updateTrigger = page.updateTrigger
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    
    var activeTiles by remember { mutableStateOf<Set<TileKey>>(emptySet()) }
    val tileCache = remember { ConcurrentHashMap<TileKey, Bitmap>() }
    var redrawTrigger by remember { mutableLongStateOf(0L) }

    // 1. Observe Viewport & Scale to establish Active Tiles
    LaunchedEffect(viewportRect, scale, layoutCoordinates) {
        snapshotFlow { Triple(viewportRect, scale, layoutCoordinates) }
            .debounce(100)
            .collectLatest { (viewport, currentScale, coords) ->
                if (coords == null || viewport.isEmpty) return@collectLatest
                
                val bounds = coords.boundsInWindow()
                val visibleBounds = bounds.intersect(viewport)
                if (visibleBounds.isEmpty) return@collectLatest
                
                // Map intersection to local intrinsic space
                val localTopLeft = coords.windowToLocal(visibleBounds.topLeft)
                val localBottomRight = coords.windowToLocal(visibleBounds.bottomRight)
                
                val visibleLocalRect = Rect(
                    min(localTopLeft.x, localBottomRight.x),
                    min(localTopLeft.y, localBottomRight.y),
                    max(localTopLeft.x, localBottomRight.x),
                    max(localTopLeft.y, localBottomRight.y)
                )
                
                val discreteScale = calculateDiscreteScale(currentScale)
                val tileIntrinsicSize = (TILE_SIZE / discreteScale)
                
                val startCol = floor(visibleLocalRect.left / tileIntrinsicSize).toInt()
                val endCol = floor(visibleLocalRect.right / tileIntrinsicSize).toInt()
                val startRow = floor(visibleLocalRect.top / tileIntrinsicSize).toInt()
                val endRow = floor(visibleLocalRect.bottom / tileIntrinsicSize).toInt()
                
                val requiredKeys = mutableSetOf<TileKey>()
                // Prefetch 1 tile margin
                for (r in (startRow - 1)..(endRow + 1)) {
                    for (c in (startCol - 1)..(endCol + 1)) {
                        requiredKeys.add(TileKey(c, r, discreteScale))
                    }
                }
                activeTiles = requiredKeys
            }
    }

    // 2. Fetch Active Tiles & Handle Page Updates
    LaunchedEffect(activeTiles, updateTrigger, page) {
        if (updateTrigger is Page.UpdateTrigger.Incremental || updateTrigger is Page.UpdateTrigger.Full) {
             // Invalidate immediately
             tileCache.clear()
             redrawTrigger++
        }
        
        withContext(Dispatchers.Default) {
             page.mutex.withLock {
                 if (!page.isPrepared) {
                     page.prepare()
                 }
             }
        }
        
        if (layoutCoordinates == null) return@LaunchedEffect
        val coords = layoutCoordinates!!
        val intrinsicPageWidth = coords.size.width.toFloat()
        val intrinsicPageHeight = coords.size.height.toFloat()

        // Cleanup: remove old tiles from BOTH outside the region and older scales
        val activeTilesByColRow = activeTiles.map { Pair(it.col, it.row) }.toSet()
        val firstScale = activeTiles.firstOrNull()?.discreteScale ?: 1f
        
        val keysToRemove = tileCache.keys().toList().filter { key ->
            val isGivenScale = activeTiles.any { it.col == key.col && it.row == key.row && it.discreteScale == key.discreteScale }
            val isOutside = !activeTilesByColRow.contains(Pair(key.col, key.row))
            val hasBetterScaleReady = !isGivenScale && tileCache.containsKey(TileKey(key.col, key.row, firstScale))
            isOutside || hasBetterScaleReady || (updateTrigger is Page.UpdateTrigger.Incremental)
        }
        
        var changed = false
        for (k in keysToRemove) {
            if (tileCache.remove(k) != null) changed = true
        }
        if (changed) redrawTrigger++

        // Generate tiles
        for (key in activeTiles) {
            if (!tileCache.containsKey(key)) {
                withContext(Dispatchers.Default) {
                    if (tileCache.containsKey(key)) return@withContext
                    
                    val tilePixelX = key.col * TILE_SIZE.toFloat()
                    val tilePixelY = key.row * TILE_SIZE.toFloat()
                    
                    val pageDrawWidth = intrinsicPageWidth * key.discreteScale
                    val pageDrawHeight = intrinsicPageHeight * key.discreteScale
                    
                    val dstRectF = RectF(
                        -tilePixelX,
                        -tilePixelY,
                        -tilePixelX + pageDrawWidth,
                        -tilePixelY + pageDrawHeight
                    )
                    
                    val generated = pageMaker.makePage(
                        bitmapRect = AndroidRect(0, 0, TILE_SIZE, TILE_SIZE),
                        bitmapSource = null, 
                        page = page,
                        clipRect = dstRectF,
                        reuseBitmap = null
                    )
                    
                    tileCache.put(key, generated)
                }
                redrawTrigger++
            }
        }
    }

    Box(modifier = modifier.onGloballyPositioned { layoutCoordinates = it }) {
        androidx.compose.runtime.key(redrawTrigger) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val entries = tileCache.entries.toList()
                for (entry in entries) {
                    val key = entry.key
                    val bitmap = entry.value
                    val tileIntrinsicSize = TILE_SIZE / key.discreteScale
                    val intrinsicX = key.col * tileIntrinsicSize
                    val intrinsicY = key.row * tileIntrinsicSize
                    
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(intrinsicX.toInt(), intrinsicY.toInt()),
                        dstSize = IntSize(tileIntrinsicSize.toInt(), tileIntrinsicSize.toInt())
                    )
                }
            }
        }
    }
}
