package com.studiomath.pencilnotes.document

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
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
@SuppressLint("ViewConstructor")
class DrawView(context: Context, val drawViewModel: DrawViewModel) : View(context) {

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
//                drawViewModel.document.pages[drawViewModel.pageIndexNow].bitmapPage =
//                    drawViewModel.makePage(
//                        drawViewModel.document.pages[drawViewModel.pageIndexNow].bitmapPage!!,
//                        null
//                    )

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

        drawViewModel.onDrawBitmapChange = {
            // The ViewModel raises an event, do something here about it...
            invalidate()
        }

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
//
}