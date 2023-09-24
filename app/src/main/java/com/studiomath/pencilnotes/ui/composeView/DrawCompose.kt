package com.studiomath.pencilnotes.ui.composeView

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.studiomath.pencilnotes.FastRenderer
import com.studiomath.pencilnotes.LowLatencySurfaceView
import com.studiomath.pencilnotes.document.DrawView
import com.studiomath.pencilnotes.file.DrawViewModel

@Composable
fun DrawCompose(modifier: Modifier = Modifier, drawViewModel: DrawViewModel) {

//    AndroidView(
//        modifier = Modifier
//            .fillMaxSize(),
//
//        factory = { context ->
//            DrawView(context = context, drawViewModel = drawViewModel)
//        }
//    )

    var fastRendering: FastRenderer = remember {
        FastRenderer(drawViewModel)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize(),

        factory = { context ->
            LowLatencySurfaceView(context = context, fastRenderer = fastRendering)
        }
    )

}