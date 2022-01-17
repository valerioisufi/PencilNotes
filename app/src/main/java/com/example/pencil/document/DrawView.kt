package com.example.pencil.document

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.transform
import com.example.pencil.R
import com.example.pencil.document.page.GestionePagina
import com.example.pencil.document.path.DrawMotionEvent
import com.example.pencil.document.path.stringToPath
import com.example.pencil.document.tool.*
import com.example.pencil.dpToPx
import com.example.pencil.file.PencilFileXml
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "DrawView"

/**
 * TODO: document your custom view class.
 */
class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    enum class Pennello(val value: Int) {
        PENNA(0),
        GOMMA(1),
        EVIDENZIATORE(2),
        LAZO(3),
        TESTO(4);

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    var strumentoAttivo = Pennello.PENNA

    var strumentoPenna: StrumentoPenna? = null
    var strumentoEvidenziatore: StrumentoEvidenziatore? = null
    var strumentoGomma: StrumentoGomma? = null
    var strumentoLazo: StrumentoLazo? = null
    var strumentoTesto: StrumentoTesto? = null


    /**
     * Funzioni per impostare il DrawView
     */
    lateinit var drawMotion: DrawMotionEvent
    fun setDrawMotioEvent(drawMotionEvent: DrawMotionEvent) {
        drawMotion = drawMotionEvent

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
//        drawMotionEvent.mDetector = GestureDetectorCompat(context, drawMotion)
        // Set the gesture detector as the double tap
        // listener.
//        drawMotionEvent.mDetector.setOnDoubleTapListener(drawMotion)
//        drawMotionEvent.mScaleDetector = ScaleGestureDetector(context, drawMotionEvent.mScaleGestureListener)

        setOnTouchListener { v, event ->
            v.performClick()
            drawMotionEvent.onTouchView(event)

            return@setOnTouchListener true
        }

        setOnHoverListener { v, event ->
            drawMotionEvent.onHoverView(event)

            return@setOnHoverListener true
        }
    }


    var maxError = 2

