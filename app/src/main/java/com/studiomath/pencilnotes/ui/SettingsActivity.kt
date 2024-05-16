package com.studiomath.pencilnotes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.studiomath.pencilnotes.ui.theme.PencilNotesTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PencilNotesTheme {
                SettingsActivity(Modifier)
            }
        }
    }
}

@Composable
fun SettingsActivity(modifier: Modifier) {
    Scaffold (

    ){

    }

}