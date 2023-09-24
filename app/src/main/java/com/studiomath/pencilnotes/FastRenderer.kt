package com.studiomath.pencilnotes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.input.motionprediction.MotionEventPredictor
import com.studiomath.pencilnotes.file.DrawViewModel

class Segment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

class FastRenderer(
    private var drawViewModel: DrawViewModel
) : CanvasFrontBufferedRenderer.Callback<Segment> {

    private var frontBufferRenderer: CanvasFrontBufferedRenderer<Segment>? = null
    private var motionEventPredictor: MotionEventPredictor? = null

    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private var currentX: Float = 0f
    private var currentY: Float = 0f

    private lateinit var pageRect:RectF

    private var paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#3F51B5")
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        isFilterBitmap = true
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 10f // default: Hairline-width (really thin)
    }

    override fun onDrawFrontBufferedLayer(
        canvas: Canvas,
        bufferWidth: Int,
        bufferHeight: Int,
        param: Segment
    ) {
        paint.apply {
//            color = lastPath.paint.color

//            strokeWidth = drawViewModel.pageNow.dimension!!.calcPxFromPt(
//                8f,
//                pageRect.width().toInt()
//            )
        }

//        canvas.drawPath(stringToPath(lastPath.path), drawLastPathPaint)
        canvas.drawLine(param.x1, param.y1, param.x2, param.y2, paint)
//        val errorCalc = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(0.01f, redrawPageRect.width().toInt())
//        canvas.drawPath(
//            stringToPath(pathFitCurve(lastPath.path, errorCalc)),
//            drawLastPathPaint
//        )
    }

    override fun onDrawMultiBufferedLayer(
        canvas: Canvas,
        bufferWidth: Int,
        bufferHeight: Int,
        params: Collection<Segment>
    ) {
        for(param in params){
            canvas.drawLine(param.x1, param.y1, param.x2, param.y2, paint)
        }
    }

    fun attachSurfaceView(surfaceView: SurfaceView) {
        frontBufferRenderer = CanvasFrontBufferedRenderer(surfaceView, this)
        motionEventPredictor = MotionEventPredictor.newInstance(surfaceView)

    }

    fun release() {
        frontBufferRenderer?.release(true)

    }

    @SuppressLint("ClickableViewAccessibility")
    val onTouchListener = View.OnTouchListener { view, event ->

        motionEventPredictor?.record(event)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // Ask that the input system not batch MotionEvents
                // but instead deliver them as soon as they're available
                view.requestUnbufferedDispatch(event)

//                pageRect =
//                    if (scalingOnDraw) scalingPageRect else if (::redrawPageRect.isInitialized) redrawPageRect else calcPageRect()

                currentX = event.x
                currentY = event.y

                // Create single point
                val segment = Segment(currentX, currentY, currentX, currentY)

                frontBufferRenderer?.renderFrontBufferedLayer(segment)

            }

            MotionEvent.ACTION_MOVE -> {
                previousX = currentX
                previousY = currentY
                currentX = event.x
                currentY = event.y

                val segment = Segment(previousX, previousY, currentX, currentY)

                // Send the short line to front buffered layer: fast rendering
                frontBufferRenderer?.renderFrontBufferedLayer(segment)

                val motionEventPredicted = motionEventPredictor?.predict()
                if (motionEventPredicted != null) {
                    val predictedSegment = Segment(
                        currentX, currentY,
                        motionEventPredicted.x, motionEventPredicted.y
                    )
                    frontBufferRenderer?.renderFrontBufferedLayer(predictedSegment)
                }
            }

            MotionEvent.ACTION_UP -> {
                frontBufferRenderer?.commit()
            }
        }
        true
    }
}


@SuppressLint("ViewConstructor")
class LowLatencySurfaceView(context: Context, private val fastRenderer: FastRenderer) :
    SurfaceView(context) {

    init {
        setOnTouchListener(fastRenderer.onTouchListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        fastRenderer.attachSurfaceView(this)
    }

    override fun onDetachedFromWindow() {
        fastRenderer.release()
        super.onDetachedFromWindow()
    }
}