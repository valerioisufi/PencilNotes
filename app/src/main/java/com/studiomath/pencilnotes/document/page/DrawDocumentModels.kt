package com.studiomath.pencilnotes.document.page

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

/**
 * Data models for the drawing document structure.
 */

@Serializable
data class Resource(@SerialName("i") val id: String, @SerialName("t") var type: ResourceType) {
    enum class ResourceType {
        PDF, IMAGE, COLOR
    }

    @SerialName("c") var content = ""
}

enum class DataType(val value: Int) {
    STROKE(0), IMAGE(1), TEXT(2), PDF(3)
}

@Serializable
data class Stroke(@SerialName("z") val zIndex: Int) {
    fun toSerializedStroke() {
        if (stroke == null) return
        color = stroke!!.brush.colorIntArgb
        size = stroke!!.brush.size

        brush =
            when (stroke!!.brush.family){
                StockBrushes.pressurePen() -> BrushFamily.PRESSURE_PEN
                StockBrushes.highlighter() -> BrushFamily.HIGHLIGHTER
                StockBrushes.marker() -> BrushFamily.MARKER
                else -> BrushFamily.MARKER
            }

        toolType =
            when (stroke!!.inputs.getToolType()){
                InputToolType.STYLUS -> ToolType.STYLUS
                InputToolType.TOUCH -> ToolType.TOUCH
                InputToolType.MOUSE -> ToolType.MOUSE
                else -> ToolType.UNKNOWN
            }

        val scratchInput = androidx.ink.strokes.StrokeInput()
        for (i in 0 until stroke!!.inputs.size) {
            stroke!!.inputs.populate(i, scratchInput)
            inputs.add(
                StrokeInput(
                    x = scratchInput.x,
                    y = scratchInput.y
                ).apply {
                    timeMillis = scratchInput.elapsedTimeMillis.toFloat()
                    strokeUnitLengthCm = if(scratchInput.strokeUnitLengthCm != androidx.ink.strokes.StrokeInput.NO_STROKE_UNIT_LENGTH) scratchInput.strokeUnitLengthCm else null
                    pressure = if(scratchInput.pressure != androidx.ink.strokes.StrokeInput.NO_PRESSURE) scratchInput.pressure else null
                    tilt = if(scratchInput.tiltRadians != androidx.ink.strokes.StrokeInput.NO_TILT) scratchInput.tiltRadians else null
                    orientation = if(scratchInput.orientationRadians != androidx.ink.strokes.StrokeInput.NO_ORIENTATION) scratchInput.orientationRadians else null
                }
            )
        }

    }
    fun toInkStroke() {
        val toolType =
            when (toolType){
                ToolType.STYLUS -> InputToolType.STYLUS
                ToolType.TOUCH -> InputToolType.TOUCH
                ToolType.MOUSE -> InputToolType.MOUSE
                else -> InputToolType.UNKNOWN
            }
        val batch = MutableStrokeInputBatch()
        inputs.forEach { input ->
            batch.add(
                type = toolType,
                x = input.x,
                y = input.y,
                elapsedTimeMillis = input.timeMillis.toLong(),
                strokeUnitLengthCm = if(input.strokeUnitLengthCm != null) input.strokeUnitLengthCm!! else androidx.ink.strokes.StrokeInput.NO_STROKE_UNIT_LENGTH,
                pressure = if(input.pressure != null) input.pressure!! else androidx.ink.strokes.StrokeInput.NO_PRESSURE,
                tiltRadians  = if(input.tilt != null) input.tilt!! else androidx.ink.strokes.StrokeInput.NO_TILT,
                orientationRadians = if(input.orientation != null) input.orientation!! else androidx.ink.strokes.StrokeInput.NO_ORIENTATION
            )
        }

        val brushFamily =
            when (brush){
                BrushFamily.PRESSURE_PEN -> StockBrushes.pressurePen()
                BrushFamily.HIGHLIGHTER -> StockBrushes.highlighter()
                BrushFamily.MARKER -> StockBrushes.marker()
            }
        val brush = Brush.createWithColorIntArgb(
            family = brushFamily,
            colorIntArgb = color,
            size = size,
            epsilon = 0.005f,
        )

        stroke = androidx.ink.strokes.Stroke(brush, batch)
    }

    enum class ToolType {
        STYLUS, TOUCH, MOUSE, UNKNOWN
    }

