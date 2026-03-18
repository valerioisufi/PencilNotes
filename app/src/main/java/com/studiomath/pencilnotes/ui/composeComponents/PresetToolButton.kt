//package com.studiomath.pencilnotes.ui.composeComponents
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.combinedClickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.outlined.Delete
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.DropdownMenu
//import androidx.compose.material3.HorizontalDivider
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberUpdatedState
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.toArgb
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.semantics.Role
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.core.view.WindowInsetsCompat
//import androidx.core.view.WindowInsetsControllerCompat
//import com.studiomath.pencilnotes.R
//import com.studiomath.pencilnotes.document.DrawViewModel
//import com.studiomath.pencilnotes.document.DrawViewModel.ToolUtilities
//import com.studiomath.pencilnotes.document.page.pt
//
//@Composable
//fun PresetToolButton(
//    preset: DrawViewModel.ToolPreset,
//    isSelected: Boolean,
//    onClick: () -> Unit,
//    onUpdate: (DrawViewModel.ToolPreset) -> Unit,
//    onRemove: () -> Unit,
//    windowInsetsController: WindowInsetsControllerCompat? = null
//) {
//    var expanded by remember { mutableStateOf(false) }
//
//    val currentPreset by rememberUpdatedState(preset)
//    val currentOnUpdate by rememberUpdatedState(onUpdate)
//    val currentOnRemove by rememberUpdatedState(onRemove)
//
//    val handleColorChange = remember {
//        { newColor: Color ->
//            currentOnUpdate(currentPreset.copy(color = newColor.toArgb()))
//        }
//    }
//
//    val handleSizeChange = remember {
//        { newSize: com.studiomath.pencilnotes.document.page.Measure ->
//            currentOnUpdate(currentPreset.copy(size = newSize.pt))
//        }
//    }
//
//    val handleDelete = remember {
//        {
//            currentOnRemove()
//            expanded = false
//        }
//    }
//
//    // Stable Color for ColorWheel
//    val stableColor = remember(preset.id) { Color(preset.color) }
//
//    Box {
//        // Main Button UI
//        Box(
//            modifier = Modifier
//                .size(40.dp) // Slightly larger for better touch target
//                .padding(4.dp)
//                .clip(CircleShape)
//                .background(Color(preset.color))
//                .combinedClickable(
//                    onClick = { onClick() },
//                    onLongClick = { expanded = true },
//                    role = Role.Button,
//                )
//                .then(
//                     if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            if (isSelected) {
//                Icon(
//                    imageVector = Icons.Default.Check, // Need to make sure this import exists or use a resource
//                    contentDescription = null,
//                    tint = if (androidx.core.graphics.ColorUtils.calculateLuminance(preset.color) > 0.5) Color.Black else Color.White,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//        }
//
//        DropdownMenu(
//            modifier = Modifier.width(300.dp),
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
//
//            Column(
//                modifier = Modifier.padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                // Color Selection
//                Text(text = "Colore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
//                Spacer(modifier = Modifier.height(8.dp))
//                ColorWheel(
//                    color = stableColor,
//                    onColorChanged = handleColorChange
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Size Selection
//                Text(text = "Dimensione", fontSize = 16.sp, fontWeight = FontWeight.Bold)
//                Spacer(modifier = Modifier.height(8.dp))
//
//                SizeSlider(
//                    modifier = Modifier.padding(8.dp),
//                    size = currentPreset.size.pt,
//                    onSizeChanged = handleSizeChange
//                )
//
//                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
//
//                // Delete Button
//                TextButton(
//                    onClick = handleDelete,
//                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
//                ) {
//                    Icon(Icons.Outlined.Delete, contentDescription = null)
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Elimina Preset")
//                }
//            }
//        }
//    }
//}
