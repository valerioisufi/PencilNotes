package com.studiomath.pencilnotes.file

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.DisplayMetrics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.transform
import androidx.lifecycle.ViewModel
import com.studiomath.pencilnotes.document.FastRenderer
import com.studiomath.pencilnotes.document.page.Dimension
import com.studiomath.pencilnotes.document.page.mm
import com.studiomath.pencilnotes.document.page.risoluzionePxInchPagePredefinito
import com.studiomath.pencilnotes.document.touch.OnDrag
import com.studiomath.pencilnotes.document.touch.OnScaleTranslate
import com.studiomath.pencilnotes.document.touch.OnTouch
import com.studiomath.pencilnotes.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.sqrt
import android.graphics.Path as AndroidPath

class DrawViewModel(
    val filesDir: File,
    var filePath: String,
    var displayMetrics: DisplayMetrics
) : ViewModel() {

    /**
     * invalidate drawView when onDrawBitmap change
     */
    var onDrawBitmapChanged: (() -> Unit)? = null

    fun updateDrawView() {
        drawViewBitmap = Bitmap.createBitmap(onDrawBitmap)
        onDrawBitmapChanged?.let { it() } // Raise the event here; any subscriber will receive this.
    }

    /**
     * data class for document data
     */
    @Serializable
    data class Resource(val id: String, var type: ResourceType) {
        enum class ResourceType {
            PDF, IMAGE, COLOR
        }

        var content = ""
    }

    enum class DataType(val value: Int) {
        PATH(0), IMAGE(1), TEXT(2), PDF(3)
    }

    @Serializable
    data class Stroke(val zIndex: Int, var type: StrokeType) {
        enum class StrokeType {
            PENNA, EVIDENZIATORE
        }

        @Serializable
        data class Point(
            var x: Float = 0f, var y: Float = 0f
        ) {
            var size: Float = 8f

            var pressure: Float? = null
            var tilt: Float? = null
            var orientation: Float? = null
        }

        var points = mutableListOf<Point>()
        var color: Int = 0xFFFFFF
    }

    @Serializable
    data class Image(val zIndex: Int) {
        var id: String = ""
    }

    @Serializable
    data class Pdf(val zIndex: Int) {
        var id: String = ""
    }

    @Serializable
    data class Page(val index: Int) {
        //        var creationDate: LocalDate = LocalDate.now()
        var width = 0f // mm
        var height = 0f // mm

        @Transient
        var dimension: Dimension? = null

        @Transient
        var rect: RectF = RectF()

        @Transient
        var matrix: Matrix = Matrix()

        /**
         * bitmapPage e canvasPage servono solo come cache da
         * utlizzare per esempio durante lo scaling o lo scorrimento
         * tra le pagine
         */
        @Transient
        var bitmapPage: Bitmap? = null

        @Transient
        var canvasPage: Canvas? = null


        /**
         * grafica contenuta nella pagina
         */
        val strokeData = mutableListOf<Stroke>()
        val imageData = mutableListOf<Image>()
        val pdfData = mutableListOf<Pdf>()
    }

    @Serializable
    data class Document(val name: String) {
        val pages = mutableListOf<Page>()
        val resources = mutableListOf<Resource>() // key = resourceId
    }

    /**
     * gestione documento
     */
    fun preparePage(pageIndex: Int) {
        document.pages[pageIndex].apply {
            dimension = Dimension(width.mm, height.mm)

            bitmapPage = Bitmap.createBitmap(
                dimension!!.calcWidthFromRisoluzionePxInch(risoluzionePxInchPagePredefinito)
                    .toInt(),
                dimension!!.calcHeightFromRisoluzionePxInch(risoluzionePxInchPagePredefinito)
                    .toInt(),
                Bitmap.Config.ARGB_8888
            )
            canvasPage = Canvas(bitmapPage!!)
        }
    }


    private var fileManager: FileManager
    var document: Document

    fun saveDocument() {
        fileManager.text = Json.encodeToString(document)
    }

    var pageIndexNow by mutableIntStateOf(0)
    val pageNow: Page
        get() {
            return document.pages[pageIndexNow]
        }

    private var jobPrepareBitmapPage: Job
    var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        fileManager = FileManager(filesDir, filePath)
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

        preparePage(pageIndexNow)
        jobPrepareBitmapPage = scope.launch {
            for ((index, page) in document.pages.withIndex()) {
                preparePage(index)

                /**
                 * aggiorno la cache
                 */
                page.bitmapPage = makePage(
                    page.bitmapPage!!,
                    null,
                    page.index
                )
            }
        }
    }


    fun addPathData(
        isLastPath: Boolean = false,
        strokeType: Stroke.StrokeType = Stroke.StrokeType.PENNA,
        point: Stroke.Point
    ) {
        if (isLastPath) {
            document.pages[pageIndexNow].strokeData.last().points.add(point)
        } else {
            document.pages[pageIndexNow].strokeData.add(
                Stroke(
                    zIndex = 100,
                    type = strokeType
                ).apply {
                    points.add(point)
                }
            )
        }
    }


    fun computePath(pathIndex: Int = document.pages[pageIndexNow].strokeData.lastIndex): Path {
        val path: Path = Path().apply {
            for ((index, point) in document.pages[pageIndexNow].strokeData[pathIndex].points.withIndex()) {
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else
                    lineTo(point.x, point.y)
            }

        }
        return path


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


    /**
     * funzioni il cui compito è quello di disegnare il contenuto della View
     */

    var maxError = 0.3

    var paint = Paint().apply {
        color = Color.parseColor("#3F51B5")
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        isFilterBitmap = true
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 3f // default: Hairline-width (really thin)
    }


    var drawLastPathPaint = Paint(paint).apply {
        style = Paint.Style.STROKE
    }
    lateinit var scalingPageRect: RectF


    /**
     * drawViewBitmap = ciò che viene mostrato a schermo
     */
    lateinit var drawViewBitmap: Bitmap

    lateinit var onDrawBitmap: Bitmap
    lateinit var redrawPageRect: RectF


    lateinit var jobRedraw: Job
//    var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun draw(
        redraw: Boolean = false,
        scaling: Boolean = false,
        changePage: Boolean = false,
        makeCursore: Boolean = false,
        dragAndDrop: Boolean = false
    ) {
        if (!::onDrawBitmap.isInitialized) return

        if (redraw) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            jobRedraw = scope.launch {
                redrawPageRect = calcPageRect()

                /**
                 * disegno la pagina sulla Bitmap
                 */
                onDrawBitmap = makePage(
                    onDrawBitmap,
                    redrawPageRect
                )
                windowMatrix =
                    Matrix(document.pages[pageIndexNow].matrix)

                updateDrawView()

                /**
                 * aggiorno la cache
                 */
                document.pages[pageIndexNow].bitmapPage =
                    makePage(
                        document.pages[pageIndexNow].bitmapPage!!,
                        null
                    )

            }
        } else if (scaling) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            scalingPageRect = calcPageRect()
            val canvas = Canvas(onDrawBitmap)
            canvas.drawColor(Color.WHITE)

            /**
             * make il colore di fondo della view
             */
            makePageBackground(canvas, scalingPageRect)

            /**
             * make lo sfondo bianco della pagina
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di scegliere tra diversi tipi di pagine
            val paintSfondoPaginaBianco = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                setShadowLayer(
                    document.pages[pageIndexNow].dimension!!.calcPxFromPt(
                        24f,
                        scalingPageRect.width().toInt()
                    ),
                    0f,
                    8f,
                    Color.parseColor("#BF959DA5")
                )
            }
            canvas.drawRect(scalingPageRect, paintSfondoPaginaBianco)

            /**
             * trasformo e disegno la pagina intera memorizzata nella cache
             */
            canvas.drawBitmap(
                document.pages[pageIndexNow].bitmapPage!!,
                null,
                scalingPageRect,
                null
            )

            // TODO: non utilizzare onDrawBitmap ma una copia
            // trasformo e disegno l'area di disegno già pronta
            val startRect =
                RectF(windowRect).apply { transform(windowMatrix) }
            val endRect =
                RectF(windowRect).apply { transform(moveMatrix) }

            val windowMatrixTransform = Matrix().apply {
                setRectToRect(startRect, endRect, Matrix.ScaleToFit.CENTER)
            }
