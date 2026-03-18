package com.studiomath.pencilnotes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.studiomath.drawview.DrawRoute
import com.studiomath.pencilnotes.ui.theme.PencilNotesTheme

class DrawActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val documentId = intent.getIntExtra("documentId", -1)

        setContent {
            PencilNotesTheme { // Usa il tema della tua app principale!

                // Chiama la Route del modulo importato
                DrawRoute(
                    documentId = documentId,
                    onNavigateBack = { finish() } // Qui diciamo all'Activity di chiudersi
                )

            }
        }
    }

    // Gestione Insets invariata...
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
