package com.studiomath.pencilnotes.document.compose.lazyDocument.state

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

sealed class AnimState

class Appearing : AnimState(){
    var alpha = Animatable(0f)
}

class DocumentViewerState {
    var pages = listOf<PageInfo>()

    data class PageInfo(
        val key: Any,
        val offset: Offset,
        val size: Size
    ){
        var alpha = Animatable(1f)


    }


}