    var paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 3f // default: Hairline-width (really thin)
    }


    lateinit var drawFile: PencilFileXml
    fun readFile(nomeFile: String, cartellaFile: String) {
        drawFile = PencilFileXml(context, nomeFile, cartellaFile)
        drawFile.readXML()
    }

    var pageAttuale = 0
    fun changePage(index: Int) {
        if (index < 0) return
        pageAttuale = drawFile.getPageIndex(index)

        drawLastPath = false
        draw(redraw = true, scaling = false)
    }


    data class InfPath(var path: String, var paint: Paint, var rect: RectF)

    var lastPath: InfPath = InfPath("", paint, RectF())


    /**
     * funzione per l'aggiunta delle risorse
     */
    fun addRisorsa(cartella: String, type: String): String {
        var id = ""
        if (drawFile.head.isEmpty()) {
            id = "#0"
        } else {
            var idInt = 0
            for (i in drawFile.head.keys) {
                val idTemp = i.replace("#", "").toInt()
                if (idTemp > idInt) {
                    idInt = idTemp
                }
            }
            idInt++
            id = "#$idInt"
        }

        val path = "$cartella/$id$type"
        drawFile.head[id] = mutableMapOf(Pair("path", path), Pair("type", type))

        return id
    }

    fun addBackgroundPdf(id: String, indexPdf: Int, indexPage: Int) {
        drawFile.preparePageIndex(indexPage)
        drawFile.body[indexPage].background =
            GestionePagina.Image(GestionePagina.Image.TypeImage.PDF).apply {
                this.id = id
                index = indexPdf
            }

        if (indexPage == pageAttuale) {
            draw(redraw = true, scaling = false)
        }
        drawFile.writeXML()
    }


    /**
     * funzioni il cui compito è quello di disegnare il contenuto della View
     */
    private var redrawOnDraw = true
    private var scalingOnDraw = false
    private var makeCursoreOnDraw = false
    var drawLastPath = false
    var drawTouchAnalyzer = false

    var drawLastPathPaint = Paint(paint).apply {
        style = Paint.Style.STROKE
    }
    lateinit var scalingPageRect: RectF

    override fun onDraw(canvas: Canvas) {

        if (redrawOnDraw) {
            canvas.drawBitmap(onDrawBitmap, 0f, 0f, null)
        } else if (scalingOnDraw) {
            // trasformo e disegno la pagina intera memorizzata nella cache
            canvas.drawBitmap(
                drawFile.body[pageAttuale].bitmapPage,
                null,
                scalingPageRect,
                null
            )

            // trasformo e disegno l'area di disegno già pronta
//            val startRect = RectF(windowRect).apply { transform(startMatrix) }
//            val endRect = RectF(windowRect).apply { transform(moveMatrix) }
//
//            val windowMatrixTransform = Matrix().apply {
//                setRectToRect(startRect, endRect, Matrix.ScaleToFit.CENTER)
//            }
//            canvas.drawBitmap(onDrawBitmap, windowMatrixTransform, null)

        } else {
            canvas.drawBitmap(onDrawBitmap, 0f, 0f, null)
        }

        if (makeCursoreOnDraw) {
            makeCursore(canvas)
        }


        val pageRect = if (scalingOnDraw) scalingPageRect else redrawPageRect
        if (drawLastPath) {
            drawLastPathPaint.apply {
                color = lastPath.paint.color

                strokeWidth = drawFile.body[pageAttuale].dimensioni.calcSpessore(
                    lastPath.paint.strokeWidth,
                    pageRect.width().toInt()
                ).toFloat()
            }

            canvas.drawPath(stringToPath(lastPath.path), drawLastPathPaint)
        }

        makePageBackground(canvas, pageRect)

//        if(drawTouchAnalyzer){
//            makeTouchAnalyzer(canvas)
//            makeInputDeviceAnalyzer(canvas)
//        }
    }


    lateinit var onDrawBitmap: Bitmap

    lateinit var redrawPageRect: RectF
    lateinit var windowRect: RectF

    lateinit var jobRedraw: Job
    var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun draw(redraw: Boolean = false, scaling: Boolean = false, makeCursore: Boolean = false) {
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

                redrawOnDraw = true
                scalingOnDraw = false
                makeCursoreOnDraw = false
                invalidate()

                /**
                 * aggiorno la cache
                 */
                drawFile.body[pageAttuale].bitmapPage = makePage(
                    drawFile.body[pageAttuale].bitmapPage,
                    null
                )
            }
        } else if (scaling) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            scalingPageRect = calcPageRect()

            redrawOnDraw = false
            scalingOnDraw = true
            makeCursoreOnDraw = false
            invalidate()
        } else if (makeCursore) {
            if (::jobRedraw.isInitialized) jobRedraw.cancel()

            redrawOnDraw = false
            scalingOnDraw = false
            makeCursoreOnDraw = true
            invalidate()
        } else {
            redrawOnDraw = false
            scalingOnDraw = false
            makeCursoreOnDraw = false
            invalidate()
        }
    }

    /**
     * le funzioni seguenti avranno il
     * prefisso make- semplicemente per distinguerle
     * dalle funzioni draw-
     */
    suspend fun makePage(bitmapSource: Bitmap, rect: RectF? = null): Bitmap =
        withContext(Dispatchers.Default) {
            Log.d(TAG, "redraw rect: $rect")
            Log.d(TAG, "redrawPageRect: $redrawPageRect")

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
            }
            val rect = rectTemp
            Log.d(TAG, "redraw rect 2: $rect")

            /**
             * make lo sfondo bianco della pagina
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di scegliere tra diversi tipi di pagine
            val paintSfondoPaginaBianco = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.white, null)
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, paintSfondoPaginaBianco)

            /**
             * make il PDF che farà da sfondo alla pagina
             */
            // TODO: 31/12/2021 in seguito implementerò anche la possibilità di utilizzare un'immagine come sfondo
            if (drawFile.body[pageAttuale].background != null) {
                val id = drawFile.body[pageAttuale].background!!.id
                val indexPdf = drawFile.body[pageAttuale].background!!.index

                val fileTemp = File(context.filesDir, drawFile.head[id]?.get("path")!!)
                val renderer = PdfRenderer(
                    ParcelFileDescriptor.open(
                        fileTemp,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                )
                val pagePdf: PdfRenderer.Page = renderer.openPage(indexPdf)

                val renderRect = Rect().apply {
                    left = if (rect.left < 0f) 0 else rect.left.toInt()
                    top = if (rect.top < 0f) 0 else rect.top.toInt()
                    right = if (rect.right > bitmap.width) bitmap.width else rect.right.toInt()
                    bottom = if (rect.bottom > bitmap.height) bitmap.height else rect.bottom.toInt()
                }

                // If transform is not set, stretch page to whole clipped area
                val renderMatrix = Matrix()
                val clipWidth: Float = rect.width()
                val clipHeight: Float = rect.height()

                renderMatrix.postScale(
                    clipWidth / pagePdf.width,
                    clipHeight / pagePdf.height
                )
                renderMatrix.postTranslate(rect.left, rect.top)
                pagePdf.render(
                    bitmap,
                    renderRect,
                    renderMatrix,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                // close the page
                pagePdf.close()
            }

            /**
             * make il contenuto della pagina
             */
            // TODO: 31/12/2021 poi valuterò l'idea di utlizzare una funzione a parte che richiama i metodi make- dei singoli strumenti
            for (tracciato in drawFile.body[pageAttuale].tracciati) {
                val pathTracciato: Path = stringToPath(tracciato.pathString)
                val paintTracciato: Paint = Paint(tracciato.paintObject!!).apply {
                    strokeWidth = drawFile.body[pageAttuale].dimensioni.calcSpessore(
                        strokeWidth,
                        rect.width().toInt()
                    ).toFloat()
                }
                val rectTracciato: RectF = tracciato.rectObject!!

                val pathTracciatoMatrix = Matrix().apply {
                    setRectToRect(rectTracciato, rect, Matrix.ScaleToFit.CENTER)
                }
                pathTracciato.transform(pathTracciatoMatrix)
                canvas.drawPath(pathTracciato, paintTracciato)
            }


            /**
             * make il bordo della pagina
             */
            val paintBordoPagina = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.gn_border_page, null)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRect(rect, paintBordoPagina)

            return@withContext bitmap
        }

    private fun makePageBackground(canvas: Canvas, pageRect: RectF) {
        val path1 = Path().apply {
            addRect(windowRect, Path.Direction.CW)
        }
        val path2 = Path().apply {
            addRect(pageRect, Path.Direction.CW)
        }

        val finalPath = Path().apply {
            op(path1, path2, Path.Op.DIFFERENCE)
        }

        val paintViewBackground = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.gn_background_page, null)
        }
        canvas.drawPath(finalPath, paintViewBackground)
        //canvas.drawColor(ResourcesCompat.getColor(resources, R.color.dark_elevation_00dp, null))

    }

    fun makeSingleTracciato(pathString: String, paintObject: Paint) {
        val pathObject = stringToPath(pathString)

        val onDrawCanvas = Canvas(onDrawBitmap)
        onDrawCanvas.drawPath(pathObject, Paint(paintObject).apply {
            strokeWidth = drawFile.body[pageAttuale].dimensioni.calcSpessore(
                paintObject.strokeWidth,
                redrawPageRect.width().toInt()
            ).toFloat()
        })

        val onScalingCanvas = Canvas(drawFile.body[pageAttuale].bitmapPage)
        val dstRect = RectF().apply {
            left = 0f
            top = 0f
            right = drawFile.body[pageAttuale].bitmapPage.width.toFloat()
            bottom = drawFile.body[pageAttuale].bitmapPage.height.toFloat()
        }
        val pathTracciatoMatrix = Matrix().apply {
            setRectToRect(redrawPageRect, dstRect, Matrix.ScaleToFit.CENTER)
        }
        pathObject.transform(pathTracciatoMatrix)
        onScalingCanvas.drawPath(pathObject, Paint(paintObject).apply {
            strokeWidth = drawFile.body[pageAttuale].dimensioni.calcSpessore(
                paintObject.strokeWidth,
                drawFile.body[pageAttuale].bitmapPage.width
            ).toFloat()
        })
    }


    lateinit var mEvent: MotionEvent
    var cursorePaint = Paint(paint).apply {
        color = ResourcesCompat.getColor(resources, R.color.purple_200, null)
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    fun makeCursore(canvas: Canvas) {
        if (mEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            if (mEvent.action == MotionEvent.ACTION_MOVE) {
                canvas.drawPoint(mEvent.x, mEvent.y, cursorePaint.apply {
                    color = ResourcesCompat.getColor(resources, R.color.purple_200, null)
                })

            } else if (mEvent.action == MotionEvent.ACTION_HOVER_MOVE) {
                canvas.drawPoint(mEvent.x, mEvent.y, cursorePaint.apply {
                    color = ResourcesCompat.getColor(resources, R.color.purple_500, null)
                })

            }

            mEvent.recycle()

        }
    }

    /**
     * funzioni di debug
     */
    fun makeTouchAnalyzer(canvas: Canvas) {
        var xPrecision = mEvent.xPrecision
        var yPrecision = mEvent.yPrecision

        var downTime = mEvent.downTime
        var eventTime = mEvent.eventTime

        var inputDevice = mEvent.device
        var inputSource = mEvent.source

        for (i in 0 until mEvent.pointerCount) {
            var toolType = mEvent.getToolType(i)
            var pointerId = mEvent.getPointerId(i)

            /**
             * toolMajor e touchMajor (così come toolMinor e touchMinor)
             * hanno lo stesso valore quando toolType = TOOL_TYPE_FINGER
             * e hanno valore nullo quando toolType = TOOL_TYPE_STYLUS
             */
            var toolMajor = mEvent.getToolMajor(i)
            var toolMinor = mEvent.getToolMinor(i)

            var touchMajor = mEvent.getTouchMajor(i)
            var touchMinor = mEvent.getTouchMinor(i)

            var size = mEvent.getSize(i)

            /**
             * Axis Values
             */
            var pressure = mEvent.getAxisValue(MotionEvent.AXIS_PRESSURE, i)
            var orientation = mEvent.getAxisValue(MotionEvent.AXIS_ORIENTATION, i)
            var tilt = mEvent.getAxisValue(MotionEvent.AXIS_TILT, i)
            var distance = mEvent.getAxisValue(
                MotionEvent.AXIS_DISTANCE,
                i
            ) // non funziona con M-Pencil di Huawei

            var x = mEvent.getX(i)
            var y = mEvent.getY(i)


            var toolRect = RectF(
                x - toolMinor / 2,
                y - toolMajor / 2,
                x + toolMinor / 2,
                y + toolMajor / 2
            )
            var toolPath = Path().apply {
                addOval(toolRect, Path.Direction.CW)
                transform(Matrix().apply {
                    setRotate((orientation * 180 / 3.14).toFloat(), x, y)
                })
            }

            canvas.drawPath(toolPath, Paint(paint).apply {
                color = ResourcesCompat.getColor(resources, R.color.light_blue_600, null)
                style = Paint.Style.STROKE
                strokeWidth = 10f
            })

            if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                var stylusPath = Path().apply {
                    addCircle(x, y, dpToPx(context, 30) * pressure, Path.Direction.CW)
                }

                canvas.drawPath(stylusPath, Paint(paint).apply {
                    color = ResourcesCompat.getColor(resources, R.color.purple_200, null)
                    style = Paint.Style.STROKE
                    strokeWidth = 10f
                })
            }
//            canvas.drawText(
//                "inputSource: $inputSource",
//                x + 150f, y,
//                Paint(paint).apply {
//                    textSize = 30f
//                    color = ResourcesCompat.getColor(resources, R.color.black, null)
//                }
//            )
            for (historyIndex in 1 until mEvent.historySize) {
                /**
                 * toolMajor e touchMajor (così come toolMinor e touchMinor)
                 * hanno lo stesso valore quando toolType = TOOL_TYPE_FINGER
                 * e hanno valore nullo quando toolType = TOOL_TYPE_STYLUS
                 */
                var toolMajorHistorical = mEvent.getHistoricalToolMajor(i, historyIndex)
                var toolMinorHistorical = mEvent.getHistoricalToolMinor(i, historyIndex)

                var touchMajorHistorical = mEvent.getHistoricalTouchMajor(i, historyIndex)
                var touchMinorHistorical = mEvent.getHistoricalTouchMinor(i, historyIndex)

                var sizeHistorical = mEvent.getHistoricalSize(i, historyIndex)

                /**
                 * Axis Values
                 */
                var pressureHistorical =
                    mEvent.getHistoricalAxisValue(MotionEvent.AXIS_PRESSURE, i, historyIndex)
                var orientationHistorical =
                    mEvent.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, i, historyIndex)
                var tiltHistorical =
                    mEvent.getHistoricalAxisValue(MotionEvent.AXIS_TILT, i, historyIndex)
                var distanceHistorical = mEvent.getHistoricalAxisValue(
                    MotionEvent.AXIS_DISTANCE,
                    i,
                    historyIndex
                ) // non funziona con M-Pencil di Huawei

                var xHistorical = mEvent.getHistoricalX(i, historyIndex)
                var yHistorical = mEvent.getHistoricalY(i, historyIndex)
            }

        }
    }

    fun makeInputDeviceAnalyzer(canvas: Canvas) {
        var deviceIds = InputDevice.getDeviceIds()

        for (deviceId in deviceIds) {
            var inputDevice = InputDevice.getDevice(deviceId)
            var descriptor = inputDevice.descriptor
            var motionRanges = inputDevice.motionRanges

        }
    }

    /**
     * le funzioni seguenti avranno il prefisso calc-
     * e il loro scopo è quello di determinare alcune
     * caratteristiche della pagina
     */
    fun calcPageRect(): RectF {
        val padding = 20f

        var widthPage = widthView - padding * 2
        var heightPage = (widthPage * sqrt(2.0)).toFloat()
        if (widthView > heightView) {
            widthPage = (widthPage * sqrt(2.0)).toFloat()
            heightPage = widthView - padding * 2
        }

        var left = padding
        var top = padding
        var right = (padding + widthPage)
        var bottom = (padding + heightPage)

        if (heightPage + padding * 2 < heightView) {
            top = (heightView - heightPage) / 2
            bottom = top + heightPage
        }

        val rect = RectF(left, top, right, bottom)
        moveMatrix.mapRect(rect)

        return rect
    }

    /**
     * classi per la memorizzazione temporanea degli inputEvent
     */
    var listTracciati = mutableListOf<Tracciato>()

    data class Tracciato(
        var toolType: Int = MotionEvent.TOOL_TYPE_STYLUS,
        var inputDeviceDescriptor: String
    ) {
        var downTime: Long? = null
        var listPoint = mutableListOf<Punto>()

        /**
         * style option
         */
        var color: Int = 0
        var strokeWidth: Float = 3f

    }

    data class Punto(var x: Float, var y: Float) {
        var eventTime: Long? = null

        var toolMajor: Float? = null
        var toolMinor: Float? = null

        var touchMajor: Float? = null
        var touchMinor: Float? = null

        var size: Float? = null

        var pressure: Float? = null
        var orientation: Float? = null
        var tilt: Float? = null
        var distance: Float? = null // non funziona con M-Pencil di Huawei
    }


