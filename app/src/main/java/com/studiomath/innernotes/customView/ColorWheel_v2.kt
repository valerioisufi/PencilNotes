package com.studiomath.innernotes.customView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.*


/**
 * TODO: document your custom view class.
 */
class ColorWheel_v2(context: Context, attrs: AttributeSet) : View(context, attrs) {

    /**
     * Utilities
     */
    private fun dpToPx(dipValue: Int): Int {
        val metrics = resources.displayMetrics
        val `val` = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dipValue.toFloat(), metrics
        )
        val res = (`val` + 0.5).toInt() // Round
        // Ensure at least 1 pixel if val was > 0
        return if (res == 0 && `val` > 0) 1 else res
    }

    /**
     * Definisco le variabili che si occupano di immagazzinare
     * il colore selezionato
     */
    private var alpha = 0xff
    private var hue = 360f
    private var sat = 0f
    private var `val` = 0f

    var color: Int
        // get colore in formato ARGB
        get() = Color.HSVToColor(alpha, floatArrayOf(hue, sat, `val`))
        // set colore in formato HSV a partire da un colore RGB
        set(color) {
            setColor(color, false)
        }

    private var onColorChangedListener: OnColorChangedListener? = null
    fun setColor(color: Int, callback: Boolean) {
        val alpha = Color.alpha(color)
        val red = Color.red(color)
        val blue = Color.blue(color)
        val green = Color.green(color)
        val hsv = FloatArray(3)
        Color.RGBToHSV(red, green, blue, hsv)
        this.alpha = alpha
        hue = hsv[0]
        sat = hsv[1]
        `val` = hsv[2]
        if (callback && onColorChangedListener != null) {
            onColorChangedListener!!.onColorChanged(
                Color.HSVToColor(
                    this.alpha,
                    floatArrayOf(hue, sat, `val`)
                )
            )
        }
        invalidate()
    }


    /**
     * Draw i riquadri di selezione del colore
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawHuePanel(canvas)
        drawSatValPanel(canvas)
//        drawAlphaPanel(canvas)
    }


    private var satValBackgroundBitmap: BitmapCache? = null
    private var hueBackgroundBitmap: BitmapCache? = null

    var paintBase = Paint().apply {
        isAntiAlias = true
    }
    var satValPaint = Paint(paintBase)
    var huePaint = Paint(paintBase)

    private var hueShader: Shader? = null
    private var valShader: Shader? = null
    private var satShader: Shader? = null

    private fun drawHuePanel(canvas: Canvas) {
        val rect = hueRect

        val centerX = (rect.width() / 2).toFloat()
        val centerY = (rect.height() / 2).toFloat()
        val raggio = (rect.width() / 2).toFloat()

        if (hueBackgroundBitmap == null) {
            hueBackgroundBitmap = BitmapCache()
            hueBackgroundBitmap!!.bitmap =
                Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
            hueBackgroundBitmap!!.canvas = Canvas(hueBackgroundBitmap!!.bitmap)

            val sweepShader: Shader = SweepGradient(
                centerX, centerY, intArrayOf(
                    Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN,
                    Color.GREEN, Color.YELLOW, Color.RED
                ), floatArrayOf(
                    0.000f, 0.166f, 0.333f, 0.499f,
                    0.666f, 0.833f, 0.999f
                )
            )
            huePaint.shader = sweepShader
            hueBackgroundBitmap!!.canvas.drawCircle(centerX, centerY, raggio, huePaint)


//            val satShader: Shader = RadialGradient(
//                centerX, centerY, raggio,
//                Color.WHITE, 0x00FFFFFF,
//                Shader.TileMode.CLAMP
//            )
//            satValPaint.shader = satShader
//            hueBackgroundBitmap!!.canvas.drawCircle(centerX, centerY, raggio, satValPaint)
        }

        canvas.drawBitmap(hueBackgroundBitmap!!.bitmap, null, rect, null)

        val paintWhite = Paint(paintBase).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(
            centro.x,
            centro.y,
            raggio * (1 - hueWidthRingPercentuale),
            paintWhite
        )

        // Tracker
        val p = hueToPoint(hue)
        var paintTracker = Paint(paintBase)
        paintTracker.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))

        canvas.drawCircle(p.x, p.y, (hueWidthRingPx / 2) - dpToPx(2), paintTracker)

        var paintTrackerBordo = Paint(paintBase).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(2).toFloat()
        }
        canvas.drawCircle(p.x, p.y, hueWidthRingPx / 2 - dpToPx(2), paintTrackerBordo)


    }

    private fun drawSatValPanel(canvas: Canvas) {
        val rect = satValRect

        val centerX = (rect.width() / 2).toFloat()
        val centerY = (rect.height() / 2).toFloat()
        val raggio = (rect.width() / 2).toFloat()

        if (satValBackgroundBitmap == null || satValBackgroundBitmap!!.value != hue) {
            if (satValBackgroundBitmap == null) {
                satValBackgroundBitmap = BitmapCache()
                satValBackgroundBitmap!!.bitmap =
                    Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
                satValBackgroundBitmap!!.canvas = Canvas(satValBackgroundBitmap!!.bitmap)
            }

            val rgb = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            val sweepShader: Shader = SweepGradient(
                centerX, centerY, intArrayOf(
                    rgb, Color.WHITE, Color.BLACK, rgb
                ), floatArrayOf(
                    0.000f, 0.333f, 0.666f, 0.999f
                )
            )
            satValPaint.shader = sweepShader
            satValBackgroundBitmap!!.canvas.drawCircle(centerX, centerY, raggio, satValPaint)

            //We set the hue value in our cache to which hue it was drawn with,
            //then we know that if it hasn't changed we can reuse our cached bitmap.
            satValBackgroundBitmap!!.value = hue
        }

        // We draw our bitmap from the cached, if the hue has changed
        // then it was just recreated otherwise the old one will be used.
        canvas.drawBitmap(satValBackgroundBitmap!!.bitmap, null, rect, null)

        val paintWhite = Paint(paintBase).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(
            centro.x,
            centro.y,
            raggio * (1 - hueWidthRingPercentuale),
            paintWhite
        )


        // Tracker
        val p = satValToPoint(sat, `val`)

        var paintTracker = Paint(paintBase)
        paintTracker.color = color

        canvas.drawCircle(p.x, p.y, dpToPx(8).toFloat(), paintTracker)

        var paintTrackerBordo = Paint(paintBase).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(2).toFloat()
        }
        canvas.drawCircle(p.x, p.y, dpToPx(8).toFloat(), paintTrackerBordo)

    }

    private fun hueToPoint(hue: Float): PointF {
        val rect = hueRect
        val centro = Point(rect.width() / 2 + rect.left, rect.height() / 2 + rect.top)
        var raggio = rect.width() / 2 - hueWidthRingPx / 2
        val angoloRad = hue * 3.14f / 180

        val p = PointF()
        p.x = (centro.x + cos(angoloRad) * raggio)
        p.y = (centro.y - sin(angoloRad) * raggio)
        return p
    }

    private fun satValToPoint(sat: Float, `val`: Float): PointF {
        /**
         * val e sat
         */
        val rect = satValRect
        val centro = Point(rect.width() / 2 + rect.left, rect.height() / 2 + rect.top)
        var raggio = rect.width() / 2 - satWidthRingPx / 2
        val angoloRad = hue * 3.14f / 180

        val p = PointF()
        p.x = (centro.x + cos(angoloRad) * raggio)
        p.y = (centro.y - sin(angoloRad) * raggio)
        return p
    }

    private fun pointToSatVal(point: Point): FloatArray {
        val result = FloatArray(2)

//        val deltaX = satPointA.x - satPointB.x
//        val deltaY = satPointC.y - satPointB.y
//
//        result[0] = if (point.x < satPointB.x) {
//            0f
//        } else if (point.x > satPointA.x) {
//            1f
//        } else {
//            (point.x - satPointB.x) / deltaX
//        }
//
//        result[1] = if (point.y < satPointB.y) {
//            1f
//        } else if (point.y > satPointC.y) {
//            0f
//        } else {
//            (satPointC.y - point.y) / deltaY
//        }

        return result
    }

    private fun pointToHue(point: Point): Float {

        val angoloRad: Float = atan2(point.y.toDouble(), point.x.toDouble()).toFloat()
        return if (angoloRad > 0) {
            angoloRad * 180 / 3.14f
        } else {
            angoloRad * 180 / 3.14f + 360f
        }

    }


    private var startTouchPoint: Point? = null
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var update = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startTouchPoint = Point(event.x.toInt(), event.y.toInt())
                update = moveTrackersIfNeeded(event)
            }
            MotionEvent.ACTION_MOVE -> update = moveTrackersIfNeeded(event)
            MotionEvent.ACTION_UP -> {
                startTouchPoint = null
                update = moveTrackersIfNeeded(event)
            }
        }
        if (update) {
            if (onColorChangedListener != null) {
                onColorChangedListener!!.onColorChanged(
                    Color.HSVToColor(
                        alpha,
                        floatArrayOf(hue, sat, `val`)
                    )
                )
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun moveTrackersIfNeeded(event: MotionEvent): Boolean {
        if (startTouchPoint == null) {
            return false
        }
        var update = false
        val pointEvento = PointF(event.x - centro.x, -(event.y - centro.y))

        val maxRaggioHue = (hueRect.width() / 2).toFloat()
        val minRaggioHue = maxRaggioHue - hueWidthRingPx
        var pointCentro = PointF(
            startTouchPoint!!.x.toFloat() - centro.x,
            -(startTouchPoint!!.y.toFloat() - centro.y)
        )

        if (pointCentro.x.pow(2) + pointCentro.y.pow(2) < maxRaggioHue.pow(2) && pointCentro.x.pow(2) + pointCentro.y.pow(
                2
            ) > minRaggioHue.pow(2)
        ) {
            hue = pointToHue(Point(pointEvento.x.toInt(), pointEvento.y.toInt()))
            update = true

        } else if (pointCentro.x.pow(2) + pointCentro.y.pow(2) < (satValRect.width() / 2).toFloat()
                .pow(2) && pointCentro.x.pow(2) + pointCentro.y.pow(2) > (satValRect.width() / 2 * (1 - hueWidthRingPercentuale)).toFloat()
                .pow(2)
        ) {
            val result = pointToSatVal(Point(pointEvento.x.toInt(), pointEvento.y.toInt()))
            sat = result[0]
            `val` = result[1]
            update = true

        }

        return update
    }


    /**
     * Imposto le dimensioni dei rettangoli di contenimento dei
     * selettori di colore
     */
    private lateinit var drawingRect: Rect
    private lateinit var hueRect: Rect
    private lateinit var satValRect: Rect
//    private lateinit var alphaRect: Rect

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingRect = Rect()
        drawingRect.left = paddingLeft
        drawingRect.right = w - paddingRight
        drawingRect.top = paddingTop
        drawingRect.bottom = h - paddingBottom

        //The need to be recreated because they depend on the size of the view.
        valShader = null
        satShader = null
//        alphaShader = null

        // Clear those bitmap caches since the size may have changed.
        //satValBackgroundCache = null
        //hueBackgroundCache = null

        centro = PointF(
            drawingRect.width() / 2f + drawingRect.left,
            drawingRect.height() / 2f + drawingRect.top
        )

        setUpHueRect()
        setUpSatValRect()
//        setUpAlphaRect()
    }


    var centro = PointF()

    var hueWidthRingPercentuale = 0.3f
    var hueWidthRingPx = 0f
    private fun setUpHueRect() {
        hueRect = Rect(drawingRect)
        hueWidthRingPx = hueRect.width() / 2 * hueWidthRingPercentuale

    }


    var satRaggio = 0f
    var satLato = 0f

    var satDistanceFromHue = dpToPx(8)

    var satPointA = PointF()
    var satPointB = PointF()
    var satPointC = PointF()

    var satWidthRingPx = 0f

    private fun setUpSatValRect() {
        satValRect = Rect().apply {
            left = hueRect.left + hueWidthRingPx.toInt() + satDistanceFromHue
            top = hueRect.top + hueWidthRingPx.toInt() + satDistanceFromHue
            right = hueRect.right - hueWidthRingPx.toInt() - satDistanceFromHue
            bottom = hueRect.bottom - hueWidthRingPx.toInt() - satDistanceFromHue
        }
        satRaggio = (satValRect.width() / 2).toFloat()
        satLato = satRaggio * sqrt(3f)

        satPointA = PointF(
            satValRect.width().toFloat(),
            satRaggio
        )
        satPointB = PointF(
            (satRaggio * 0.5).toFloat(),
            (satRaggio - satLato / 2)
        )
        satPointC = PointF(
            (satRaggio * 0.5).toFloat(),
            (satRaggio + satLato / 2)
        )

        satWidthRingPx = satValRect.width() / 2 * hueWidthRingPercentuale

    }

//    private fun setUpAlphaRect() {
//        val dRect = drawingRect
//        val left = dRect.left + BORDER_WIDTH
//        val top = dRect.bottom - ALPHA_PANEL_HEIGHT + BORDER_WIDTH
//        val bottom = dRect.bottom - BORDER_WIDTH
//        val right = dRect.right - BORDER_WIDTH - PANEL_SPACING - HUE_PANEL_WIDTH
//        alphaRect = Rect(left, top, right, bottom)
//
//
//        if (::alphaBackgroundBitmap.isInitialized) alphaBackgroundBitmap.recycle()
//        alphaBackgroundBitmap = Bitmap.createBitmap(alphaRect.width(), alphaRect.height(), Bitmap.Config.ARGB_8888)
//        val alphaBackgroundCanvas = Canvas(alphaBackgroundBitmap)
//
//        val numRectanglesHorizontal = ceil((alphaRect.width() / QUADRATO_TRASPARENZA_SIZE).toDouble()).toInt()
//        val numRectanglesVertical = ceil((alphaRect.height() / QUADRATO_TRASPARENZA_SIZE).toDouble()).toInt()
//
//        val r = Rect()
//        var verticalStartWhite = true
//        for (i in 0..numRectanglesVertical) {
//            var isWhite = verticalStartWhite
//            for (j in 0..numRectanglesHorizontal) {
//                r.top = i * QUADRATO_TRASPARENZA_SIZE
//                r.left = j * QUADRATO_TRASPARENZA_SIZE
//                r.bottom = r.top + QUADRATO_TRASPARENZA_SIZE
//                r.right = r.left + QUADRATO_TRASPARENZA_SIZE
//                alphaBackgroundCanvas.drawRect(r, if (isWhite) paintWhite else paintGray)
//                isWhite = !isWhite
//            }
//            verticalStartWhite = !verticalStartWhite
//        }
//    }

    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        onColorChangedListener = listener
    }


    interface OnColorChangedListener {
        fun onColorChanged(newColor: Int)
    }


    inner class BitmapCache {
        lateinit var canvas: Canvas
        lateinit var bitmap: Bitmap
        var value: Float = 0f
    }


}