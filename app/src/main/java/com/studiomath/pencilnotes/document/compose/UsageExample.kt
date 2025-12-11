package com.studiomath.pencilnotes.document.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.studiomath.pencilnotes.document.compose.lazyDocument.LazyDocumentViewer
import com.studiomath.pencilnotes.document.compose.lazyDocument.items
import com.studiomath.pencilnotes.document.page.DrawDocumentData
import com.studiomath.pencilnotes.document.page.PageMaker

@Composable
fun LazyDocumentViewerExample(
    pages: List<DrawDocumentData.Page>,
    pageMaker: PageMaker
) {
    LazyDocumentViewer {
        items(
            items = pages,
            key = { page -> page.index },
            itemSize = { page -> page.dimension!! } // Assumes page.dimension is set (e.g. prepared) or you calculate it
        ) { page ->
             PageComposable(
                 modifier = Modifier, 
                 page = page,
                 pageMaker = pageMaker
             )
        }
    }
}
