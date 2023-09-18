package com.studiomath.pencilnotes.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.transform
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.dpToPx
import com.studiomath.pencilnotes.file.DrawViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val TAG = "DrawView"

/**
 * TODO: document your custom view class.
 */
class DrawView(context: Context, val drawViewModel: DrawViewModel) : View(context) {

    var activeTool = DrawViewModel.Path.PathType.PENNA

    private var isStylusActive = true
    private var continueScaleTranslate = false

    private fun onTouchView(event: MotionEvent) {

        if (event.action == MotionEvent.ACTION_DOWN) continueScaleTranslate = false

        /**
         * gestione degli input provenienti da TOOL_TYPE_STYLUS
         */
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || (event.pointerCount == 1 && !isStylusActive && !continueScaleTranslate)) {
            var descriptorInputDevice = event.device.descriptor

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawViewModel.addPathData(
                        pathType = activeTool,
                        point = DrawViewModel.Path.Point(
                            event.x, event.y
                        ).apply {
                            pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE)
                            orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
                            tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
                        }
                    )
                }

                MotionEvent.ACTION_MOVE -> {
                    for (historyIndex in 1 until event.historySize) {
                        drawViewModel.addPathData(
                            isLastPath = true,
                            pathType = activeTool,
                            point = DrawViewModel.Path.Point(
                                event.getHistoricalX(historyIndex),
                                event.getHistoricalY(historyIndex)
                            ).apply {
                                pressure = event.getHistoricalAxisValue(
                                    MotionEvent.AXIS_PRESSURE,
                                    historyIndex
                                )
                                orientation = event.getHistoricalAxisValue(
                                    MotionEvent.AXIS_ORIENTATION,
                                    historyIndex
                                )
                                tilt = event.getHistoricalAxisValue(
                                    MotionEvent.AXIS_TILT,
                                    historyIndex
                                )
                            }
                        )
                    }

                    drawViewModel.addPathData(
                        isLastPath = true,
                        pathType = activeTool,
                        point = DrawViewModel.Path.Point(
                            event.x, event.y
                        ).apply {
                            pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE)
                            orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
                            tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
                        }
                    )


                }
            }

            return

        }

        /**
         * controllo il palmRejection
         */
        if (palmRejection(event)) {
            return
        }

        /**
         * eseguo lo scaling
         */
        if ((event.pointerCount == 1 || event.pointerCount == 2) && event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            /**
             * funzione che si occupa dello scale e dello spostamento
             */
            // TODO: 23/01/2022 sarebbe il caso di avviare lo scale solo
            //  dopo che sia stato rilevato un movimento significativo

            /**
             * Matrix()
             * https://i-rant.arnaudbos.com/matrices-for-developers/
             * https://i-rant.arnaudbos.com/2d-transformations-android-java/
             */
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    drawViewModel.down.pointers = mutableListOf(
                        PointF(
                            event.getX(drawViewModel.FIRST_POINTER_INDEX),
                            event.getY(drawViewModel.FIRST_POINTER_INDEX)
                        )
                    )

                    drawViewModel.startMatrix =
                        Matrix(drawViewModel.document.pages[drawViewModel.pageIndexNow].matrix)
                    drawLastPath = false

                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    drawViewModel.isScaling = true

                    drawViewModel.down.pointers = mutableListOf(
                        PointF(
                            event.getX(drawViewModel.FIRST_POINTER_INDEX),
                            event.getY(drawViewModel.FIRST_POINTER_INDEX)
                        ),
                        PointF(
                            event.getX(drawViewModel.SECOND_POINTER_INDEX),
                            event.getY(drawViewModel.SECOND_POINTER_INDEX)
                        )
                    )

                    drawViewModel.startMatrix =
                        Matrix(drawViewModel.document.pages[drawViewModel.pageIndexNow].matrix)

                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        if (drawViewModel.isScaling) {
                        }

                        drawViewModel.move.pointers = mutableListOf(
                            PointF(
                                event.getX(drawViewModel.FIRST_POINTER_INDEX),
                                event.getY(drawViewModel.FIRST_POINTER_INDEX)
                            )
                        )

                    } else if (event.pointerCount == 2) {
                        drawViewModel.move.pointers = mutableListOf(
                            PointF(
                                event.getX(drawViewModel.FIRST_POINTER_INDEX),
                                event.getY(drawViewModel.FIRST_POINTER_INDEX)
                            ),
                            PointF(
                                event.getX(drawViewModel.SECOND_POINTER_INDEX),
                                event.getY(drawViewModel.SECOND_POINTER_INDEX)
                            )
                        )

                    }


                    drawViewModel.translate = PointF(
                        drawViewModel.move.focusPos.x - drawViewModel.down.focusPos.x,
                        drawViewModel.move.focusPos.y - drawViewModel.down.focusPos.y
                    )
                    drawViewModel.scaleFactor =
                        (drawViewModel.move.distance / drawViewModel.down.distance)


                    drawViewModel.moveMatrix = Matrix(drawViewModel.startMatrix)

                    val f = FloatArray(9)
                    drawViewModel.moveMatrix.getValues(f)

                    /**
                     * scale max e scale min
                     */
                    val lastScaleFactor = f[Matrix.MSCALE_X]

                    val scaleMax = 5f
                    val scaleMin = 1f
                    if (lastScaleFactor * drawViewModel.scaleFactor < scaleMin) {
                        drawViewModel.scaleFactor = scaleMin / lastScaleFactor
                    }
                    if (lastScaleFactor * drawViewModel.scaleFactor > scaleMax) {
                        drawViewModel.scaleFactor = scaleMax / lastScaleFactor
                    }
                    drawViewModel.moveMatrix.postScale(
                        drawViewModel.scaleFactor,
                        drawViewModel.scaleFactor,
                        drawViewModel.down.focusPos.x,
                        drawViewModel.down.focusPos.y
                    )

                    /**
                     * translate max/min
                     */
                    val pageRectNow = calcPageRect(matrix = drawViewModel.moveMatrix)
                    val pageRectModel = calcPageRect(matrix = Matrix())

                    if (pageRectNow.left + drawViewModel.translate.x >= pageRectModel.left) {
                        drawViewModel.translate.x = pageRectModel.left - pageRectNow.left
                    }
                    if (pageRectNow.top + drawViewModel.translate.y >= pageRectModel.top) {
                        drawViewModel.translate.y = pageRectModel.top - pageRectNow.top
                    }
                    if (pageRectNow.right + drawViewModel.translate.x <= pageRectModel.right) {
                        drawViewModel.translate.x = pageRectModel.right - pageRectNow.right
                    }
                    if (pageRectNow.bottom + drawViewModel.translate.y <= pageRectModel.bottom) {
                        drawViewModel.translate.y = pageRectModel.bottom - pageRectNow.bottom
                    }

                    drawViewModel.moveMatrix.postTranslate(
                        drawViewModel.translate.x,
                        drawViewModel.translate.y
                    )

                    drawViewModel.document.pages[drawViewModel.pageIndexNow].matrix =
                        Matrix(drawViewModel.moveMatrix)
                    draw(scaling = true)

                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.actionIndex == 1) {
                        drawViewModel.down.pointers = mutableListOf(
                            PointF(
                                event.getX(drawViewModel.FIRST_POINTER_INDEX),
                                event.getY(drawViewModel.FIRST_POINTER_INDEX)
                            )
                        )
                    } else if (event.actionIndex == 0) {
                        drawViewModel.down.pointers = mutableListOf(
                            PointF(
                                event.getX(drawViewModel.SECOND_POINTER_INDEX),
                                event.getY(drawViewModel.SECOND_POINTER_INDEX)
                            )
                        )
                    }

                    drawViewModel.startMatrix =
                        Matrix(drawViewModel.document.pages[drawViewModel.pageIndexNow].matrix)
                    drawViewModel.isScaling = false

                }

                MotionEvent.ACTION_UP -> {
                    draw(redraw = true)

                }
            }

            if (!isStylusActive) {
                drawLastPath = false
            }
            continueScaleTranslate = true
        }
    }

    private fun onHoverView(event: MotionEvent) {
        draw(makeCursore = true)

//        Log.d(TAG, "onHoverView: ${event.action}")
//
//        when (event.action) {
//            MotionEvent.ACTION_HOVER_ENTER -> hoverStart(drawView, event)
//            MotionEvent.ACTION_HOVER_MOVE -> hoverMove(drawView, event)
//            MotionEvent.ACTION_HOVER_EXIT -> hoverUp(drawView, event)
//        }
    }


    /**
     * funzine che restituisce TRUE quando viene appoggiato sullo schermo il palmo della mano
     */
    // TODO: 23/01/2022 qui devo tener conto del fatto che, quando viene
    //  rilevato il palmo, alcune azioni come oo scale siano già iniziate.
    //  Per cui io dovrei ultimare quelle azioni
    private fun palmRejection(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            if (event.getToolMinor(i) / event.getToolMajor(i) < 0.5) {
                return true
            }
        }
        return false
    }


    init {
        setOnTouchListener { v, event ->
            v.performClick()
            onTouchView(event)

            return@setOnTouchListener true
        }

        setOnHoverListener { v, event ->
            onHoverView(event)

            return@setOnHoverListener true
        }
    }
