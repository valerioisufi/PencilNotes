package com.studiomath.pencilnotes.ui.composeComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.document.DrawViewModel.ToolUtilities
import com.studiomath.pencilnotes.document.page.pt

@Composable
fun PresetToolButton(
    preset: DrawViewModel.ToolPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onUpdate: (DrawViewModel.ToolPreset) -> Unit,
    onRemove: () -> Unit,
    windowInsetsController: WindowInsetsControllerCompat? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        val selectedModifier = if (isSelected) {
            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        } else {
            Modifier
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { expanded = true },
                    role = Role.Button,
                )
                .then(selectedModifier)
                .padding(4.dp), 
            contentAlignment = Alignment.Center
        ) {
            // Main Tool Icon
            val iconRes = when (preset.toolType) {
                ToolUtilities.Tool.INK_PEN -> R.drawable.icon_ink_pen
                ToolUtilities.Tool.INK_HIGHLIGHTER -> R.drawable.icon_ink_highlighter
                ToolUtilities.Tool.ERASER -> R.drawable.icon_ink_eraser
                ToolUtilities.Tool.LAZO -> R.drawable.icon_lasso_select
                ToolUtilities.Tool.TEXT -> R.drawable.icon_text_fields
                ToolUtilities.Tool.PAN -> R.drawable.icon_pan_tool
            }
            
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color(preset.color),
                modifier = Modifier.size(24.dp)
            )

            // Size Indicator (Small dot at the bottom right)
            if (preset.toolType != ToolUtilities.Tool.ERASER && preset.toolType != ToolUtilities.Tool.PAN && preset.toolType != ToolUtilities.Tool.LAZO) {
               // Optional: Add a visual indicator for size, but the icon tint is good for now.
               // Maybe a small circle stroke?
            }
        }

        DropdownMenu(
            modifier = Modifier.width(300.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tool Type Selector
                Text(text = "Strumento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tools = listOf(
                        ToolUtilities.Tool.INK_PEN to R.drawable.icon_ink_pen,
                        ToolUtilities.Tool.INK_HIGHLIGHTER to R.drawable.icon_ink_highlighter, 
                        ToolUtilities.Tool.ERASER to R.drawable.icon_ink_eraser
                    ) // Add more if needed
                    
                    tools.forEach { (type, res) ->
                        val isTypeSelected = preset.toolType == type
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isTypeSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .combinedClickable(onClick = { 
                                    onUpdate(preset.copy(toolType = type))
                                }),
                            contentAlignment = Alignment.Center
                        ){
                            Icon(painter = painterResource(id = res), contentDescription = null)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Color Selection
                Text(text = "Colore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ColorWheel(
                    color = Color(preset.color),
                    onColorChanged = {
                        onUpdate(preset.copy(color = it.toArgb()))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Size Selection
                Text(text = "Dimensione", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // Local state for slider to be smooth, update model on finish? 
                // Or just direct update. ToolButton in DrawActivity used direct update.
                SizeSlider(
                    modifier = Modifier.padding(8.dp),
                    size = preset.size.pt,
                    onSizeChanged = {
                        onUpdate(preset.copy(size = it.pt))
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Delete Button
                TextButton(
                    onClick = { 
                        onRemove()
                        expanded = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elimina Preset")
                }
            }
        }
    }
}
