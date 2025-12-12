package com.studiomath.pencilnotes.document.compose

import android.graphics.Rect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.studiomath.pencilnotes.document.page.Page
import com.studiomath.pencilnotes.document.page.PageMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PageComposable(
    modifier: Modifier = Modifier,
    page: Page,
    pageMaker: PageMaker
) {
    // Observe version to trigger recomposition when strokes are added
    // Observe updateTrigger to distinguish between Full and Incremental updates
    val updateTrigger = page.updateTrigger
    var bitmap by remember { mutableStateOf(page.bitmapPage) }
    // Key to force recomposition of Image even if bitmap reference stays same (for mutable bitmaps)
    var redrawKey by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    LaunchedEffect(page, updateTrigger) {
        when(updateTrigger) {
            is Page.UpdateTrigger.Full -> {
                 withContext(Dispatchers.Default) {
                     if (!page.isPrepared) {
                         page.prepare()
                     }
                     val cachedBitmap = page.bitmapPage
                     if (cachedBitmap != null) {
                        val result = pageMaker.makePage(
                            bitmapRect = Rect(0, 0, cachedBitmap.width, cachedBitmap.height),
                            bitmapSource = cachedBitmap,
                            page = page
                        )
                        page.bitmapPage = result
                     }
                 }
                 bitmap = page.bitmapPage
                 redrawKey++
            }
            is Page.UpdateTrigger.Incremental -> {
                // The bitmap has already been modified in place (canvas draw).
                // We just need to ensure the UI refreshes.
                // If bitmap reference changed (rare for incremental), update it.
                if (bitmap != page.bitmapPage) {
                    bitmap = page.bitmapPage
                }
                redrawKey++
            }
            Page.UpdateTrigger.None -> {
                // Initial load if bitmap is missing
                if (bitmap == null) {
                    withContext(Dispatchers.Default) {
                         if (!page.isPrepared) {
                             page.prepare()
                         }
                         val cachedBitmap = page.bitmapPage
                          if (cachedBitmap != null) {
                            val result = pageMaker.makePage(
                                bitmapRect = Rect(0, 0, cachedBitmap.width, cachedBitmap.height),
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

    Box(modifier = modifier) {
        if (bitmap != null) {
            androidx.compose.runtime.key(redrawKey) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(), // Create new ImageBitmap wrapper forces redraw if bitmap content changed
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}