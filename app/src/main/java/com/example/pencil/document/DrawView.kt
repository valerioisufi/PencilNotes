package com.example.pencil.document

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.TextUtils.split
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.transform
import com.example.pencil.R
import com.example.pencil.document.page.Dimensioni
import com.example.pencil.document.page.GestionePagina
import com.example.pencil.document.path.DrawMotionEvent
import com.example.pencil.document.path.pathFitCurve
import com.example.pencil.document.path.stringToList
import com.example.pencil.document.path.stringToPath
import com.example.pencil.document.tool.*
import com.example.pencil.file.PencilFileXml
import java.io.File
import kotlin.math.log
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

    var maxError = 10
    private lateinit var pageRect: RectF
    private lateinit var windowRect: RectF
    private var path = Path()
    var paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 3f // default: Hairline-width (really thin)
    }

    fun setDrawMotioEvent(drawMotionEvent: DrawMotionEvent){
        setOnTouchListener { v, event ->
            v.performClick()
            drawMotionEvent.onTouchView(this, event)

            return@setOnTouchListener true
        }

        setOnHoverListener { v, event ->
            drawMotionEvent.onHoverView(this, event)

            return@setOnHoverListener true
        }
    }

    var strumentoAttivo = Pennello.PENNA

    var strumentoPenna: StrumentoPenna? = null
    var strumentoEvidenziatore: StrumentoEvidenziatore? = null
    var strumentoGomma: StrumentoGomma? = null
    var strumentoLazo: StrumentoLazo? = null
    var strumentoTesto: StrumentoTesto? = null

    var paginaAttuale = GestionePagina(context, this)





    var pathMatrix = Matrix()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (redraw) {
            //pageCanvas.clipRect(windowRect)
            drawPage(pageCanvas)
            drawPageBackground(pageBitmap)

            drawPagePaths(pageCanvas)
            //pageCanvas.clipRect(pageRect)
            canvas.drawBitmap(pageBitmap, 0f, 0f, null)

            redraw = false
            drawPageCache()
        } else if(scaling) {
            // disegno la pagina
            drawPage(scalingCanvas)

            // trasformo e disegno la pagina intera memorizzata nella cache
            canvas.drawBitmap(cachePageBitmap, null, pageRect, null)
            Log.d(TAG, "onDraw: " + cachePageBitmap.width + cachePageBitmap.height)

            // trasformo e disegno l'area di disegno già pronta
            var startRect = RectF(windowRect)
            var endRect = RectF(windowRect)
            startRect.transform(startMatrix)
            endRect.transform(moveMatrix)

            var windowMatrixTransform = Matrix()
            windowMatrixTransform.setRectToRect(startRect, endRect, Matrix.ScaleToFit.CENTER)

            //scalingCanvas.drawBitmap(pageBitmap, windowMatrixTransform, null)
            canvas.drawBitmap(pageBitmap, windowMatrixTransform, null)

            //drawPage(pageCanvas)
            //canvas.drawBitmap(scalingBitmap, 0f, 0f, null)
            scaling = false
        } else{
            canvas.drawBitmap(pageBitmap, 0f, 0f, null)

        }

        if (drawLastPath) {
            var paint = Paint(lastPath.paint)
            paint.strokeWidth = page.dimensioni.calcSpessore(lastPath.paint.strokeWidth, pageRect.width().toInt()).toFloat()

            canvas.drawPath(stringToPath(lastPath.path), paint)
            //Log.d(TAG, "onDraw: drawLastPath")
        }

        drawViewBackground(canvas)
        drawPageBorder(canvas)
    }


    data class InfPath(var path: String, var paint: Paint, var rect: RectF)

    lateinit var page: Page
    var nPage = 0
    //private var pathList = mutableListOf<InfPath>()
    private var lastPath: InfPath = InfPath("", paint, RectF())
    lateinit var drawFile: PencilFileXml

    var widthPagePredefinito = 210
    var heightPagePredefinito = 297
    var risoluzionePagePredefinito = 300

    data class Page(var dimensioni: Dimensioni){
        //var background : Bitmap? = null
        var pathPenna = mutableListOf<InfPath>()
        var pathEvidenziatore = mutableListOf<InfPath>()
    }

    fun readFile(nomeFile: String, cartellaFile: String){
        drawFile = PencilFileXml(context, nomeFile, cartellaFile)
        drawFile.readXML()
    }
    fun changePage(index: Int){
        nPage = index
        readPage(nPage)
    }
    fun readPage(index: Int = nPage) {
        if(drawFile.body.lastIndex < index){
            drawFile.newPage(index, widthPagePredefinito, heightPagePredefinito, risoluzionePagePredefinito)
        }
        val pageTemp = drawFile.getPage(index)
        val dimensioni = Dimensioni(widthPagePredefinito.toFloat(), heightPagePredefinito.toFloat(), risoluzionePagePredefinito.toFloat())
        page = Page(dimensioni)

        fun stringToPaint(paintS: String): Paint {
            val paintList = split(paintS, "#").toList()
            val paintTemp = Paint().apply {
                color = paintList[0].toInt()
                // Smooths out edges of what is drawn without affecting shape.
                isAntiAlias = true
                // Dithering affects how colors with higher-precision than the device are down-sampled.
                isDither = true
                style = when (paintList[1]) {
                    "STROKE" -> Paint.Style.STROKE
                    "FILL" -> Paint.Style.FILL
                    else -> Paint.Style.FILL_AND_STROKE
                }
                strokeJoin = Paint.Join.ROUND // default: MITER
                strokeCap = Paint.Cap.ROUND // default: BUTT
                strokeWidth = paintList[2].toFloat()
            }
            return paintTemp
        }
        fun stringToRect(rectS: String): RectF {
            val rectList = split(rectS, "#").toList()
            val rectTemp = RectF().apply {
                left = rectList[0].toFloat()
                top = rectList[1].toFloat()
                right = rectList[2].toFloat()
                bottom = rectList[3].toFloat()
            }
            return rectTemp
        }
        for(elemento in pageTemp.pathPenna){
            val pathS = elemento["path"]
            val paintS = elemento["style"]
            val rectS = elemento["rect"]

            val paintTemp = stringToPaint(paintS!!)
            val rectTemp = stringToRect(rectS!!)

            val infPath = InfPath(pathS!!, paintTemp, rectTemp)
            page.pathPenna.add(infPath)
        }
        for(elemento in pageTemp.pathEvidenziatore){
            val pathS = elemento["path"]
            val paintS = elemento["style"]
            val rectS = elemento["rect"]

            val paintTemp = stringToPaint(paintS!!)
            val rectTemp = stringToRect(rectS!!)

            val infPath = InfPath(pathS!!, paintTemp, rectTemp)
            page.pathEvidenziatore.add(infPath)
        }

        redraw = true
        invalidate()
    }

    fun writePage(index: Int = nPage) {
        fun paintToString(paint: Paint): String {
            val paintS =
                paint.color.toString() + "#" +
                        when (paint.style) {
                            Paint.Style.STROKE -> "STROKE"
                            Paint.Style.FILL -> "FILL"
                            Paint.Style.FILL_AND_STROKE -> "FILL_AND_STROKE"
                        } + "#" +
                        paint.strokeWidth.toString()
            return paintS
        }
        fun rectToString(rect: RectF): String {
            val rectS = rect.left.toString() + "#" + rect.top.toString() + "#" + rect.right.toString() + "#" + rect.bottom.toString()
            return rectS
        }

        val pageTemp = PencilFileXml.Page(widthPagePredefinito, heightPagePredefinito, risoluzionePagePredefinito)
        for(elemento in page.pathPenna){
            val elementoMap = mutableMapOf<String, String>()
            elementoMap["path"] = elemento.path
            elementoMap["style"] = paintToString(elemento.paint)
            elementoMap["rect"] = rectToString(elemento.rect)

            pageTemp.pathPenna.add(elementoMap)
        }
        for(elemento in page.pathEvidenziatore){
            val elementoMap = mutableMapOf<String, String>()
            elementoMap["path"] = elemento.path
            elementoMap["style"] = paintToString(elemento.paint)
            elementoMap["rect"] = rectToString(elemento.rect)

            pageTemp.pathEvidenziatore.add(elementoMap)
        }

        drawFile.body[index].pathPenna = pageTemp.pathPenna
        drawFile.body[index].pathEvidenziatore = pageTemp.pathEvidenziatore
    }

    fun newPath(path: String, paint: Paint/*, type: String = "Penna"*/) {
        lastPath = InfPath(path, paint, pageRect)
        drawLastPath = true

        Log.d(TAG, "newPath: newPath")
    }

    fun rewritePath(path: String) {
        lastPath.path = path

        invalidate()
    }

    fun savePath(path: String, paint: Paint, type: String = "Penna") {
        var errorCalc = page.dimensioni.calcSpessore(maxError.toFloat(), pageRect.width().toInt())
        lastPath.path = pathFitCurve(path, errorCalc)
        lastPath.paint = paint

        when(type){
            "Penna" -> page.pathPenna.add(lastPath)
            "Evidenziatore" -> page.pathEvidenziatore.add(lastPath)
        }

        drawLastPath = false
        var paint = Paint(lastPath.paint)
        paint.strokeWidth = page.dimensioni.calcSpessore(lastPath.paint.strokeWidth, pageRect.width().toInt()).toFloat()

        //var pathTemp = pathFitCurve(lastPath.path, maxError)
        pageCanvas.drawPath(stringToPath(lastPath.path), paint)
        invalidate()

        writePage(nPage)
        drawFile.writeXML()
    }

    /**
     * funzione per l'aggiunta delle risorse
     */
    fun addRisorsa(cartella: String, type: String): String {
        var id = ""
        if(drawFile.head.isEmpty()){
            id = "#0"
        }else{
            var idInt = 0
            for(i in drawFile.head.keys){
                val idTemp = i.replace("#", "").toInt()
                if (idTemp > idInt){
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
    fun addBackgroundPdf(id: String, indexPdf: Int, indexPage: Int){
        if (indexPage > drawFile.body.lastIndex){
            drawFile.newPage(indexPage, widthPagePredefinito, heightPagePredefinito, risoluzionePagePredefinito)
        }
        drawFile.body[indexPage].background = mutableMapOf(Pair("id", id), Pair("index", indexPdf.toString()))

        if (indexPage == nPage){
            redraw = true
            invalidate()
        }
        drawFile.writeXML()
    }



    fun setPathPaint(_path: Path, _paint: Paint) {
        path = _path
        paint = _paint
        invalidate()
    }

    fun savePathPaint(_path: Path, _paint: Paint) {
        pageCanvas.drawPath(_path, _paint)
        invalidate()
    }


    private fun drawPath(infPath: InfPath) {
        var pathMatrix = Matrix()
        pathMatrix.setRectToRect(infPath.rect, pageRect, Matrix.ScaleToFit.CENTER)
    }

    private fun drawPage(canvas: Canvas, rect: RectF = pageRect) {
        val paintPage = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.black, null)
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 3f // default: Hairline-width (really thin)
        }


        //canvas.drawColor(ResourcesCompat.getColor(resources, R.color.dark_elevation_00dp, null))

        paintPage.color = ResourcesCompat.getColor(resources, R.color.white, null)
        paintPage.style = Paint.Style.FILL

        // sfondo pagina
        canvas.drawRect(rect, paintPage)
        //canvas.clipRect(pageRect)
    }

    private fun drawPageBackground(bitmap: Bitmap, rectScaleToFit: RectF = pageRect){
        if(drawFile.body[nPage].background != null){
            var id = drawFile.body[nPage].background?.get("id")
            var indexPdf = drawFile.body[nPage].background!!["index"]?.toInt()!!

            var fileTemp = File(context.filesDir, drawFile.head[id]?.get("path"))
            val renderer = PdfRenderer(ParcelFileDescriptor.open(fileTemp, ParcelFileDescriptor.MODE_READ_ONLY))


            val pagePdf: PdfRenderer.Page = renderer.openPage(indexPdf)

            var renderRect = Rect()
            if (rectScaleToFit.left < 0f){
                renderRect.left = 0
            } else{
                renderRect.left = rectScaleToFit.left.toInt()
            }
            if (rectScaleToFit.top < 0f){
                renderRect.top = 0
            } else{
                renderRect.top = rectScaleToFit.top.toInt()
            }
            if (rectScaleToFit.right > bitmap.width){
                renderRect.right = bitmap.width
            } else{
                renderRect.right = rectScaleToFit.right.toInt()
            }
            if (rectScaleToFit.bottom > bitmap.height){
                renderRect.bottom = bitmap.height
            } else{
                renderRect.bottom = rectScaleToFit.bottom.toInt()
            }
            Log.d(TAG, "drawPageBackground: " + renderRect.toString())

            // If transform is not set, stretch page to whole clipped area
            val renderMatrix = Matrix()
            val clipWidth: Float = rectScaleToFit.width()
            val clipHeight: Float = rectScaleToFit.height()

            renderMatrix.postScale(
                clipWidth / pagePdf.width,
                clipHeight / pagePdf.height
            )
            renderMatrix.postTranslate(rectScaleToFit.left, rectScaleToFit.top)


            pagePdf.render(bitmap, renderRect, renderMatrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // close the page
            pagePdf.close()
        }
    }

    private fun drawPagePaths(canvas: Canvas, rectScaleToFit: RectF = pageRect){
        fun drawPathStructure(pathTemp: String, rectTemp: RectF){
            var paintPuntoControllo = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.bezier_punto_controlo, null)
                strokeWidth = 0.5f
                style = Paint.Style.STROKE

                isAntiAlias = true
                isDither = true
            }
            var paintLineacControllo = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.bezier_linea_controlo, null)
                strokeWidth = 3f
                style = Paint.Style.STROKE

                isAntiAlias = true
                isDither = true
            }
            var paintCurva = Paint().apply {
                color = ResourcesCompat.getColor(resources, R.color.bezier_curva, null)
                strokeWidth = 2f
                style = Paint.Style.STROKE

                isAntiAlias = true
                isDither = true
            }

            /**
             * pathMatrix è già stato modificato per corrispondere al tracciato corrente
             */
            var pathList = stringToList(pathTemp)
            var x = 0f
            var y = 0f

            for (pathSegmento in pathList){
                var pathToDraw = Path()

                when(pathSegmento["type"]){
                    "M" -> {
                        x = pathSegmento["x"]!!.toFloat()
                        y = pathSegmento["y"]!!.toFloat()

//                        pathToDraw.addCircle(x, y, 5f, Path.Direction.CW)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintPuntoControllo)
                    }
                    "L" -> {
                        pathToDraw.moveTo(x, y)
                        x = pathSegmento["x"]!!.toFloat()
                        y = pathSegmento["y"]!!.toFloat()
                        pathToDraw.lineTo(x, y)
                        pathToDraw.transform(pathMatrix)
                        canvas.drawPath(pathToDraw, paintCurva)

//                        pathToDraw.rewind()
//                        pathToDraw.addCircle(x, y, 5f, Path.Direction.CW)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintPuntoControllo)
                    }
                    "C" -> {
                        pathToDraw.moveTo(x, y)
                        var x1 = pathSegmento["x1"]!!.toFloat()
                        var y1 = pathSegmento["y1"]!!.toFloat()
                        pathToDraw.lineTo(x1, y1)
                        pathToDraw.transform(pathMatrix)
                        canvas.drawPath(pathToDraw, paintLineacControllo)

                        pathToDraw.rewind()
                        pathToDraw.addCircle(x1, y1, 2f, Path.Direction.CW)
                        pathToDraw.transform(pathMatrix)
                        canvas.drawPath(pathToDraw, paintPuntoControllo)

                        pathToDraw.reset()
                        pathToDraw.moveTo(x, y)
                        x = pathSegmento["x"]!!.toFloat()
                        y = pathSegmento["y"]!!.toFloat()
                        var x2 = pathSegmento["x2"]!!.toFloat()
                        var y2 = pathSegmento["y2"]!!.toFloat()
                        pathToDraw.cubicTo(x1, y1, x2, y2, x, y)
                        pathToDraw.transform(pathMatrix)
                        canvas.drawPath(pathToDraw, paintCurva)

//                        pathToDraw.rewind()
//                        pathToDraw.addCircle(x, y, 5f, Path.Direction.CW)
//                        pathToDraw.transform(pathMatrix)
//                        canvas.drawPath(pathToDraw, paintPuntoControllo)

                        pathToDraw.reset()
                        pathToDraw.moveTo(x, y)
                        pathToDraw.lineTo(x2, y2)
                        pathToDraw.transform(pathMatrix)
                        canvas.drawPath(pathToDraw, paintLineacControllo)

                        pathToDraw.reset()
                        pathToDraw.addCircle(x2, y2, 2f, Path.Direction.CW)
                        pathToDraw.transform(pathMatrix)
                        canvas.drawPath(pathToDraw, paintPuntoControllo)
                    }


                }
            }

        }


        fun drawPath(pathTemp: String, paintTemp: Paint, rectTemp: RectF){
            val path: Path = stringToPath(pathTemp)
            pathMatrix.setRectToRect(rectTemp, rectScaleToFit, Matrix.ScaleToFit.CENTER)
            path.transform(pathMatrix)

            val paint = Paint(paintTemp)
            paint.strokeWidth =
                page.dimensioni.calcSpessore(paintTemp.strokeWidth, rectScaleToFit.width().toInt()).toFloat()

            canvas.drawPath(path, paint)
            drawPathStructure(pathTemp, rectTemp)
        }

        for (i in page.pathEvidenziatore.indices) {
            drawPath(page.pathEvidenziatore[i].path, page.pathEvidenziatore[i].paint, page.pathEvidenziatore[i].rect)
        }
        for (i in page.pathPenna.indices) {
            drawPath(page.pathPenna[i].path, page.pathPenna[i].paint, page.pathPenna[i].rect)
        }
    }

    private fun drawPageBorder(canvas: Canvas){
        val paintPage = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.gn_border_page, null)
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 3f // default: Hairline-width (really thin)
        }

        // bordo pagina
        canvas.drawRect(pageRect, paintPage)
    }

    private fun drawViewBackground(canvas: Canvas) {
        var path1 = Path()
        path1.addRect(windowRect, Path.Direction.CW)
        var path2 = Path()
        path2.addRect(pageRect, Path.Direction.CW)

        var finalPath = Path()
        finalPath.op(path1, path2, Path.Op.DIFFERENCE)

        val paintViewBackground = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.gn_background_page, null)
        }
        canvas.drawPath(finalPath, paintViewBackground)
        //canvas.drawColor(ResourcesCompat.getColor(resources, R.color.dark_elevation_00dp, null))
    }

    private lateinit var cachePageCanvas: Canvas
    private lateinit var cachePageBitmap: Bitmap
    private fun drawPageCache(){
        if (::cachePageBitmap.isInitialized) cachePageBitmap.recycle()
        // TODO: 14/11/2021 Sistemare dimensione massima della bitmap
        var risoluzionePxInch = 300
        cachePageBitmap = Bitmap.createBitmap(
            page.dimensioni.calcWidthFromRisoluzionePxInch(risoluzionePxInch).toInt(),
            page.dimensioni.calcHeightFromRisoluzionePxInch(risoluzionePxInch).toInt(),
            Bitmap.Config.ARGB_8888)
        cachePageCanvas = Canvas(cachePageBitmap)

        var rectScaleToFit = RectF(0f, 0f, cachePageBitmap.width.toFloat(), cachePageBitmap.height.toFloat())
        drawPage(cachePageCanvas, rectScaleToFit)
        drawPageBackground(cachePageBitmap, rectScaleToFit)
        drawPagePaths(cachePageCanvas, rectScaleToFit)
    }

    private fun createPage() {
        val padding = 20

        val widthPage = widthView - padding * 2
        val heightPage = widthPage * sqrt(2.0)

        val left = padding.toFloat()
        var top = padding.toFloat()
        val right = (padding + widthPage).toFloat()
        var bottom = (padding + heightPage).toFloat()

        if (heightPage + padding * 2 < heightView) {
            top = ((heightView - heightPage) / 2).toFloat()
            bottom = (top + heightPage).toFloat()
        }

        pageRect = RectF(left, top, right, bottom)
        moveMatrix.mapRect(pageRect)
    }

    private var widthView: Int = 0
    private var heightView: Int = 0

    private lateinit var pageCanvas: Canvas
    private lateinit var pageBitmap: Bitmap

    private lateinit var scalingCanvas: Canvas
    private lateinit var scalingBitmap: Bitmap

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        widthView = width
        heightView = height

        if (::pageBitmap.isInitialized) pageBitmap.recycle()
        pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pageCanvas = Canvas(pageBitmap)

        // canvas e bitmap per lo scaling
        if (::scalingBitmap.isInitialized) scalingBitmap.recycle()
        scalingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        scalingCanvas = Canvas(scalingBitmap)
        // pageCanvas.drawColor(backgroundColor)

        windowRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        createPage()
    }


    private var redraw = true
    private var scaling = false

    //private var scaleCache = false
    private var drawLastPath = false
    //private var scaleFactorPaint = 1f

    /**
     * Funzione che si occupa dello scale e dello spostamento
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
                sStartPos =
                    PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))


                startDistance =
                    sqrt((sStartPos.x - fStartPos.x).pow(2) + (sStartPos.y - fStartPos.y).pow(2))
                startFocusPos =
                    PointF((fStartPos.x + sStartPos.x) / 2, (fStartPos.y + sStartPos.y) / 2)

                drawLastPath = false
            }
            MotionEvent.ACTION_MOVE -> {
                fMovePos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sMovePos =
                    PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

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


                Log.d("Scale factor: ", f[Matrix.MTRANS_X].toString())

                createPage()
                //scaleCache = true
                scaling = true
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastTranslate = PointF(translate.x, translate.y)
                //lastScaleFactor = scaleFactor

                startMatrix = Matrix(moveMatrix)

                redraw = true
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                moveMatrix = startMatrix

                val f = FloatArray(9)
                moveMatrix.getValues(f)
                lastScaleFactor = f[Matrix.MSCALE_X]

                createPage()
                scaling = true
                invalidate()
            }
        }
    }



    // funzione che riporta moveMatrix in una condizione normale
    fun scaleTranslateAnimation(startMatrix: Matrix, finalMatrix: Matrix){
        val durata = 1f

    }



}