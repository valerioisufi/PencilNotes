package com.studiomath.pencilnotes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studiomath.pencilnotes.file.DrawViewModel
import com.studiomath.pencilnotes.ui.composeView.DrawCompose
import com.studiomath.pencilnotes.ui.theme.PencilNotesTheme

class DrawActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val filePath = intent.getStringExtra("filePath")

        val drawViewModel = viewModels<DrawViewModel> {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DrawViewModel(
                        filePath = "$filePath", filesDir = filesDir
                    ) as T
                }
            }
        }.value

        WindowCompat.setDecorFitsSystemWindows(window, false)
//        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
//            controller.hide(WindowInsetsCompat.Type.systemBars())
//            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        }

        setContent {
            PencilNotesTheme {
                DrawActivity(drawViewModel = drawViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawActivity(modifier: Modifier = Modifier, drawViewModel: DrawViewModel) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "")
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "")
            Icon(imageVector = Icons.Default.Home, contentDescription = "")
            Icon(imageVector = Icons.Default.Search, contentDescription = "")
        }

        DrawCompose(
            drawViewModel = drawViewModel
        )
    }

}