//            canvas.drawBitmap(drawViewBitmap, windowMatrixTransform, null)

            updateDrawView()

        } else if (changePage) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            scalingPageRect = calcPageRect()
            val canvas = Canvas(onDrawBitmap)

            /**
             * make il colore di fondo della view
             */
            makePageBackground(canvas, scalingPageRect)

            /**
             * make lo sfondo bianco della pagina
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di scegliere tra diversi tipi di pagine
            val paintSfondoPaginaBianco = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                setShadowLayer(
                    document.pages[pageIndexNow].dimension!!.calcPxFromPt(
                        24f,
                        scalingPageRect.width().toInt()
                    ),
                    0f,
                    8f,
                    Color.parseColor("#BF959DA5")
                )
            }
            canvas.drawRect(scalingPageRect, paintSfondoPaginaBianco)

            /**
             * trasformo e disegno la pagina intera memorizzata nella cache
             */
            canvas.drawBitmap(
                document.pages[pageIndexNow].bitmapPage!!,
                null,
                scalingPageRect,
                null
            )

            updateDrawView()

            draw(redraw = true)

//        } else if (makeCursore) {
//            if (::jobRedraw.isInitialized) jobRedraw.cancel()
//
//            redrawOnDraw = false
//            scalingOnDraw = false
//            changePageOnDraw = false
//            makeCursoreOnDraw = true
//            invalidate()
//
//        } else if (dragAndDrop) {
//            if (::jobRedraw.isInitialized) jobRedraw.cancel()
//
//            redrawOnDraw = false
//            scalingOnDraw = false
//            changePageOnDraw = false
//            makeCursoreOnDraw = false
//            dragAndDropOnDraw = true
//            invalidate()
//
//        } else {
//            redrawOnDraw = false
//            scalingOnDraw = false
//            makeCursoreOnDraw = false
//            invalidate()
        }
    }


    var activeTool = Stroke.StrokeType.PENNA


    lateinit var windowRect: RectF

    /**
     * le funzioni seguenti avranno il
     * prefisso make- semplicemente per distinguerle
     * dalle funzioni draw-
     */
    suspend fun makePage(
        bitmapSource: Bitmap,
        rect: RectF? = null,
        pageIndex: Int = pageIndexNow
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val bitmap = Bitmap.createBitmap(bitmapSource)
            val canvas = Canvas(bitmap)

            /**
             * verifico se il Rect passato come parametro alla funzione sia
             * uguale a null, in tal caso ne creo uno io con le dimensioni della Bitmap
             */
            var rectTemp = RectF()
            if (rect == null) {
                rectTemp.apply {
                    left = 0f
                    top = 0f
                    right = bitmap.width.toFloat()
                    bottom = bitmap.height.toFloat()
                }
            } else {
                rectTemp = rect

                /**
                 * make il colore di fondo della view, se serve
                 */
                makePageBackground(canvas, rectTemp)
            }
            val rect = rectTemp


            /**
             * make lo sfondo bianco della pagina e ShadowLayer
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di scegliere tra diversi tipi di pagine
            val paintSfondoPaginaBianco = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                setShadowLayer(
                    document.pages[pageIndex].dimension!!.calcPxFromPt(
                        24f,
                        rect.width().toInt()
                    ),
                    0f,
                    8f,
                    Color.parseColor("#BF959DA5")
                )
            }
            canvas.drawRect(rect, paintSfondoPaginaBianco)

            /**
             * make la rigatura o la quadrettatura
             */
//            val rigaturaQuadrettatura =
//                RigaturaQuadrettatura(context, RigaturaQuadrettatura.Type.Rigatura1R)
//            rigaturaQuadrettatura.makeRigaturaQuadrettatura(
//                canvas,
//                document.pages[pageIndex].dimension!!,
//                rect
//            )

//            /**
//             * make il PDF che farà da sfondo alla pagina
//             */
//            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di utilizzare un'immagine come sfondo
//            if (document.pages[pageIndex].background != null) {
//                val id = document.pages[pageIndex].background!!.id
//                val indexPdf = document.pages[pageIndex].background!!.index
//
//                val fileTemp = File(filesDir, document.resources[id]?.get("path")!!)
//                val renderer = PdfRenderer(
//                    ParcelFileDescriptor.open(
//                        fileTemp,
//                        ParcelFileDescriptor.MODE_READ_ONLY
//                    )
//                )
//                val pagePdf: PdfRenderer.Page = renderer.openPage(indexPdf)
//
//                val renderRect = Rect().apply {
//                    left = if (rect.left < 0f) 0 else rect.left.toInt()
//                    top = if (rect.top < 0f) 0 else rect.top.toInt()
//                    right = if (rect.right > bitmap.width) bitmap.width else rect.right.toInt()
//                    bottom = if (rect.bottom > bitmap.height) bitmap.height else rect.bottom.toInt()
//                }
//
//                // If transform is not set, stretch page to whole clipped area
//                val renderMatrix = Matrix()
//                val clipWidth: Float = rect.width()
//                val clipHeight: Float = rect.height()
//
//                var scaleX = clipWidth / pagePdf.width
//                var scaleY = clipHeight / pagePdf.height
//
//                var translateX = rect.left
//                var translateY = rect.top
//
//                if (scaleX < scaleY) {
//                    scaleY = scaleX
//
//                    var heightPage = scaleX * pagePdf.height
//                    translateY += (clipHeight - heightPage) / 2
//                } else {
//                    scaleX = scaleY
//
//                    var widthPage = scaleX * pagePdf.width
//                    translateX += (clipWidth - widthPage) / 2
//                }
//
//                renderMatrix.postScale(
//                    scaleX, scaleY
//                )
//
//                renderMatrix.postTranslate(translateX, translateY)
//                pagePdf.render(
//                    bitmap,
//                    renderRect,
//                    renderMatrix,
//                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
//                )
//
//                // close the page
//                pagePdf.close()
//            }

            /**
             * make il contenuto della pagina
             */
            canvas.clipRect(rect)

            /**
             * make images
             */
//            for (image in document.pages[pageIndex].imageData) {
//                if (image.bitmap == null) {
////                    val inputFile = FileManager(context, drawFile.head[image.id]?.get("path")!!)
////                    val inputStream = inputFile.file.inputStream()
////
////                    image.bitmap = BitmapFactory.decodeStream(inputStream)
//                }
//
//                val pageMatrix = Matrix().apply {
//                    setRectToRect(image.rectPage, rect, Matrix.ScaleToFit.CENTER)
//                }
//                val rectVisualizzazione = RectF(image.rectVisualizzazione).apply {
//                    transform(pageMatrix)
//                }
//                val imageRect =
//                    RectF(0f, 0f, image.bitmap!!.width.toFloat(), image.bitmap!!.height.toFloat())
//                val imageMatrix = Matrix().apply {
//                    setRectToRect(imageRect, rectVisualizzazione, Matrix.ScaleToFit.CENTER)
//                }
//
//                canvas.drawBitmap(image.bitmap!!, imageMatrix, null)
//            }

            /**
             * make tracciati
             */
            // TODO: 31/12/2021 poi valuterò l'idea di utlizzare una funzione a parte che richiama i metodi make- dei singoli strumenti
//            preparePage(pageIndex)
//            for (path in document.pages[pageIndex].strokeData) {
//                val pathTracciato: AndroidPath = stringToPath(tracciato.pathString)
//                val paintTracciato: Paint = Paint(tracciato.paintObject!!).apply {
//                    strokeWidth = drawFile.body[pageIndex].dimensioni.calcPxFromPt(
//                        strokeWidth,
//                        rect.width().toInt()
//                    )
//                }
//                val rectTracciato: RectF = tracciato.rectObject!!
//
//                val pathTracciatoMatrix = Matrix().apply {
//                    setRectToRect(rectTracciato, rect, Matrix.ScaleToFit.CENTER)
//                }
//                pathTracciato.transform(pathTracciatoMatrix)
//                canvas.drawPath(pathTracciato, paintTracciato)
//            }


            /**
             * make il bordo della pagina
             */
            // TODO: 24/01/2022 non necessario
//            val paintBordoPagina = Paint().apply {
//                color = ResourcesCompat.getColor(resources, R.color.gn_border_page, null)
//                style = Paint.Style.STROKE
//                strokeWidth = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                    (dpToPx(context, 1)).toFloat(),
//                    rect.width().toInt()
//                ).toFloat()
//            }
//            canvas.drawRect(rect, paintBordoPagina)

            return@withContext bitmap
        }

    fun makePageBackground(canvas: Canvas, pageRect: RectF) {
        val path1 = AndroidPath().apply {
            addRect(windowRect, AndroidPath.Direction.CW)
        }
        val path2 = AndroidPath().apply {
            addRect(pageRect, AndroidPath.Direction.CW)
        }

        val finalPath = AndroidPath().apply {
            op(path1, path2, AndroidPath.Op.DIFFERENCE)
        }

        val paintViewBackground = Paint().apply {
            color = Color.parseColor("#FFFFFF")
        }
        canvas.drawPath(finalPath, paintViewBackground)
        //canvas.drawColor(ResourcesCompat.getColor(resources, R.color.dark_elevation_00dp, null))

    }

    fun makeSingleTracciato(pathString: String, paintObject: Paint) {
//        val pathObject = stringToPath(pathString)
//
//        val onDrawCanvas = Canvas(onDrawBitmap)
//        onDrawCanvas.clipRect(redrawPageRect)
//        onDrawCanvas.drawPath(pathObject, Paint(paintObject).apply {
//            strokeWidth = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                paintObject.strokeWidth,
//                redrawPageRect.width().toInt()
//            )
//        })
//
//        val onScalingCanvas = Canvas(drawFile.body[pageAttuale].bitmapPage)
//        val dstRect = RectF().apply {
//            left = 0f
//            top = 0f
//            right = document.pages[pageAttuale].bitmapPage.width.toFloat()
//            bottom = document.pages[pageAttuale].bitmapPage.height.toFloat()
//        }
//        val pathTracciatoMatrix = Matrix().apply {
//            setRectToRect(redrawPageRect, dstRect, Matrix.ScaleToFit.CENTER)
//        }
//        pathObject.transform(pathTracciatoMatrix)
//        onScalingCanvas.drawPath(pathObject, Paint(paintObject).apply {
//            strokeWidth = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                paintObject.strokeWidth,
//                drawFile.body[pageAttuale].bitmapPage.width
//            )
//        })
    }


