package com.studiomath.pencilnotes.document.page

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.createBitmap
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import com.studiomath.pencilnotes.document.DrawViewModel
import com.studiomath.pencilnotes.file.DrawDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

class DrawDocumentRepository(
    context: Context,
    val documentId: Int,
    var drawViewModel: DrawViewModel
) {
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

        @Transient // Flag per tracciare le modifiche e salvare solo le pagine modificate
        var isModified = false

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

    data class Document(val name: String) {
        val pages = mutableListOf<DrawDocumentData.Page>()
        val resources = mutableListOf<DrawDocumentData.Resource>() // key = resourceId
    }

    private val db: DrawDatabase = DrawDatabase.getInstance(context)
    private val pageDao = db.pageDao()
    private val resourceDao = db.resourceDao()

    lateinit var document: DrawDocumentData.Document
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

//    var documentJob: Job
//    var documentScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


//    // MODIFIED: La logica di salvataggio ora aggiorna le pagine modificate nel DB
//    var saveDocument = debounce(scope = documentScope) {
//        documentJob = documentScope.launch {
//            if (!isDocumentLoaded) return@launch
//
//            val modifiedPages = document.pages.filter { it.isModified }
//            if (modifiedPages.isEmpty()) return@launch
//
//            documentMutex.withLock {
//                modifiedPages.forEach { page ->
//                    // Crea un oggetto PageContent con i dati da serializzare
//                    val pageContent = PageContent(
//                        strokeData = page.strokeData,
//                        imageData = page.imageData,
//                        pdfData = page.pdfData
//                    )
//                    val contentJson = Json.encodeToString(pageContent)
//
//                    // Aggiorna il contenuto della pagina nel DB
//                    pageDao.updatePageContent(page.dbId, contentJson)
//                    page.isModified = false // Resetta il flag
//                }
//            }
//        }
//    }
//
//    var isDocumentLoaded by mutableStateOf(false)
//    var isDocumentShowed by mutableStateOf(false)
//
//
//    // MODIFIED: L'inizializzazione ora carica i dati dal database
//    init {
//        documentJob = documentScope.launch {
//            // 1. Carica il documento principale
//            val dbDocument = documentDao.getDocumentById(documentId)
//                ?: throw IllegalStateException("Document with ID $documentId not found")
//
//            // 2. Carica le pagine e le risorse associate
//            val dbPages = pageDao.getPagesForDocument(documentId)
//            val dbResources = resourceDao.getResourcesForDocument(documentId)
//
//            // 3. Popola l'oggetto Document in memoria
//            document = Document(dbDocument.name).apply {
//                this.dbId = dbDocument.id
//
//                dbResources.forEach { dbResource ->
//                    // Qui dovresti mappare l'entità Resource del DB alla tua classe Resource interna
//                    // Esempio:
//                    // this.resources.add(Resource(...))
//                }
//
//                dbPages.forEach { dbPage ->
//                    val pageContent = Json.decodeFromString<PageContent>(dbPage.content)
//
//                    this.pages.add(Page(dbPage.pageNumber).apply {
//                        this.dbId = dbPage.id
//                        this.width = dbPage.width
//                        this.height = dbPage.height
//                        this.strokeData.addAll(pageContent.strokeData)
//                        this.imageData.addAll(pageContent.imageData)
//                        this.pdfData.addAll(pageContent.pdfData)
//                        prepare() // Prepara la pagina (crea bitmap, ecc.)
//                    })
//                }
//            }
//
//            isDocumentLoaded = true
//
//            // 4. Richiedi il ridisegno dell'UI
//            drawViewModel.drawManager.requestDraw(
//                DrawAttachments(DrawAttachments.DrawMode.UPDATE).apply {
//                    update = DrawAttachments.Update.DRAW_BITMAP
//                }
//            )
//            drawViewModel.drawManager.requestDraw(
//                DrawAttachments(DrawAttachments.DrawMode.UPDATE).apply {
//                    update = DrawAttachments.Update.CACHE_ALL
//                }
//            )
//        }
//    }
//
//    // MODIFIED: La funzione ora inserisce una nuova riga nella tabella 'pages'
//    fun addPage(page: Page) {
//        documentScope.launch {
//            val dbPage = com.studiomath.pencilnotes.file.Page(
//                documentId = document.dbId,
//                pageNumber = document.pages.size,
//                width = page.width,
//                height = page.height,
//                content = "{}" // Contenuto iniziale vuoto
//            )
//            val newPageId = pageDao.insert(dbPage)
//            page.dbId = newPageId.toInt()
//            page.prepare()
//
//            withContext(Dispatchers.Main) {
//                document.pages.add(page)
//                drawViewModel.drawManager.calcPage.needToBeUpdated = true
//                drawViewModel.drawManager.requestDraw(
//                    DrawAttachments(drawMode = DrawAttachments.DrawMode.UPDATE).apply {
//                        update = DrawAttachments.Update.DRAW_BITMAP
//                    }
//                )
//            }
//        }
//    }
//
//    // MODIFIED: La funzione ora rimuove la pagina dal DB
//    fun removePage(index: Int = document.pages.lastIndex) {
//        if (index >= 0 && index < document.pages.size) {
//            val pageToRemove = document.pages[index]
//            documentScope.launch {
//                pageDao.deleteById(pageToRemove.dbId) // Assumendo che esista un metodo deleteById in PageDao
//
//                withContext(Dispatchers.Main) {
//                    document.pages.removeAt(index)
//                    // Aggiorna i numeri di pagina successivi se necessario
//                    // ...
//
//                    drawViewModel.drawManager.calcPage.needToBeUpdated = true
//                    drawViewModel.drawManager.requestDraw(
//                        DrawAttachments(drawMode = DrawAttachments.DrawMode.UPDATE).apply {
//                            update = DrawAttachments.Update.DRAW_BITMAP
//                        }
//                    )
//                }
//            }
//        }
//    }


}