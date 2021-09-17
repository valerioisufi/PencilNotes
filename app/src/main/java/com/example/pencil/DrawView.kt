package com.example.pencil

import android.content.Context
import android.graphics.*
import android.text.TextUtils.split
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * TODO: document your custom view class.
 */
class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private lateinit var pageRect : RectF
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

        if (true){
            drawPage(canvas)

            for(i in pathList.indices){
                var path = readPath(pathList[i].path)
                pathMatrix.setRectToRect(pathList[i].rect, pageRect, Matrix.ScaleToFit.CENTER)

                path.transform(pathMatrix)
                canvas.drawPath(path, pathList[i].paint)
            }

            redraw = false
        }else{

        }
        //drawPage()
        //canvas.drawBitmap(pageBitmap, 0f, 0f, null)

        /*for (i in pathList) {
            drawPath(i)
        }*/
        //canvas.drawBitmap(pageBitmap, moveMatrix, null)
        if(drawLastPath) {
            canvas.drawPath(readPath(lastPath.path), lastPath.paint)
        }
    }


    data class InfPath(var path: String, var paint: Paint, var rect : RectF)//, var save : Boolean)
    private var pathList = mutableListOf<InfPath>()
    private var lastPath : InfPath = InfPath("", paint, RectF())

    fun newPath(path: String, paint: Paint){
        lastPath = InfPath(path, paint, pageRect)
        drawLastPath = true
    }
    fun rewritePath(path: String){
        lastPath.path = path

        invalidate()
    }
    fun savePath(path: String, paint: Paint){
        lastPath.path = path
        lastPath.paint = paint

        pathList.add(lastPath)

        drawLastPath = false
        redraw = true
        invalidate()
    }

    private fun readPath(path: String):Path{
        var realPath = Path()
        var stringPath = split(path, " ")

        var i = 0
        while(i < stringPath.size){
            when(stringPath[i]){
                "M" -> {
                    realPath.moveTo(stringPath[i+1].toFloat(),stringPath[i+2].toFloat())
                    i += 2
                }
                "Q" -> {
                    realPath.quadTo(stringPath[i+1].toFloat(),stringPath[i+2].toFloat(),stringPath[i+3].toFloat(),stringPath[i+4].toFloat())
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

    fun setPathPaint(_path: Path, _paint: Paint){
        path = _path
        paint = _paint
        invalidate()
    }

    fun savePathPaint(_path: Path, _paint: Paint) {
        pageCanvas.drawPath(_path, _paint)
        invalidate()
    }



    private fun drawPath(infPath : InfPath){
        var pathMatrix = Matrix()
        pathMatrix.setRectToRect(infPath.rect, pageRect, Matrix.ScaleToFit.CENTER)
        //_path.transform(pathMatrix)

        //pageCanvas.drawPath(infPath.path, infPath.paint)
    }

    private fun drawPage(canvas: Canvas){
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


        canvas.drawColor(ResourcesCompat.getColor(resources, R.color.dark_elevation_00dp, null))
        // bordo pagina
        canvas.drawRect(pageRect, paintPage)

        paintPage.color = ResourcesCompat.getColor(resources, R.color.white, null)
        paintPage.style = Paint.Style.FILL

        // sfondo pagina
        canvas.drawRect(pageRect, paintPage)
        //pageCanvas.clipRect(pageRect)
    }

    private fun createPage(){
        val padding = 20

        val widthPage = widthView - padding*2
        val heightPage = widthPage * sqrt(2.0)

        val left = padding.toFloat()
        val top = padding.toFloat()
        val right = (padding + widthPage).toFloat()
        val bottom = (padding + heightPage).toFloat()

        pageRect = RectF(left, top, right, bottom)
        moveMatrix.mapRect(pageRect)
    }

    private var widthView : Int = 0
    private var heightView : Int = 0

    private lateinit var pageCanvas: Canvas
    private lateinit var pageBitmap: Bitmap

    //private lateinit var pathCanvas: Canvas
    //private lateinit var pathBitmap: Bitmap

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        widthView = width
        heightView = height

        if (::pageBitmap.isInitialized) pageBitmap.recycle()
        pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pageCanvas = Canvas(pageBitmap)
        // pageCanvas.drawColor(backgroundColor)

        createPage()
    }





    // private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
    // private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    /*private fun applyMatrix(path: Path):Path{
        //path.transform(canvasMatrix)
        return path
    }*/

    private var redraw = true
    private var drawLastPath = false

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
    private var moveFocusPos = PointF()

    private var lastTranslate = PointF(0f, 0f)
    private var lastScaleFactor = 1f

    private var scaleFactor = 1f
    private var translate = PointF(0f, 0f)

    private var startFocusPos = PointF()

    fun scaleTranslate(event: MotionEvent){
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                fStartPos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sStartPos = PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))


                startDistance = sqrt((sStartPos.x - fStartPos.x).pow(2) + (sStartPos.y - fStartPos.y).pow(2))
                startFocusPos = PointF((fStartPos.x + sStartPos.x) / 2, (fStartPos.y + sStartPos.y) / 2)
            }
            MotionEvent.ACTION_MOVE -> {
                fMovePos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sMovePos = PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                moveDistance = sqrt((sMovePos.x - fMovePos.x).pow(2) + (sMovePos.y - fMovePos.y).pow(2))
                moveFocusPos = PointF((fMovePos.x + sMovePos.x) / 2, (fMovePos.y + sMovePos.y) / 2)

                translate = PointF(moveFocusPos.x - startFocusPos.x, moveFocusPos.y - startFocusPos.y)
                scaleFactor = (moveDistance/startDistance)


                moveMatrix.setTranslate(translate.x, translate.y)
                moveMatrix.postScale(scaleFactor, scaleFactor, moveFocusPos.x, moveFocusPos.y)
                moveMatrix.preConcat(startMatrix)

                /*val f = FloatArray(9)
                matrix.getValues(f)

                val scaleX = f[Matrix.MSCALE_X]
                val scaleY = f[Matrix.MSCALE_Y]

                if(scaleX * scaleFactor <= 1f){
                    scaleFactor = 1f / scaleX

                }*/
                //finalScaleFactor = lastScaleFactor * (moveDistance/startDistance)
                /*if(lastScaleFactor * scaleFactor <= 1f){
                    scaleFactor = 1f / lastScaleFactor

                }*/

                createPage()
                redraw = true
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastTranslate = PointF(translate.x, translate.y)
                lastScaleFactor = scaleFactor

                startMatrix = Matrix(moveMatrix)

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