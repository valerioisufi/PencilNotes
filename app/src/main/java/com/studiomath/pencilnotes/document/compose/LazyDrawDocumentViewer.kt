package com.studiomath.pencilnotes.document.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.document.compose.lazyDocument.LazyDocumentViewer
import com.studiomath.pencilnotes.document.compose.lazyDocument.items

@Composable
fun LazyDrawDocumentViewer(
    modifier: Modifier = Modifier,
    drawViewModel: DrawViewModel
) {
    if (drawViewModel.data.isDocumentLoaded) {
        LazyDocumentViewer(
            modifier = modifier
        ) {
            items(
                items = drawViewModel.data.pagesState,
                key = { page -> page.index },
                itemSize = { page -> page.dimension!! }
            ) { page ->
                PageComposable(
                    modifier = Modifier.background(Color.White),
                    page = page,
                    pageMaker = drawViewModel.pageMaker
                )
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
