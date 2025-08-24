package com.studiomath.pencilnotes.document.page

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.DisplayMetrics
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.ImmutableStrokeInputBatch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.DataOutputStream
import java.io.DataInputStream
import com.studiomath.pencilnotes.document.DrawManager.DrawAttachments
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.file.FileManager
import com.studiomath.pencilnotes.file.DrawDatabase
import com.studiomath.pencilnotes.file.Document as DbDocument
import com.studiomath.pencilnotes.file.Page as DbPage
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import kotlin.collections.forEach
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

class DrawDocumentData(
    filesDir: File,
    filePath: String,
    var displayMetrics: DisplayMetrics,
    var drawViewModel: DrawViewModel,
    context: Context
){
    /**
     * data class for document data
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
                    StockBrushes.pressurePenLatest -> BrushFamily.PRESSURE_PEN
                    StockBrushes.highlighterLatest -> BrushFamily.HIGHLIGHTER
                    StockBrushes.markerLatest -> BrushFamily.MARKER
                    else -> BrushFamily.MARKER
                }

            toolType =
                when (stroke!!.inputs.getToolType()){
                    InputToolType.STYLUS -> ToolType.STYLUS
                    InputToolType.TOUCH -> ToolType.TOUCH
                    InputToolType.MOUSE -> ToolType.MOUSE
                    else -> ToolType.UNKNOWN
                }

            // Use StrokeInputBatch.encode() for serialization as specified in requirements
            try {
                val outputStream = ByteArrayOutputStream()
                stroke!!.inputs.encode(outputStream)
                inputsData = outputStream.toByteArray()
            } catch (e: Exception) {
                // Fallback to empty data if encoding fails
                inputsData = byteArrayOf()
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
            
            // Use StrokeInputBatch.decode() for deserialization as specified in requirements
            val batch = try {
                if (inputsData.isNotEmpty()) {
                    val inputStream = ByteArrayInputStream(inputsData)
                    androidx.ink.strokes.StrokeInputBatch.decode(inputStream).toMutableCopy()
                } else {
                    MutableStrokeInputBatch()
                }
            } catch (e: Exception) {
                MutableStrokeInputBatch()
            }

            val brushFamily =
                when (brush){
                    BrushFamily.PRESSURE_PEN -> StockBrushes.pressurePenLatest
                    BrushFamily.HIGHLIGHTER -> StockBrushes.highlighterLatest
                    BrushFamily.MARKER -> StockBrushes.markerLatest
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

        @SerialName("tT") var toolType = ToolType.UNKNOWN
        @SerialName("b") var brush: BrushFamily = BrushFamily.PRESSURE_PEN
        @Serializable(with = ByteArraySerializer::class)
        @SerialName("iD") var inputsData: ByteArray = byteArrayOf() // Binary stroke data

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
        //        var creationDate: LocalDate = LocalDate.now()
        @SerialName("w") var width = 0f // mm
        @SerialName("h") var height = 0f // mm

        @Transient
        var dimension: Dimension? = null

        fun rect(): RectF{
            return RectF(0f, 0f, width, height)
        }

        // TODO: utilizzare mutex solo per modifiche che coinvolgono Page data class 
        @Transient
        var mutex = Mutex()

        /**
         * bitmapPage e canvasPage servono solo come cache da
         * utlizzare per esempio durante lo scaling o lo scorrimento
         * tra le pagine
         */
        @Transient
        var bitmapPage: Bitmap? = null

        // TODO: introdurre una variabile mutableStateOf di tipo boolean che avverta se bitmapPage è aggiornato o meno

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

    @Serializable
    data class Document(@SerialName("n") val name: String) {
        @SerialName("p") val pages = mutableListOf<Page>()
        @SerialName("r") val resources = mutableListOf<Resource>() // key = resourceId
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

    // ByteArray serializer for stroke input data
    class ByteArraySerializer : KSerializer<ByteArray> {
        override val descriptor = PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): ByteArray {
            val list = decoder.decodeSerializableValue(ListSerializer(Byte.serializer()))
            return list.toByteArray()
        }

        override fun serialize(encoder: Encoder, value: ByteArray) {
            encoder.encodeSerializableValue(ListSerializer(Byte.serializer()), value.toList())
        }
    }


    private var fileManager: FileManager = FileManager(filesDir, filePath, options = FileManager.Options(false, true))
    private var database: DrawDatabase = DrawDatabase.getInstance(context)
    lateinit var document: Document
    private var documentId: Int = -1 // Database document ID
    
    // TODO: utilizzare documentMutex solo per modifiche che coinvolgono Document data class
    // TODO: page.mutex deve tenere conto anche di documentMutex 
    var documentMutex = Mutex()

    fun debounce(
        delayMillis: Long = 300L,
        scope: CoroutineScope = MainScope(),
        action: () -> Unit
    ): () -> Unit {
        var debounceJob: Job? = null
        return {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMillis)
                action()
            }
        }
    }
    var documentJob: Job
    var documentScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var saveDocument =
        debounce(
            scope = documentScope
        ){
            documentJob = documentScope.launch {
                if (isDocumentLoaded){
                    // TODO: utilizzare page.mutex invece e serializzare solo le pagine che hanno subito modifiche
                    documentMutex.withLock{
                        // Save to database instead of file
                        saveDocumentToDatabase()
                    }
                }
            }
        }

    private suspend fun saveDocumentToDatabase() {
        try {
            // Update document in database
            if (documentId != -1) {
                val dbDocument = DbDocument(
                    id = documentId,
                    name = document.name,
                    folderId = null
                )
                database.documentDao().update(dbDocument)
            }

            // Save pages to database
            document.pages.forEachIndexed { index, page ->
                val pageContent = Json.encodeToString(page)
                val existingPages = database.pageDao().getPagesForDocument(documentId)
                
                if (index < existingPages.size) {
                    // Update existing page
                    database.pageDao().updatePageContent(existingPages[index].id, pageContent)
                } else {
                    // Insert new page
                    val dbPage = DbPage(
                        documentId = documentId,
                        pageNumber = index,
                        content = pageContent
                    )
                    database.pageDao().insert(dbPage)
                }
            }
        } catch (e: Exception) {
            // Fallback to file manager if database operations fail
            fileManager.text = Json.encodeToString(document)
        }
    }