    enum class BrushFamily {
        PRESSURE_PEN, HIGHLIGHTER, MARKER
    }

    // TODO: utlizzare invece una lista di float
    @Serializable
    data class StrokeInput(
        @SerialName("x") var x: Float = 0f, @SerialName("y") var y: Float = 0f
    ) {
        @SerialName("m") var timeMillis: Float = 0f
        @SerialName("l") var strokeUnitLengthCm: Float? = null
        @SerialName("p") var pressure: Float? = null
        @SerialName("t") var tilt: Float? = null
        @SerialName("o") var orientation: Float? = null
    }

    @SerialName("tT") var toolType = ToolType.UNKNOWN
    @SerialName("b") var brush: BrushFamily = BrushFamily.PRESSURE_PEN
    @SerialName("i") var inputs = mutableListOf<StrokeInput>()

    @SerialName("s") var size: Float = 8f
    @SerialName("c") var color: Int = 0xFFFFFF

    @Transient
    var stroke: androidx.ink.strokes.Stroke? = null
}

@Serializable
data class Image(@SerialName("z") val zIndex: Int) {
    @SerialName("i") var id: String = ""
}

@Serializable
data class Pdf(@SerialName("z") val zIndex: Int) {
    @SerialName("i") var id: String = ""
}

@Serializable
data class Page(@SerialName("i") val index: Int) {
    @Transient // ID della pagina nel database
    var dbId: Int = 0

    //        var creationDate: LocalDate = LocalDate.now()
    @SerialName("w") var width = 0f // mm
    @SerialName("h") var height = 0f // mm

    @Transient
    var dimension: Dimension? = null

    fun rect(): RectF {
        return RectF(0f, 0f, width, height)
    }

    @Transient
    var mutex = Mutex()

    @Transient
    var version by mutableIntStateOf(0)

    /**
     * bitmapPage e canvasPage servono solo come cache da
     * utlizzare per esempio durante lo scaling o lo scorrimento
     * tra le pagine
     */
    @Transient
    var bitmapPage: Bitmap? = null

    // TODO: introdurre una variabile mutableStateOf di tipo boolean che avverta se bitmapPage è aggiornato o meno

    @Transient // Flag per tracciare le modifiche e salvare solo le pagine modificate
    var isModified = false
    
    sealed class UpdateTrigger {
        object None : UpdateTrigger()
        data class Incremental(val version: Int) : UpdateTrigger()
        data class Full(val version: Int) : UpdateTrigger()
    }

    @Transient
    var updateTrigger by mutableStateOf<UpdateTrigger>(UpdateTrigger.None)

    /**
     * grafica contenuta nella pagina
     */
    @SerialName("sD") val strokeData = mutableListOf<Stroke>()
    @SerialName("iD") val imageData = mutableListOf<Image>()
    @SerialName("pD") val pdfData = mutableListOf<Pdf>()

    @Transient
    var isPrepared = false
    fun prepare() {
        dimension = Dimension(width.mm, height.mm)

        bitmapPage = createBitmap(
            dimension!!.calcWidthFromResolutionPxInch(resolutionPxInchPageDefault)
                .toInt(),
            dimension!!.calcHeightFromResolutionPxInch(resolutionPxInchPageDefault)
                .toInt()
        )
        strokeData.forEach { stroke ->
            stroke.toInkStroke()
        }

        isPrepared = true
    }
}

/**
 * Used for storing page content in the database.
 * This class wraps all the drawn content of a page.
 */
@Serializable
data class PageContent(
    @SerialName("sD") val strokeData: List<Stroke> = emptyList(),
    @SerialName("iD") val imageData: List<Image> = emptyList(),
    @SerialName("pD") val pdfData: List<Pdf> = emptyList()
)

@Serializable
data class Document(@SerialName("n") val name: String) {
    @SerialName("p") val pages = mutableListOf<Page>()
    @SerialName("r") val resources = mutableListOf<Resource>() // key = resourceId
    
    @Transient
    var dbId: Int = 0
}

// TODO: da utilizzare per ridurre il numero di cifre salvate nella serializzazione 
class FloatStrokeInputSerializer : KSerializer<Float> {
    override val descriptor = PrimitiveSerialDescriptor("value_name", PrimitiveKind.FLOAT)

    override fun deserialize(decoder: Decoder): Float {
        return decoder.decodeFloat()
    }

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat((value * 1000).roundToInt() / 1000f)
    }
}