//    enum class Pennello(val value: Int) {
//        PENNA(0),
//        GOMMA(1),
//        EVIDENZIATORE(2),
//        LAZO(3),
//        TESTO(4);
//
//        companion object {
//            fun fromInt(value: Int) = values().first { it.value == value }
//        }
//    }
//
//    var strumentoAttivo = Pennello.PENNA
//
//    var strumentoPenna: StrumentoPenna? = null
//    var strumentoEvidenziatore: StrumentoEvidenziatore? = null
//    var strumentoGomma: StrumentoGomma? = null
//    var strumentoLazo: StrumentoLazo? = null
//    var strumentoTesto: StrumentoTesto? = null
//
//
    /**
     * Funzioni per impostare il DrawView
     */

    var maxError = 0.3

    var paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        isFilterBitmap = true
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 3f // default: Hairline-width (really thin)
    }

//
//    lateinit var jobPrepareBitmapPage: Job
//    fun readFile(nomeFile: String, cartellaFile: String) {
//        this.nomeFile = nomeFile
//        this.cartellaFile = cartellaFile
////        drawFile = PencilFileXml(context, nomeFile, cartellaFile)
//        drawFile.readXML()
//
//        jobPrepareBitmapPage = scope.launch {
//            for (pagina in drawFile.body){
//                /**
//                 * aggiorno la cache
//                 */
//                pagina.bitmapPage = makePage(
//                    pagina.bitmapPage,
//                    null,
//                    pagina.index
//                )
//            }
//        }
//
//    }
//
//    var pageAttuale = 0
//    fun changePage(index: Int) {
//        if (index < 0) return
//        pageAttuale = drawFile.getPageIndex(index)
//
//        val activity = context as Activity
////        activity.findViewById<TextView>(R.id.contatoreTextView).text = "n.$pageAttuale"
//
//        drawLastPath = false
//        draw(changePage = true)
//    }
//
//
//    data class InfPath(var path: String, var paint: Paint, var rect: RectF)
//
//    var lastPath: InfPath = InfPath("", paint, RectF())
//
//
//    /**
//     * funzione per l'aggiunta delle risorse
//     */
//    fun addRisorsa(cartella: String, type: String): String {
//        var id = ""
//        if (drawFile.head.isEmpty()) {
//            id = "#0"
//        } else {
//            var idInt = 0
//            for (i in drawFile.head.keys) {
//                val idTemp = i.replace("#", "").toInt()
//                if (idTemp > idInt) {
//                    idInt = idTemp
//                }
//            }
//            idInt++
//            id = "#$idInt"
//        }
//
//        val path = "$cartella/$id$type"
//        drawFile.head[id] = mutableMapOf(Pair("path", path), Pair("type", type))
//
//        return id
//    }
//
//    fun addBackgroundPdf(id: String, indexPdf: Int, indexPage: Int) {
//        drawFile.preparePageIndex(indexPage)
//        drawFile.body[indexPage].background =
//            GestionePagina.Image(GestionePagina.Image.TypeImage.PDF).apply {
//                this.id = id
//                index = indexPdf
//            }
//
//        if (indexPage == pageAttuale) {
//            draw(redraw = true)
//        }
//        drawFile.writeXML()
//    }
//
//
    /**
     * funzioni il cui compito è quello di disegnare il contenuto della View
     */
    private var redrawOnDraw = false
    private var scalingOnDraw = false
    private var changePageOnDraw = false
    private var makeCursoreOnDraw = false
    private var dragAndDropOnDraw = false
    var drawLastPath = false

    var drawLastPathPaint = Paint(paint).apply {
        style = Paint.Style.STROKE
    }
    lateinit var scalingPageRect: RectF

    override fun onDraw(canvas: Canvas) {

        if (redrawOnDraw) {
            canvas.drawBitmap(onDrawBitmap, 0f, 0f, null)

        } else if (scalingOnDraw) {
            /**
             * make il colore di fondo della view
             */
            drawViewModel.makePageBackground(canvas, scalingPageRect)

            /**
             * make lo sfondo bianco della pagina
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di scegliere tra diversi tipi di pagine
            val paintSfondoPaginaBianco = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.white, null)
                style = Paint.Style.FILL
                setShadowLayer(
                    drawViewModel.document.pages[drawViewModel.pageIndexNow].dimension!!.calcPxFromPt(
                        24f,
                        scalingPageRect.width().toInt()
                    ),
                    0f,
                    8f,
                    ResourcesCompat.getColor(resources, R.color.shadow, null)
                )
            }
            canvas.drawRect(scalingPageRect, paintSfondoPaginaBianco)

            /**
             * trasformo e disegno la pagina intera memorizzata nella cache
             */
            canvas.drawBitmap(
                drawViewModel.document.pages[drawViewModel.pageIndexNow].bitmapPage!!,
                null,
                scalingPageRect,
                null
            )

            // TODO: non utilizzare onDrawBitmap ma una copia
            // trasformo e disegno l'area di disegno già pronta
            val startRect =
                RectF(drawViewModel.windowRect).apply { transform(drawViewModel.windowMatrix) }
            val endRect =
                RectF(drawViewModel.windowRect).apply { transform(drawViewModel.moveMatrix) }

            val windowMatrixTransform = Matrix().apply {
                setRectToRect(startRect, endRect, Matrix.ScaleToFit.CENTER)
            }
            canvas.drawBitmap(onDrawBitmap, windowMatrixTransform, null)

        } else if (changePageOnDraw) {
            scalingPageRect = calcPageRect()

            /**
             * make il colore di fondo della view
             */
            drawViewModel.makePageBackground(canvas, scalingPageRect)

            /**
             * make lo sfondo bianco della pagina
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di scegliere tra diversi tipi di pagine
            val paintSfondoPaginaBianco = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.white, null)
                style = Paint.Style.FILL
                setShadowLayer(
                    drawViewModel.document.pages[drawViewModel.pageIndexNow].dimension!!.calcPxFromPt(
                        24f,
                        scalingPageRect.width().toInt()
                    ),
                    0f,
                    8f,
                    ResourcesCompat.getColor(resources, R.color.shadow, null)
                )
            }
            canvas.drawRect(scalingPageRect, paintSfondoPaginaBianco)

            /**
             * trasformo e disegno la pagina intera memorizzata nella cache
             */
            canvas.drawBitmap(
                drawViewModel.document.pages[drawViewModel.pageIndexNow].bitmapPage!!,
                null,
                scalingPageRect,
                null
            )

        } else {
            canvas.drawBitmap(onDrawBitmap, 0f, 0f, null)

        }

        if (makeCursoreOnDraw) {
//            makeCursore(canvas)
        }

        if (dragAndDropOnDraw) {
            canvas.drawARGB(50, 255, 0, 0)
            dragAndDropOnDraw = false
        }

        /**
         * make lastPath
         */
//        val pageRect =
//            if (scalingOnDraw) scalingPageRect else if (::redrawPageRect.isInitialized) redrawPageRect else calcPageRect()
//
//        canvas.clipRect(pageRect)
//        if (drawLastPath) {
//            drawLastPathPaint.apply {
//                color = lastPath.paint.color
//
//                strokeWidth = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                    lastPath.paint.strokeWidth,
//                    pageRect.width().toInt()
//                )
//            }
//
//            canvas.drawPath(stringToPath(lastPath.path), drawLastPathPaint)
////            val errorCalc = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(0.01f, redrawPageRect.width().toInt())
////            canvas.drawPath(
////                stringToPath(pathFitCurve(lastPath.path, errorCalc)),
////                drawLastPathPaint
////            )
//        }
    }


    lateinit var onDrawBitmap: Bitmap
    lateinit var redrawPageRect: RectF

    lateinit var jobRedraw: Job
    var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
                onDrawBitmap = drawViewModel.makePage(
                    onDrawBitmap,
                    redrawPageRect
                )
                drawViewModel.windowMatrix =
                    Matrix(drawViewModel.document.pages[drawViewModel.pageIndexNow].matrix)

                redrawOnDraw = true
                scalingOnDraw = false
                changePageOnDraw = false
                makeCursoreOnDraw = false
                invalidate()

                /**
                 * aggiorno la cache
                 */
                drawViewModel.document.pages[drawViewModel.pageIndexNow].bitmapPage =
                    drawViewModel.makePage(
                        drawViewModel.document.pages[drawViewModel.pageIndexNow].bitmapPage!!,
                        null
                    )

            }
        } else if (scaling) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            scalingPageRect = calcPageRect()

            redrawOnDraw = false
            scalingOnDraw = true
            changePageOnDraw = false
            makeCursoreOnDraw = false
            invalidate()

        } else if (changePage) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            redrawOnDraw = false
            scalingOnDraw = false
            changePageOnDraw = true
            makeCursoreOnDraw = false
            invalidate()

            draw(redraw = true)

        } else if (makeCursore) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            redrawOnDraw = false
            scalingOnDraw = false
            changePageOnDraw = false
            makeCursoreOnDraw = true
            invalidate()

        } else if (dragAndDrop) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            redrawOnDraw = false
            scalingOnDraw = false
            changePageOnDraw = false
            makeCursoreOnDraw = false
            dragAndDropOnDraw = true
            invalidate()

        } else {
            redrawOnDraw = false
            scalingOnDraw = false
            makeCursoreOnDraw = false
            invalidate()
        }
    }

    /**
     * le funzioni seguenti avranno il prefisso calc-
     * e il loro scopo è quello di determinare alcune
     * caratteristiche della pagina
     */
    fun calcPageRect(
        matrix: Matrix = drawViewModel.document.pages[drawViewModel.pageIndexNow].matrix,
        paddingDp: Int = 8
    ): RectF {
        val padding = dpToPx(context, paddingDp).toFloat()

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

    /**
     * onSizeChanged
     */
    private var widthView: Int = 0
    private var heightView: Int = 0

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        widthView = width
        heightView = height

        if (::onDrawBitmap.isInitialized) onDrawBitmap.recycle()
        onDrawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        drawViewModel.windowRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
//        redrawPageRect = calcPageRect()

        draw(redraw = true, scaling = false)
    }