//
//    /**
//     * Disegna i tracciati
//     */
//    private fun drawPagePaths(canvas: Canvas, rectScaleToFit: RectF = pageRect) {
//        fun drawPathStructure(pathTemp: String, rectTemp: RectF) {
//            var paintPuntoControllo = Paint().apply {
//                color = ResourcesCompat.getColor(resources, R.color.bezier_punto_controlo, null)
//                strokeWidth = drawFile.body[pageAttuale].dimensioni.calcSpessore(
//                    1f,
//                    rectScaleToFit.width().toInt()
//                ).toFloat()
//                style = Paint.Style.STROKE
//
//                isAntiAlias = true
//                isDither = true
//            }
//            var paintLineacControllo = Paint().apply {
//                color = ResourcesCompat.getColor(resources, R.color.bezier_linea_controlo, null)
//                strokeWidth = 3f
//                style = Paint.Style.STROKE
//
//                isAntiAlias = true
//                isDither = true
//            }
//            var paintCurva = Paint().apply {
//                color = ResourcesCompat.getColor(resources, R.color.bezier_curva, null)
//                strokeWidth = 2f
//                style = Paint.Style.STROKE
//
//                isAntiAlias = true
//                isDither = true
//            }
//
//            /**
//             * pathMatrix è già stato modificato per corrispondere al tracciato corrente
//             */
//            var pathList = stringToList(pathTemp)
//            var x = 0f
//            var y = 0f
//
//            for (pathSegmento in pathList) {
//                var pathToDraw = Path()
//
//                when (pathSegmento["type"]) {
//                    "M" -> {
//                        x = pathSegmento["x"]!!.toFloat()
//                        y = pathSegmento["y"]!!.toFloat()
//
////                        pathToDraw.addCircle(x, y, 5f, Path.Direction.CW)
////                        pathToDraw.transform(pathMatrix)
////                        canvas.drawPath(pathToDraw, paintPuntoControllo)
//                    }
//                    "L" -> {
//                        pathToDraw.moveTo(x, y)
//                        x = pathSegmento["x"]!!.toFloat()
//                        y = pathSegmento["y"]!!.toFloat()
//                        pathToDraw.lineTo(x, y)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintCurva)
//
////                        pathToDraw.rewind()
////                        pathToDraw.addCircle(x, y, 5f, Path.Direction.CW)
////                        pathToDraw.transform(pathMatrix)
////                        canvas.drawPath(pathToDraw, paintPuntoControllo)
//                    }
//                    "C" -> {
//                        pathToDraw.moveTo(x, y)
//                        var x1 = pathSegmento["x1"]!!.toFloat()
//                        var y1 = pathSegmento["y1"]!!.toFloat()
//                        pathToDraw.lineTo(x1, y1)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintLineacControllo)
//
//                        pathToDraw.rewind()
//                        pathToDraw.addCircle(x1, y1, 2f, Path.Direction.CW)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintPuntoControllo)
//
//                        pathToDraw.reset()
//                        pathToDraw.moveTo(x, y)
//                        x = pathSegmento["x"]!!.toFloat()
//                        y = pathSegmento["y"]!!.toFloat()
//                        var x2 = pathSegmento["x2"]!!.toFloat()
//                        var y2 = pathSegmento["y2"]!!.toFloat()
//                        pathToDraw.cubicTo(x1, y1, x2, y2, x, y)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintCurva)
//
////                        pathToDraw.rewind()
////                        pathToDraw.addCircle(x, y, 5f, Path.Direction.CW)
////                        pathToDraw.transform(pathMatrix)
////                        canvas.drawPath(pathToDraw, paintPuntoControllo)
//
//                        pathToDraw.reset()
//                        pathToDraw.moveTo(x, y)
//                        pathToDraw.lineTo(x2, y2)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintLineacControllo)
//
//                        pathToDraw.reset()
//                        pathToDraw.addCircle(x2, y2, 2f, Path.Direction.CW)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintPuntoControllo)
//                    }
//
//
//                }
//            }
//
//        }

    /**
     * onLayout
     */
    var exclusionRects = mutableListOf<Rect>()
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Update rect bounds and the exclusionRects list
        exclusionRects.add(Rect().apply {
            this.right = right
            this.left = left
            this.bottom = bottom
            this.top = bottom - dpToPx(context, 200)

        })
        systemGestureExclusionRects = exclusionRects
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

        windowRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        draw(redraw = true, scaling = false)
    }

    /**
     * funzione che si occupa del cambio pagina
     */
    private var currentX = 0f
    private var currentY = 0f
    private var changePageEffettuato = false
    fun scrollChangePagina(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentX = event.x
                currentY = event.y

                changePageEffettuato = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(event.y - currentY) < 30 && !changePageEffettuato) {
                    if (event.x - currentX > 200) {
                        changePage(pageAttuale - 1)
                        changePageEffettuato = true
                    } else if (event.x - currentX < -200) {
                        changePage(pageAttuale + 1)
                        changePageEffettuato = true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {

            }
        }
    }


    /**
     * funzione che si occupa dello scale e dello spostamento
     */
    private var startMatrix = Matrix()
    private var moveMatrix = Matrix()


    private val FIRST_POINTER_INDEX = 0
    private val SECOND_POINTER_INDEX = 1

    private var fStartPos = PointF()
    private var sStartPos = PointF()

    private var fMovePos = PointF()
    private var sMovePos = PointF()

    private var startDistance = 0f
    private var moveDistance = 0f

    private var lastTranslate = PointF(0f, 0f)
    private var lastScaleFactor = 1f

    private var scaleFactor = 1f
    private var translate = PointF(0f, 0f)

    private var startFocusPos = PointF()
    private var moveFocusPos = PointF()


    fun scaleTranslate(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                fStartPos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sStartPos = PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                startDistance =
                    sqrt((sStartPos.x - fStartPos.x).pow(2) + (sStartPos.y - fStartPos.y).pow(2))
                startFocusPos =
                    PointF((fStartPos.x + sStartPos.x) / 2, (fStartPos.y + sStartPos.y) / 2)

                drawLastPath = false
            }
            MotionEvent.ACTION_MOVE -> {
                fMovePos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sMovePos = PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                moveDistance =
                    sqrt((sMovePos.x - fMovePos.x).pow(2) + (sMovePos.y - fMovePos.y).pow(2))
                moveFocusPos = PointF((fMovePos.x + sMovePos.x) / 2, (fMovePos.y + sMovePos.y) / 2)

                translate =
                    PointF(moveFocusPos.x - startFocusPos.x, moveFocusPos.y - startFocusPos.y)
                scaleFactor = (moveDistance / startDistance)


                moveMatrix.setTranslate(translate.x, translate.y)
                moveMatrix.preConcat(startMatrix)

                val f = FloatArray(9)
                moveMatrix.getValues(f)
                lastScaleFactor = f[Matrix.MSCALE_X]

                // scale max e scale min
                val scaleMax = 5f
                val scaleMin = 1f
                if (lastScaleFactor * scaleFactor < scaleMin) {
                    scaleFactor = scaleMin / lastScaleFactor
                }
                if (lastScaleFactor * scaleFactor > scaleMax) {
                    scaleFactor = scaleMax / lastScaleFactor
                }
                moveMatrix.postScale(scaleFactor, scaleFactor, moveFocusPos.x, moveFocusPos.y)

                moveMatrix.getValues(f)
                lastScaleFactor = f[Matrix.MSCALE_X]


                Log.d("Scale factor: ", f[Matrix.MSCALE_X].toString())

                draw(scaling = true)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastTranslate = PointF(translate.x, translate.y)
                //lastScaleFactor = scaleFactor
                draw(redraw = true)
            }

        }
    }

    // funzione che riporta moveMatrix in una condizione normale
    fun scaleTranslateAnimation(startMatrix: Matrix, finalMatrix: Matrix) {
        val durata = 1f

    }


}