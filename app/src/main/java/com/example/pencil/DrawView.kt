package com.example.pencil

import android.content.Context
import android.graphics.*
import android.text.TextUtils.split
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.transform
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "DrawView"

/**
 * TODO: document your custom view class.
 */
class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var pageRect: RectF
    private lateinit var windowRect: RectF
    private var path = Path()
    private var paint = Paint().apply {
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


    var pathMatrix = Matrix()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (redraw) {
            //pageCanvas.clipRect(windowRect)
            drawPage(pageCanvas)

            drawPagePaths(pageCanvas)
            //pageCanvas.clipRect(pageRect)
            canvas.drawBitmap(pageBitmap, 0f, 0f, null)

            redraw = false
            drawPageCache()
        } else if(scaling) {
            // disegno la pagina
            drawPage(scalingCanvas)

            // trasformo e disegno la pagina intera memorizzata nella cache
            scalingCanvas.drawBitmap(cachePageBitmap, null, pageRect, null)

            // trasformo e disegno l'area di disegno già pronta
            var startRect = RectF(windowRect)
            var endRect = RectF(windowRect)
            startRect.transform(startMatrix)
            endRect.transform(moveMatrix)

            var windowMatrixTransform = Matrix()
            windowMatrixTransform.setRectToRect(startRect, endRect, Matrix.ScaleToFit.CENTER)

            scalingCanvas.drawBitmap(pageBitmap, windowMatrixTransform, null)

            //drawPage(pageCanvas)
            canvas.drawBitmap(scalingBitmap, 0f, 0f, null)
            scaling = false
        } else{
            canvas.drawBitmap(pageBitmap, 0f, 0f, null)

        }

        //drawPage()
        //canvas.drawBitmap(pageBitmap, 0f, 0f, null)

        /*for (i in pathList) {
            drawPath(i)
        }*/

        /*if(scaleCache){
            scaleCache = false
        }*/


        //canvas.drawBitmap(pageBitmap, 0f, 0f, null)
        //drawPageBackground(canvas)

        if (drawLastPath) {
            var paint = Paint(lastPath.paint)
            paint.strokeWidth = lastPath.paint.strokeWidth * scaleFactorPaint

            canvas.drawPath(readPath(lastPath.path), paint)
            //Log.d(TAG, "onDraw: drawLastPath")
        }

        drawPageBackground(canvas)
        drawPageBorder(canvas)
    }


    data class InfPath(var path: String, var paint: Paint, var rect: RectF)//, var save : Boolean)

    private var pathList = mutableListOf<InfPath>()
    private var lastPath: InfPath = InfPath("", paint, RectF())
    lateinit var drawFile: FileManager

    fun readFile(nomeFile: String) {
        drawFile = FileManager(context, nomeFile)

        val textFile = drawFile.text
        val listLine = split(textFile, "\n").toList()
        for(line in listLine){
            var listTemp = split(line, ";").toList()

            var pathS = listTemp[0]
            var paintS = listTemp[1]
            var rectS = listTemp[2]

            var paintList = split(paintS, "#").toList()
            var rectList = split(rectS, "#").toList()

            var paintTemp = Paint().apply {
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
            var rectTemp = RectF().apply {
                left = rectList[0].toFloat()
                top = rectList[1].toFloat()
                right = rectList[2].toFloat()
                bottom = rectList[3].toFloat()
            }

            var infPath = InfPath(pathS, paintTemp, rectTemp)
            pathList.add(infPath)

            invalidate()
        }
    }

    fun writeFile(path: String, paint: Paint, rect: RectF) {
        var textFile = drawFile.text
        if (textFile != "") {
            textFile += "\n"
        }
        var pathS = path
        var paintS =
                paint.color.toString() + "#" +
                when (paint.style) {
                    Paint.Style.STROKE -> "STROKE"
                    Paint.Style.FILL -> "FILL"
                    Paint.Style.FILL_AND_STROKE -> "FILL_AND_STROKE"
                } + "#" +
                paint.strokeWidth.toString()
        var rectS = rect.left.toString() + "#" + rect.top.toString() + "#" + rect.right.toString() + "#" + rect.bottom.toString()
        textFile += "$pathS;$paintS;$rectS"

        drawFile.text = textFile
        drawFile.writeToFile()
    }

    fun newPath(path: String, paint: Paint) {
        lastPath = InfPath(path, paint, pageRect)
        drawLastPath = true

        Log.d(TAG, "newPath: newPath")
    }

    fun rewritePath(path: String) {
        lastPath.path = path

        invalidate()
    }

    fun savePath(path: String, paint: Paint) {
        lastPath.path = path
        lastPath.paint = paint

        pathList.add(lastPath)
        writeFile(lastPath.path, lastPath.paint, lastPath.rect)

        drawLastPath = false
        //redraw = true

        var paint = Paint(lastPath.paint)
        paint.strokeWidth = lastPath.paint.strokeWidth * scaleFactorPaint

        pageCanvas.drawPath(readPath(lastPath.path), paint)

        invalidate()
    }

    private fun readPath(path: String): Path {
        var realPath = Path()
        var stringPath = split(path, " ")

        var i = 0
        while (i < stringPath.size) {
            when (stringPath[i]) {
                "M" -> {
                    realPath.moveTo(stringPath[i + 1].toFloat(), stringPath[i + 2].toFloat())
                    i += 2
                }
                "Q" -> {
                    realPath.quadTo(
                        stringPath[i + 1].toFloat(),
                        stringPath[i + 2].toFloat(),
                        stringPath[i + 3].toFloat(),
                        stringPath[i + 4].toFloat()
                    )
                    i += 4
                }
            }

            i++
        }
        return realPath
    }

    /*fun getPaint(): Paint {
        return paint
    }

    fun setPaint(_paint: Paint) {
        paint = _paint
    }

    fun getPath(): Path {
        return path
    }*/

    /*fun setPath(_path: Path) {
        path = applyMatrix(_path)

        invalidate()
    }*/

    /*fun savePath(_path: Path) {
        pageCanvas.drawPath(applyMatrix(_path), paint)

        invalidate()
    }*/

    /*fun addPathPaint(_path: Path, _paint: Paint, save : Boolean = false) {

        val infPath = InfPath(_path, _paint, RectF(pageRect), save)
        if (save){
            val lastPathIndex = pathList.count() - 1
            pathList[lastPathIndex] = infPath
        }else{
            val lastPathIndex = pathList.count() - 1
            if(lastPathIndex < 0){
                pathList.add(infPath)
            } else if(pathList[lastPathIndex].save) {
                pathList.add(infPath)
            } else{
                pathList[lastPathIndex] = infPath
            }
        }

        invalidate()
    }*/

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
        //_path.transform(pathMatrix)

        //pageCanvas.drawPath(infPath.path, infPath.paint)
    }

    private fun drawPage(canvas: Canvas) {
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
        canvas.drawRect(pageRect, paintPage)
        //canvas.clipRect(pageRect)
    }

    private fun drawPagePaths(canvas: Canvas, rectScaleToFit: RectF = pageRect){
        for (i in pathList.indices) {
            var path = readPath(pathList[i].path)
            pathMatrix.setRectToRect(pathList[i].rect, rectScaleToFit, Matrix.ScaleToFit.CENTER)
            path.transform(pathMatrix)

            var paint = Paint(pathList[i].paint)
            paint.strokeWidth = pathList[i].paint.strokeWidth * scaleFactorPaint

            canvas.drawPath(path, paint)
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

    private fun drawPageBackground(canvas: Canvas) {
        var path1 = Path()
        path1.addRect(windowRect, Path.Direction.CW)
        var path2 = Path()
        path2.addRect(pageRect, Path.Direction.CW)

        var finalPath = Path()
        finalPath.op(path1, path2, Path.Op.DIFFERENCE)

        val paintPageBackground = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.gn_background_page, null)
        }
        canvas.drawPath(finalPath, paintPageBackground)
        //canvas.drawColor(ResourcesCompat.getColor(resources, R.color.dark_elevation_00dp, null))
    }

    private lateinit var cachePageCanvas: Canvas
    private lateinit var cachePageBitmap: Bitmap
    private fun drawPageCache(){
        if (::cachePageBitmap.isInitialized) cachePageBitmap.recycle()
        cachePageBitmap = Bitmap.createBitmap(pageRect.width().toInt(), pageRect.height().toInt(), Bitmap.Config.ARGB_8888)
        cachePageCanvas = Canvas(cachePageBitmap)

        var rectScaleToFit = RectF(0f, 0f, pageRect.width(), pageRect.height())
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

    //private lateinit var pathCanvas: Canvas
    //private lateinit var pathBitmap: Bitmap

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


    // private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    // private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    /*private fun applyMatrix(path: Path):Path{
        //path.transform(canvasMatrix)
        return path
    }*/

    private var redraw = true
    private var scaling = false

    //private var scaleCache = false
    private var drawLastPath = false
    private var scaleFactorPaint = 1f

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
                scaleFactorPaint = f[Matrix.MSCALE_X]

                if (scaleFactorPaint * scaleFactor < 0.5f) {
                    scaleFactor = 0.5f / scaleFactorPaint
                }
                if (scaleFactorPaint * scaleFactor > 5f) {
                    scaleFactor = 5f / scaleFactorPaint
                }
                moveMatrix.postScale(scaleFactor, scaleFactor, moveFocusPos.x, moveFocusPos.y)

                moveMatrix.getValues(f)
                scaleFactorPaint = f[Matrix.MSCALE_X]


                Log.d("Scale factor: ", f[Matrix.MTRANS_X].toString())

                createPage()
                //scaleCache = true
                scaling = true
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastTranslate = PointF(translate.x, translate.y)
                lastScaleFactor = scaleFactor

                startMatrix = Matrix(moveMatrix)

                redraw = true
                invalidate()

                /*for(infPath in pathList){
                    var path = readPath(infPath.path)
                    path.transform(moveMatrix)
                    pageCanvas.drawPath(path, infPath.paint)
                }
                invalidate()*/
            }
        }

    }
}