//    lateinit var mEvent: MotionEvent
//    var cursorePaint = Paint(paint).apply {
//        color = ResourcesCompat.getColor(resources, R.color.purple_200, null)
//        style = Paint.Style.STROKE
//        strokeWidth = 10f
//    }
//
//    fun makeCursore(canvas: Canvas) {
//        if (mEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
//            val pageRect =
//                if (scalingOnDraw) scalingPageRect else if (::redrawPageRect.isInitialized) redrawPageRect else calcPageRect()
//
//            if (mEvent.action == MotionEvent.ACTION_MOVE) {
//                canvas.drawPoint(mEvent.x, mEvent.y, cursorePaint.apply {
//                    color = ResourcesCompat.getColor(resources, R.color.purple_200, null)
//                })
//
//            } else if (mEvent.action == MotionEvent.ACTION_HOVER_MOVE) {
//                canvas.drawPoint(mEvent.x, mEvent.y, cursorePaint.apply {
//                    color = when (strumentoAttivo) {
//                        Pennello.PENNA -> strumentoPenna!!.colorStrumento
//                        else -> strumentoEvidenziatore!!.colorStrumento
//                    }
//                    strokeWidth = when (strumentoAttivo) {
//                        Pennello.PENNA -> drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                            strumentoPenna!!.strokeWidthStrumento,
//                            pageRect.width().toInt()
//                        )
//
//                        else -> drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                            strumentoEvidenziatore!!.strokeWidthStrumento,
//                            pageRect.width().toInt()
//                        )
//                    }
//                })
//
//            }
//
////            mEvent.recycle()
//
//        }
//    }

    /**
     * onSizeChanged
     */
    private var widthView: Int = 0
    private var heightView: Int = 0

    fun onSizeChanged(width: Int, height: Int) {

        widthView = width
        heightView = height

        if (::drawViewBitmap.isInitialized) drawViewBitmap.recycle()
        drawViewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (::onDrawBitmap.isInitialized) onDrawBitmap.recycle()
        onDrawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        windowRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
//        redrawPageRect = calcPageRect()


        draw(redraw = true, scaling = false)
    }

    /**
     * le funzioni seguenti avranno il prefisso calc-
     * e il loro scopo è quello di determinare alcune
     * caratteristiche della pagina
     */
    fun calcPageRect(
        matrix: Matrix = document.pages[pageIndexNow].matrix,
        paddingDp: Float = 8f
    ): RectF {
        val padding = dpToPx(paddingDp, displayMetrics).toFloat()

        var onWidth = true
        var widthPage = widthView - padding * 2
        var heightPage = (widthPage * sqrt(2.0)).toFloat()
        if (heightPage + padding * 2 > heightView) {
            onWidth = false
            heightPage = heightView - padding * 2
            widthPage = (heightPage / sqrt(2.0)).toFloat()
        }

        var left = padding
        var top = padding
        var right = (padding + widthPage)
        var bottom = (padding + heightPage)

        if (onWidth) {
            top = (heightView - heightPage) / 2
            bottom = top + heightPage
        } else {
            left = (widthView - widthPage) / 2
            right = left + widthPage
        }

        val rect = RectF(left, top, right, bottom)
        matrix.mapRect(rect)

        return rect
    }

    var windowMatrix = Matrix()
    var moveMatrix = Matrix()


    var fastRenderer: FastRenderer = FastRenderer(this)
    var onTouch = OnTouch(this)
    var onScaleTranslate = OnScaleTranslate(this)
    var onDrag = OnDrag(this)


}