//
//    /**
//     * funzione che si occupa del cambio pagina
//     */
//    private var currentX = 0f
//    private var currentY = 0f
//    private var changePageEffettuato = false
//    fun scrollChangePagina(event: MotionEvent) {
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                currentX = event.x
//                currentY = event.y
//
//                changePageEffettuato = false
//            }
//            MotionEvent.ACTION_MOVE -> {
//                if (abs(event.y - currentY) < 30 && !changePageEffettuato) {
//                    if (event.x - currentX > 200) {
//                        changePage(pageAttuale - 1)
//                        changePageEffettuato = true
//                    } else if (event.x - currentX < -200) {
//                        changePage(pageAttuale + 1)
//                        changePageEffettuato = true
//                    }
//                }
//            }
//            MotionEvent.ACTION_UP -> {
//
//            }
//        }
//    }
//
//
//    /**
//     * gestione del Drag and Drop
//     */
////    override fun onDragEvent(event: DragEvent): Boolean {
////        when (event.action) {
////            DragEvent.ACTION_DRAG_STARTED -> {
////                // Determines if this View can accept the dragged data.
////
////                // Returns true to indicate that the View can accept the dragged data.
////                // Returns false to indicate that, during the current drag and drop operation,
////                // this View will not receive events again until ACTION_DRAG_ENDED is sent.
////                return event.clipDescription.hasMimeType("image/jpeg") || event.clipDescription.hasMimeType(
////                    "image/png"
////                )
////            }
////
////            DragEvent.ACTION_DRAG_ENTERED -> {
////                draw(dragAndDrop = true)
////
////                // Returns true; the value is ignored.
////                return true
////            }
////            DragEvent.ACTION_DRAG_LOCATION -> {
////                draw(dragAndDrop = true)
////
////                // Ignore the event.
////                return true
////            }
////            DragEvent.ACTION_DRAG_EXITED -> {
////                draw()
////
////                // Returns true; the value is ignored.
////                return true
////            }
////
////            DragEvent.ACTION_DROP -> {
////                val imageItem: ClipData.Item = event.clipData.getItemAt(0)
////                val uri = imageItem.uri
////
////                // Request permission to access the image data being dragged into
////                // the target activity's ImageView element.
////                val dropPermissions = requestDragAndDropPermissions(context as Activity, event)
////
////                var estensione =
////                    if (event.clipDescription.hasMimeType("image/jpeg")) "jpg" else
////                        if (event.clipDescription.hasMimeType("image/png")) "png" else ""
////                val id = addRisorsa(cartellaFile, ".$estensione")
////                val inputStream = context.contentResolver.openInputStream(uri)
////                val outputFile = FileManager(context, "$id.$estensione", cartellaFile)
////                val outputStream = outputFile.file.outputStream()
////
////                val buffer = ByteArray(1024)
////                var n = 0
////                if (inputStream != null) {
////                    while (inputStream.read(buffer)
////                            .also { n = it } != -1
////                    ) outputStream.write(buffer, 0, n)
////                }
////
////                inputStream?.close()
////                outputStream.close()
////
////                drawFile.body[pageAttuale].images.add(
////                    GestionePagina.Image(
////                        GestionePagina.Image.TypeImage.valueOf(estensione.uppercase())
////                    ).apply {
////                        this.id = id
////                        this.rectPage = redrawPageRect
////                        this.rectVisualizzazione = RectF().apply {
////                            left = event.x - 200
////                            top = event.y - 200
////                            right = event.x + 200
////                            bottom = event.y + 200
////                        }
////                    }
////                )
////
////                // Release the permission immediately afterwards because it's
////                // no longer needed.
////                dropPermissions!!.release()
////
////                draw(redraw = true)
////                drawFile.writeXML()
////
////                // Returns true. DragEvent.getResult() will return true.
////                return true
////            }
////
////            DragEvent.ACTION_DRAG_ENDED -> {
////                // Does a getResult(), and displays what happened.
////                when (event.result) {
////                    true ->
////                        Toast.makeText(context, "The drop was handled.", Toast.LENGTH_LONG)
////                    else ->
////                        Toast.makeText(context, "The drop didn't work.", Toast.LENGTH_LONG)
////                }.show()
////
////                draw()
////                // Returns true; the value is ignored.
////                return true
////            }
////            else -> {
////                // An unknown action type was received.
////                Log.e("DragDrop Example", "Unknown action type received by View.OnDragListener.")
////                return false
////            }
////        }
////    }
}