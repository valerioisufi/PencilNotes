package com.studiomath.pencilnotes.document.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.studiomath.pencilnotes.document.page.DrawDocumentData
import android.graphics.Bitmap

@Composable
fun PageComposable(
    page: DrawDocumentData.Page,
    bitmap: Bitmap?, // Bitmap dalla cache
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(page.dimension!!.width.mm / page.dimension!!.height.mm)
    ) {
        // Disegna la bitmap di cache se esiste
        bitmap?.let {
            drawImage(it.asImageBitmap())
        }
        // Qui puoi anche disegnare altro, come il numero di pagina, etc.
    }
}