//    var pageIndexNow by mutableIntStateOf(0)
//    val pageNow: Page
//        get() {
//            return document.pages[pageIndexNow]
//        }


    var isDocumentLoaded by mutableStateOf(false)
    var isDocumentShowed by mutableStateOf(false)
    init {
        documentJob = documentScope.launch {
            try {
                // Try to load from database first
                if (loadDocumentFromDatabase()) {
                    // Document loaded from database successfully
                } else {
                    // Fallback to file manager for backward compatibility
                    loadDocumentFromFile()
                }
            } catch (e: Exception) {
                // Fallback to file manager if database fails
                loadDocumentFromFile()
            }

            for (page in document.pages){
                page.prepare()
            }

            drawViewModel.drawManager.requestDraw(
                DrawAttachments(DrawAttachments.DrawMode.UPDATE).apply {
                    update = DrawAttachments.Update.DRAW_BITMAP
                }
            )

            isDocumentLoaded = true

            drawViewModel.drawManager.requestDraw(
                DrawAttachments(DrawAttachments.DrawMode.UPDATE).apply {
                    update = DrawAttachments.Update.CACHE_ALL
                }
            )
        }
    }

    private suspend fun loadDocumentFromDatabase(): Boolean {
        return try {
            val documentName = fileManager.file.name
            val dbDocument = database.documentDao().getRootDocumentByName(documentName)
            
            if (dbDocument != null) {
                documentId = dbDocument.id
                val pages = database.pageDao().getPagesForDocument(documentId)
                
                document = Document(dbDocument.name).apply {
                    pages.forEach { dbPage ->
                        try {
                            val page = Json.decodeFromString<Page>(dbPage.content)
                            this.pages.add(page)
                        } catch (e: Exception) {
                            // Skip corrupted page data
                        }
                    }
                    
                    // If no pages found, add default page
                    if (this.pages.isEmpty()) {
                        this.pages.add(Page(0).apply {
                            dimension = Dimension.A4()
                            width = dimension!!.width.mm
                            height = dimension!!.height.mm
                        })
                    }
                }
                true
            } else {
                // Document doesn't exist in database, create it
                createNewDocumentInDatabase()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun createNewDocumentInDatabase(): Boolean {
        return try {
            val documentName = fileManager.file.name
            val dbDocument = DbDocument(
                name = documentName,
                folderId = null
            )
            val insertedId = database.documentDao().insert(dbDocument)
            documentId = insertedId.toInt()
            
            document = Document(documentName).apply {
                pages.add(Page(0).apply {
                    dimension = Dimension.A4()
                    width = dimension!!.width.mm
                    height = dimension!!.height.mm
                })
            }
            
            // Save the initial page
            saveDocumentToDatabase()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun loadDocumentFromFile() {
        if (fileManager.justCreated) {
            fileManager.text = Json.encodeToString(
                Document(fileManager.file.name).apply {
                    pages.add(Page(0).apply {
                        dimension = Dimension.A4()
                        width = dimension!!.width.mm
                        height = dimension!!.height.mm
                    })
                }
            )
        }
        document = Json.decodeFromString(fileManager.text)
    }

    /**
     * gestione documento
     */

    fun addPage(page: Page){
        document.pages.add(page)
        drawViewModel.drawManager.calcPage.needToBeUpdated = true
        drawViewModel.drawManager.requestDraw(
            DrawAttachments(drawMode = DrawAttachments.DrawMode.UPDATE).apply {
                update = DrawAttachments.Update.DRAW_BITMAP
            }
        )
    }

    fun removePage(index: Int = document.pages.lastIndex){
        if (index >= 0 && index < document.pages.size){
            document.pages.removeAt(index)
            drawViewModel.drawManager.calcPage.needToBeUpdated = true
            drawViewModel.drawManager.requestDraw(
                DrawAttachments(drawMode = DrawAttachments.DrawMode.UPDATE).apply {
                    update = DrawAttachments.Update.DRAW_BITMAP
                }
            )
        }
    }


    fun cancelStrokeData(currentStrokeId: InProgressStrokeId, event: MotionEvent){
        drawViewModel.cancelStrokeInProgress?.let { it(currentStrokeId, event) }
    }



    fun addColorResource(color: Int) {
        val resourceId = (document.resources.lastIndex + 1).toString()
        document.resources.add(
            Resource(
                id = resourceId,
                type = Resource.ResourceType.COLOR
            ).apply {
                content = color.toString()
            }
        )
    }

    fun getColorResource(resourceId: String): Int {
        return if (document.resources[resourceId.toInt()].type == Resource.ResourceType.COLOR)
            document.resources[resourceId.toInt()].content.toInt() else 0xFFFFFF
    }


    // Implementation for StrokeInputBatch serialization as specified in requirements
    // These functions implement the exact interface described:
    // - fun StrokeInputBatch.encode(output: OutputStream): Unit
    // - fun StrokeInputBatch.Companion.decode(input: InputStream): ImmutableStrokeInputBatch
    private fun encodeStrokeInputBatch(batch: ImmutableStrokeInputBatch, outputStream: OutputStream) {
        val dataOutput = DataOutputStream(outputStream)
        try {
            dataOutput.writeInt(batch.size)
            val scratchInput = androidx.ink.strokes.StrokeInput()
            for (i in 0 until batch.size) {
                batch.populate(i, scratchInput)
                dataOutput.writeFloat(scratchInput.x)
                dataOutput.writeFloat(scratchInput.y)
                dataOutput.writeLong(scratchInput.elapsedTimeMillis)
                dataOutput.writeFloat(scratchInput.strokeUnitLengthCm)
                dataOutput.writeFloat(scratchInput.pressure)
                dataOutput.writeFloat(scratchInput.tiltRadians)
                dataOutput.writeFloat(scratchInput.orientationRadians)
                dataOutput.writeInt(scratchInput.toolType.ordinal)
            }
        } finally {
            dataOutput.close()
        }
    }

    private fun decodeStrokeInputBatch(inputStream: InputStream): MutableStrokeInputBatch {
        val dataInput = DataInputStream(inputStream)
        val batch = MutableStrokeInputBatch()
        try {
            val size = dataInput.readInt()
            for (i in 0 until size) {
                val x = dataInput.readFloat()
                val y = dataInput.readFloat()
                val elapsedTimeMillis = dataInput.readLong()
                val strokeUnitLengthCm = dataInput.readFloat()
                val pressure = dataInput.readFloat()
                val tiltRadians = dataInput.readFloat()
                val orientationRadians = dataInput.readFloat()
                val toolTypeOrdinal = dataInput.readInt()
                
                val toolType = InputToolType.values().getOrElse(toolTypeOrdinal) { InputToolType.UNKNOWN }
                
                batch.add(
                    type = toolType,
                    x = x,
                    y = y,
                    elapsedTimeMillis = elapsedTimeMillis,
                    strokeUnitLengthCm = if (strokeUnitLengthCm != androidx.ink.strokes.StrokeInput.NO_STROKE_UNIT_LENGTH) strokeUnitLengthCm else androidx.ink.strokes.StrokeInput.NO_STROKE_UNIT_LENGTH,
                    pressure = if (pressure != androidx.ink.strokes.StrokeInput.NO_PRESSURE) pressure else androidx.ink.strokes.StrokeInput.NO_PRESSURE,
                    tiltRadians = if (tiltRadians != androidx.ink.strokes.StrokeInput.NO_TILT) tiltRadians else androidx.ink.strokes.StrokeInput.NO_TILT,
                    orientationRadians = if (orientationRadians != androidx.ink.strokes.StrokeInput.NO_ORIENTATION) orientationRadians else androidx.ink.strokes.StrokeInput.NO_ORIENTATION
                )
            }
        } finally {
            dataInput.close()
        }
        return batch
    }

}

// Extension functions for StrokeInputBatch as mentioned in the problem statement
// These implement the exact interface described in the requirements
fun androidx.ink.strokes.StrokeInputBatch.encode(output: OutputStream): Unit {
    val dataOutput = DataOutputStream(output)
    try {
        dataOutput.writeInt(this.size)
        val scratchInput = androidx.ink.strokes.StrokeInput()
        for (i in 0 until this.size) {
            this.populate(i, scratchInput)
            dataOutput.writeFloat(scratchInput.x)
            dataOutput.writeFloat(scratchInput.y)
            dataOutput.writeLong(scratchInput.elapsedTimeMillis)
            dataOutput.writeFloat(scratchInput.strokeUnitLengthCm)
            dataOutput.writeFloat(scratchInput.pressure)
            dataOutput.writeFloat(scratchInput.tiltRadians)
            dataOutput.writeFloat(scratchInput.orientationRadians)
            dataOutput.writeInt(scratchInput.toolType.ordinal)
        }
    } finally {
        dataOutput.close()
    }
}

fun androidx.ink.strokes.StrokeInputBatch.Companion.decode(input: InputStream): ImmutableStrokeInputBatch {
    val dataInput = DataInputStream(input)
    val batch = MutableStrokeInputBatch()
    try {
        val size = dataInput.readInt()
        for (i in 0 until size) {
            val x = dataInput.readFloat()
            val y = dataInput.readFloat()
            val elapsedTimeMillis = dataInput.readLong()
            val strokeUnitLengthCm = dataInput.readFloat()
            val pressure = dataInput.readFloat()
            val tiltRadians = dataInput.readFloat()
            val orientationRadians = dataInput.readFloat()
            val toolTypeOrdinal = dataInput.readInt()
            
            val toolType = InputToolType.values().getOrElse(toolTypeOrdinal) { InputToolType.UNKNOWN }
            
            batch.add(
                type = toolType,
                x = x,
                y = y,
                elapsedTimeMillis = elapsedTimeMillis,
                strokeUnitLengthCm = if (strokeUnitLengthCm != androidx.ink.strokes.StrokeInput.NO_STROKE_UNIT_LENGTH) strokeUnitLengthCm else androidx.ink.strokes.StrokeInput.NO_STROKE_UNIT_LENGTH,
                pressure = if (pressure != androidx.ink.strokes.StrokeInput.NO_PRESSURE) pressure else androidx.ink.strokes.StrokeInput.NO_PRESSURE,
                tiltRadians = if (tiltRadians != androidx.ink.strokes.StrokeInput.NO_TILT) tiltRadians else androidx.ink.strokes.StrokeInput.NO_TILT,
                orientationRadians = if (orientationRadians != androidx.ink.strokes.StrokeInput.NO_ORIENTATION) orientationRadians else androidx.ink.strokes.StrokeInput.NO_ORIENTATION
            )
        }
    } finally {
        dataInput.close()
    }
    return batch.asImmutable()
}