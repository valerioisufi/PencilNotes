package com.studiomath.pencilnotes.document

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studiomath.pencilnotes.document.page.Dimension.Companion.Length
import com.studiomath.pencilnotes.document.page.DrawDocumentRepository
import com.studiomath.pencilnotes.document.page.PageMaker
import com.studiomath.pencilnotes.document.page.pt
import com.studiomath.pencilnotes.document.page.px
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.io.File

class DrawViewModel(
    val context: Context,
    var documentId: Int,
    var displayMetrics: DisplayMetrics,
    var configuration: ViewConfiguration
) : ViewModel() {


    val pageMaker = PageMaker(displayMetrics)

    // Using DrawDocumentRepository instead of DrawDocumentData
    var repository: DrawDocumentRepository = DrawDocumentRepository(context, documentId, this)
    // Alias for compatibility if needed, but better to migrate consumers
    val data: DrawDocumentRepository get() = repository

    private val sharedPreferences = context.getSharedPreferences("pencil_notes_prefs", Context.MODE_PRIVATE)
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }


    @Serializable
    data class ToolUtilities(val toolType: Tool){
        enum class Tool {
            INK_PEN, INK_HIGHLIGHTER, ERASER, TEXT, LAZO, PAN
        }
        @Serializable
        data class BrushSettings(
            val size: Float,
            val color: Int
        )

        private var brushList = mutableListOf<BrushSettings>()

        fun getBrush(index: Int): Brush{
            if (index >= brushList.size) {
                when(toolType){
                    Tool.INK_PEN -> brushList.add(BrushSettings(3f, Color.BLUE))
                    Tool.INK_HIGHLIGHTER -> brushList.add(BrushSettings(15f, Color.argb(0.25f, 1f, 1f, 0f)))
                    Tool.ERASER -> brushList.add(BrushSettings(20f, Color.argb(0.8f, 1f, 1f, 1f)))
                    Tool.LAZO -> brushList.add(BrushSettings(2f, Color.argb(1f, 0.53f, 0.6f, 0.7f)))
                    else -> brushList.add(BrushSettings(4f, Color.BLACK))
                }
            }
            var family = when(toolType){
                Tool.INK_PEN -> StockBrushes.pressurePen()
                Tool.INK_HIGHLIGHTER -> StockBrushes.highlighter()
                Tool.LAZO -> StockBrushes.dashedLine()
                else -> StockBrushes.marker()
            }
            return Brush.createWithColorIntArgb(
                family = family,
                colorIntArgb = brushList[index].color,
                size = brushList[index].size,
                epsilon = 0.1F
            )
        }
    }
    val penTool = ToolUtilities(ToolUtilities.Tool.INK_PEN)
    val highlighterTool = ToolUtilities(ToolUtilities.Tool.INK_HIGHLIGHTER)
    val eraserTool = ToolUtilities(ToolUtilities.Tool.ERASER)
    val lazoTool = ToolUtilities(ToolUtilities.Tool.LAZO)

    var selectedTool by mutableStateOf(ToolUtilities.Tool.INK_PEN)
    var activeBrush by mutableStateOf(penTool.getBrush(0))


    /**
     * Returns the active brush with size scaled to World Pixels (Scale 1.0).
     * This is suitable for use with InProgressStrokes where we provide a Screen->World transform.
     */
    fun getActiveBrushForCompose(): Brush {
        // 1 pt = 1/72 inch.
        // Screen density (xdpi) = pixels per inch.
        // SizeInPx = SizeInPt * (xdpi / 72)
        val pxPerPt = displayMetrics.xdpi / 72f
        val sizeInPx = activeBrush.size * pxPerPt
        
        return activeBrush.copy(size = sizeInPx)
    }

    fun addStroke(pageIndex: Int, stroke: androidx.ink.strokes.Stroke, transformToPage: Matrix) {
        viewModelScope.launch {
            repository.documentMutex.withLock {
                if (pageIndex < 0 || pageIndex >= repository.document.pages.size) return@launch

                val page = repository.document.pages[pageIndex]
                val zIndex = page.strokeData.size

                // We need to transform the stroke to Page Coordinates.
                // Since androidx.ink.strokes.Stroke is immutable in its inputs, we recreate it.
                val inputs = stroke.inputs
                val batch = androidx.ink.strokes.MutableStrokeInputBatch()
                val scratchInput = androidx.ink.strokes.StrokeInput()

                // Matrix helper array
                val points = FloatArray(2)

                for (i in 0 until inputs.size) {
                    inputs.populate(i, scratchInput)
                    points[0] = scratchInput.x
                    points[1] = scratchInput.y
                    transformToPage.mapPoints(points)

                    batch.add(
                        type = inputs.getToolType(), // Assuming stroke has uniform tool type
                        x = points[0],
                        y = points[1],
                        elapsedTimeMillis = scratchInput.elapsedTimeMillis,
                        strokeUnitLengthCm = scratchInput.strokeUnitLengthCm,
                        pressure = scratchInput.pressure,
                        tiltRadians = scratchInput.tiltRadians,
                        orientationRadians = scratchInput.orientationRadians
                    )
                }

                val transformedStroke = androidx.ink.strokes.Stroke(stroke.brush, batch)

                val newStroke = com.studiomath.pencilnotes.document.page.Stroke(zIndex).apply {
                    this.stroke = transformedStroke
                    toSerializedStroke()
                }

                page.strokeData.add(newStroke)

                // Incremental Update Logic
                if (page.bitmapPage != null) {
                    val canvas = android.graphics.Canvas(page.bitmapPage!!)
                    val inkStroke = newStroke.stroke
                    if (inkStroke != null) {
                        pageMaker.canvasStrokeRenderer.draw(
                            stroke = inkStroke,
                            canvas = canvas,
                            strokeToScreenTransform = Matrix() // Data is already in Page Pixel coordinates
                        )
                    }
                }

                // Trigger Incremental Update
                page.version++
                page.updateTrigger =
                    com.studiomath.pencilnotes.document.page.Page.UpdateTrigger.Incremental(page.version)
                page.isModified = true // Mark dirty
            }
            // Mark for saving (after lock is released)
            repository.saveDocument()
        }
    }



    var finishActivity: (() -> Unit)? = null

    @Serializable
    data class ToolPreset(
        val id: String = java.util.UUID.randomUUID().toString(),
        var toolType: ToolUtilities.Tool,
        var color: Int,
        var size: Float // in pt
    )

    var penPresets = androidx.compose.runtime.mutableStateListOf<ToolPreset>()
    var highlighterPresets = androidx.compose.runtime.mutableStateListOf<ToolPreset>()

    var activePenPresetId: String? = null
    var activeHighlighterPresetId: String? = null

    init {
        loadPresets()
    }

    private fun loadPresets() {
        // Load Pen Presets
        val penJson = sharedPreferences.getString("pen_presets", null)
        activePenPresetId = sharedPreferences.getString("active_pen_preset_id", null)
        if (penJson != null) {
            try {
                val list = json.decodeFromString<List<ToolPreset>>(penJson)
                penPresets.addAll(list)
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Defaults
            penPresets.add(ToolPreset(toolType = ToolUtilities.Tool.INK_PEN, color = Color.BLACK, size = 3f))
            penPresets.add(ToolPreset(toolType = ToolUtilities.Tool.INK_PEN, color = Color.BLUE, size = 3f))
            penPresets.add(ToolPreset(toolType = ToolUtilities.Tool.INK_PEN, color = Color.RED, size = 3f))
            activePenPresetId = penPresets.first().id
        }

        // Load Highlighter Presets
        val highlighterJson = sharedPreferences.getString("highlighter_presets", null)
        activeHighlighterPresetId = sharedPreferences.getString("active_highlighter_preset_id", null)
        if (highlighterJson != null) {
            try {
                val list = json.decodeFromString<List<ToolPreset>>(highlighterJson)
                highlighterPresets.addAll(list)
            } catch (e: Exception) { e.printStackTrace() }
        } else {
             // Defaults
            highlighterPresets.add(ToolPreset(toolType = ToolUtilities.Tool.INK_HIGHLIGHTER, color = Color.YELLOW, size = 15f))
            highlighterPresets.add(ToolPreset(toolType = ToolUtilities.Tool.INK_HIGHLIGHTER, color = Color.GREEN, size = 15f))
            activeHighlighterPresetId = highlighterPresets.first().id
        }
    }

    private fun savePresets() {
        val penJson = json.encodeToString(penPresets.toList())
        val highlighterJson = json.encodeToString(highlighterPresets.toList())
        sharedPreferences.edit()
            .putString("pen_presets", penJson)
            .putString("highlighter_presets", highlighterJson)
            .putString("active_pen_preset_id", activePenPresetId)
            .putString("active_highlighter_preset_id", activeHighlighterPresetId)
            .apply()
    }

    fun setActivePreset(preset: ToolPreset) {
        if (preset.toolType == ToolUtilities.Tool.INK_PEN) {
            activePenPresetId = preset.id
        } else if (preset.toolType == ToolUtilities.Tool.INK_HIGHLIGHTER) {
            activeHighlighterPresetId = preset.id
        }
        
        // Also update the active brush immediately
        activeBrush = activeBrush.copyWithColorIntArgb(
             size = preset.size,
             colorIntArgb = preset.color
        )
        savePresets()
    }

    fun addPreset(type: ToolUtilities.Tool) {
        val newPreset = if (type == ToolUtilities.Tool.INK_PEN) {
             ToolPreset(toolType = ToolUtilities.Tool.INK_PEN, color = Color.BLACK, size = 5f)
        } else {
             ToolPreset(toolType = ToolUtilities.Tool.INK_HIGHLIGHTER, color = Color.YELLOW, size = 15f)
        }
        
        if (type == ToolUtilities.Tool.INK_PEN) penPresets.add(newPreset)
        else highlighterPresets.add(newPreset)
        
        savePresets()
    }

    fun updatePreset(preset: ToolPreset) {
         if (preset.toolType == ToolUtilities.Tool.INK_PEN) {
             val index = penPresets.indexOfFirst { it.id == preset.id }
             if (index != -1) penPresets[index] = preset
         } else {
             val index = highlighterPresets.indexOfFirst { it.id == preset.id }
             if (index != -1) highlighterPresets[index] = preset
         }
         savePresets()
    }

    fun removePreset(preset: ToolPreset) {
        if (preset.toolType == ToolUtilities.Tool.INK_PEN) penPresets.remove(preset)
        else highlighterPresets.remove(preset)
        savePresets()
    }
}
