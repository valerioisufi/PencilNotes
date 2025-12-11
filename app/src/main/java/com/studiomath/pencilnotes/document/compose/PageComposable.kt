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
import com.studiomath.pencilnotes.document.page.DrawDocumentData
import com.studiomath.pencilnotes.document.page.PageMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PageComposable(
    modifier: Modifier = Modifier,
    page: DrawDocumentData.Page,
    pageMaker: PageMaker
) {
    // Observe version to trigger recomposition when strokes are added
    val version = page.version 
    var bitmap by remember(page, version) { mutableStateOf(page.bitmapPage) }

    LaunchedEffect(page, version) {
        // We move preparation to Default dispatcher to avoid blocking UI if prepare() is heavy
        // although prepare() in DrawDocumentData uses createBitmap which is somewhat fast but better safe.
        // makePage uses withContext(Dispatchers.Default) internally, but we need the bitmap BEFORE calling it
        // if we want to reuse it.
        
        withContext(Dispatchers.Default) {
             if (!page.isPrepared) {
                 page.prepare()
             }
        }
        
        val cachedBitmap = page.bitmapPage
        if (cachedBitmap != null) {
            // Use the cached bitmap as source. PageMaker will draw onto it.
            // We pass the bitmap's dimensions as the rect, ensuring a match.
            val result = pageMaker.makePage(
                bitmapRect = Rect(0, 0, cachedBitmap.width, cachedBitmap.height),
                bitmapSource = cachedBitmap,
                page = page
            )
            
            bitmap = result
            page.bitmapPage = result
        }
    }

    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(), // Create new ImageBitmap wrapper forces redraw if bitmap content changed
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}