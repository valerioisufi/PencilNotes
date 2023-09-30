package com.studiomath.pencilnotes.document

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.view.SurfaceView
import androidx.core.graphics.set
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.input.motionprediction.MotionEventPredictor
import com.studiomath.pencilnotes.document.stroke.StrokeRenderer
import com.studiomath.pencilnotes.file.DrawViewModel


class FastRenderer(
    private var drawViewModel: DrawViewModel
) : CanvasFrontBufferedRenderer.Callback<StrokeRenderer> {

    var frontBufferRenderer: CanvasFrontBufferedRenderer<StrokeRenderer>? = null

    private lateinit var pageRect: RectF

    private var paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3F51B5")
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = false
        isFilterBitmap = true
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 10f // default: Hairline-width (really thin)
    }

    private lateinit var onDrawFastRenderer: Bitmap

    override fun onDrawFrontBufferedLayer(
        canvas: Canvas,
        bufferWidth: Int,
        bufferHeight: Int,
        param: StrokeRenderer
    ) {
//        paint.apply {
//            color = lastPath.paint.color

//            strokeWidth = drawViewModel.pageNow.dimension!!.calcPxFromPt(
//                8f,
//                pageRect.width().toInt()
//            )
//        }

//        canvas.drawPath(stringToPath(lastPath.path), drawLastPathPaint)

//        val errorCalc = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(0.01f, redrawPageRect.width().toInt())
//        canvas.drawPath(
//            stringToPath(pathFitCurve(lastPath.path, errorCalc)),
//            drawLastPathPaint
//        )
//
//        val path = drawViewModel.computePath()
//        canvas.drawPath(path, paint)

        param.renderPoints(Canvas(onDrawFastRenderer), drawViewModel.paint)
        canvas.drawBitmap(onDrawFastRenderer, 0f, 0f, null)

//        param.renderPredictedPoint(canvas, drawViewModel.paint)


    }

    override fun onDrawMultiBufferedLayer(
        canvas: Canvas,
        bufferWidth: Int,
        bufferHeight: Int,
        params: Collection<StrokeRenderer>
    ) {
    }

    fun clear(){
        frontBufferRenderer!!.clear()
        Canvas(onDrawFastRenderer).apply {
            drawColor(Color.parseColor("#00FFFFFF"))
        }
    }

    fun attachSurfaceView(surfaceView: SurfaceView) {
        frontBufferRenderer = CanvasFrontBufferedRenderer(surfaceView, this)

    }

    fun release() {
        frontBufferRenderer?.release(true)

    }

    fun onSizeChanged(width: Int, height: Int) {

        if (::onDrawFastRenderer.isInitialized) onDrawFastRenderer.recycle()
        onDrawFastRenderer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}


@SuppressLint("ViewConstructor")
class LowLatencySurfaceView(context: Context, private val drawViewModel: DrawViewModel) :
    SurfaceView(context) {

    init {
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)

        setOnTouchListener(drawViewModel.onTouch.onTouchListener)
        setOnHoverListener(drawViewModel.onTouch.onHoverListener)

        drawViewModel.onTouch.motionEventPredictor = MotionEventPredictor.newInstance(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawViewModel.fastRenderer.attachSurfaceView(this)
    }

    override fun onDetachedFromWindow() {
        drawViewModel.fastRenderer.release()
        super.onDetachedFromWindow()
    }
}