package com.studiomath.pencilnotes.ui.composeView

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.studiomath.pencilnotes.document.DrawView
import com.studiomath.pencilnotes.file.DrawViewModel

@Composable
fun DrawCompose(modifier: Modifier = Modifier, drawViewModel: DrawViewModel) {

    AndroidView(
        modifier = Modifier
            .fillMaxSize(),

        factory = { context ->
            DrawView(context = context, drawViewModel = drawViewModel)
        }
    )

}