package com.studiomath.pencilnotes.ui.composeView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.studiomath.pencilnotes.document.FastRenderer
import com.studiomath.pencilnotes.document.DrawView
import com.studiomath.pencilnotes.document.LowLatencySurfaceView
import com.studiomath.pencilnotes.file.DrawViewModel

@Composable
fun DrawCompose(modifier: Modifier = Modifier, drawViewModel: DrawViewModel) {

    Box {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),

            factory = { context ->
                DrawView(context = context, drawViewModel = drawViewModel)
            }
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize(),

            factory = { context ->
                LowLatencySurfaceView(
                    context = context,
                    drawViewModel = drawViewModel
                )
            }
        )
    }

}