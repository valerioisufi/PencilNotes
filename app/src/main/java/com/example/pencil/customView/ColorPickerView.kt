package com.example.pencil.customView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil

/**
 * TODO: document your custom view class.
 */
class ColorPickerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    /**
     * Utilities
     */
    private fun dpToPx(dipValue: Int): Int {
        val metrics = resources.displayMetrics
        val `val` = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dipValue.toFloat(), metrics)
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

        drawSatValPanel(canvas)
        drawHuePanel(canvas)
        drawAlphaPanel(canvas)
    }

    private lateinit var drawingRect: Rect
    private lateinit var satValRect: Rect
    private lateinit var hueRect: Rect
    private lateinit var alphaRect: Rect

    private var valShader: Shader? = null
    private var satShader: Shader? = null
    private var alphaShader: Shader? = null

    /**
     * We cache a bitmap of the sat/val panel which is expensive to draw each time.
     * We can reuse it when the user is sliding the circle picker as long as the hue isn't changed.
     *
     * We cache the hue background to since its also very expensive now.
     */
    private var satValBackgroundBitmap: BitmapCache? = null
    private var hueBackgroundBitmap: BitmapCache? = null
    private lateinit var alphaBackgroundBitmap: Bitmap

    private var startTouchPoint: Point? = null
    private var onColorChangedListener: OnColorChangedListener? = null

    var satValPaint = Paint()
    var satValTrackerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2).toFloat()
        isAntiAlias = true
    }
    var hueAlphaTrackerPaint = Paint().apply {
        color = -0x424243
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2).toFloat()
        isAntiAlias = true
    }
    var alphaPaint = Paint()
    var borderPaint = Paint().apply {
        color = -0x919192
        style = Paint.Style.STROKE
        strokeWidth = BORDER_WIDTH.toFloat()
        isAntiAlias = true
    }

    val paintWhite = Paint().apply {
        color = -0x1
    }
    val paintGray = Paint().apply {
        color = -0x343435
    }



    private fun drawSatValPanel(canvas: Canvas) {
        val rect = satValRect
        drawRectPanel(rect, canvas)

        if (valShader == null) {
            //Black gradient has either not been created or the view has been resized.
            valShader = LinearGradient(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.left.toFloat(),
                rect.bottom.toFloat(),
                -0x1,
                -0x1000000,
                Shader.TileMode.CLAMP
            )
        }

        if (satValBackgroundBitmap == null || satValBackgroundBitmap!!.value != hue) {
            if (satValBackgroundBitmap == null) {
                satValBackgroundBitmap = BitmapCache()
                satValBackgroundBitmap!!.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
                satValBackgroundBitmap!!.canvas = Canvas(satValBackgroundBitmap!!.bitmap)
            }

            val rgb = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            satShader = LinearGradient(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.right.toFloat(),
                rect.top.toFloat(),
                -0x1,
                rgb,
                Shader.TileMode.CLAMP
            )
            val mShader = ComposeShader(valShader!!, satShader!!, PorterDuff.Mode.MULTIPLY)
            satValPaint.shader = mShader

            // Finally we draw on our canvas, the result will be
            // stored in our bitmap which is already in the cache.
            // Since this is drawn on a canvas not rendered on
            // screen it will automatically not be using the
            // hardware acceleration. And this was the code that
            // wasn't supported by hardware acceleration which mean
            // there is no need to turn it of anymore. The rest of
            // the view will still be hw accelerated.
            satValBackgroundBitmap!!.canvas.drawRect(0f, 0f, rect.width().toFloat(), rect.height().toFloat(), satValPaint)

            //We set the hue value in our cache to which hue it was drawn with,
            //then we know that if it hasn't changed we can reuse our cached bitmap.
            satValBackgroundBitmap!!.value = hue
        }

        // We draw our bitmap from the cached, if the hue has changed
        // then it was just recreated otherwise the old one will be used.
        canvas.drawBitmap(satValBackgroundBitmap!!.bitmap, null, rect, null)

        // Tracker
        val p = satValToPoint(sat, `val`)
        satValTrackerPaint.color = -0x1000000
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), (CIRCLE_TRACKER_RADIUS - dpToPx(1)).toFloat(), satValTrackerPaint)
        satValTrackerPaint.color = -0x222223
        canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), CIRCLE_TRACKER_RADIUS.toFloat(), satValTrackerPaint)

    }

    private fun drawHuePanel(canvas: Canvas) {
        val rect = hueRect
        drawRectPanel(rect, canvas)

        if (hueBackgroundBitmap == null) {
            hueBackgroundBitmap = BitmapCache()
            hueBackgroundBitmap!!.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
            hueBackgroundBitmap!!.canvas = Canvas(hueBackgroundBitmap!!.bitmap)


            val hueColors = IntArray((rect.height() + 0.5f).toInt())
            var h = 360f
            for (i in hueColors.indices) {
                hueColors[i] = Color.HSVToColor(floatArrayOf(h, 1f, 1f))
                h -= 360f / hueColors.size
            }

            // Time to draw the hue color gradient,
            // its drawn as individual lines which
            // will be quite many when the resolution is high
            // and/or the panel is large.
            val linePaint = Paint()
            linePaint.strokeWidth = 0f

            for (i in hueColors.indices) {
                linePaint.color = hueColors[i]
                hueBackgroundBitmap!!.canvas.drawLine(0f, i.toFloat(), rect.width().toFloat(), i.toFloat(), linePaint)
            }
        }
        canvas.drawBitmap(hueBackgroundBitmap!!.bitmap, null, rect, null)

        // Tracker
        val p = hueToPoint(hue)
        val r = RectF()
        r.left = (rect.left - SLIDER_TRACKER_OFFSET).toFloat()
        r.right = (rect.right + SLIDER_TRACKER_OFFSET).toFloat()
        r.top = (p.y - SLIDER_TRACKER_SIZE / 2).toFloat()
        r.bottom = (p.y + SLIDER_TRACKER_SIZE / 2).toFloat()
        canvas.drawRoundRect(r, 2f, 2f, hueAlphaTrackerPaint)


    }

    private fun drawAlphaPanel(canvas: Canvas) {
        val rect = alphaRect
        drawRectPanel(rect, canvas)


        val hsv = floatArrayOf(hue, sat, `val`)
        val color = Color.HSVToColor(hsv)
        val acolor = Color.HSVToColor(0, hsv)

        alphaShader = LinearGradient(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.top.toFloat(), color, acolor, Shader.TileMode.CLAMP)
        alphaPaint.shader = alphaShader

        canvas.drawBitmap(alphaBackgroundBitmap, null, rect, null)
        canvas.drawRect(rect, alphaPaint)

        // Tracker
        val p = alphaToPoint(alpha)
        val r = RectF()
        r.left = (p.x - SLIDER_TRACKER_SIZE / 2).toFloat()
        r.right = (p.x + SLIDER_TRACKER_SIZE / 2).toFloat()
        r.top = (rect.top - SLIDER_TRACKER_OFFSET).toFloat()
        r.bottom = (rect.bottom + SLIDER_TRACKER_OFFSET).toFloat()
        canvas.drawRoundRect(r, 2f, 2f, hueAlphaTrackerPaint)

    }

    private fun drawRectPanel(rect: Rect, canvas: Canvas){
        if (BORDER_WIDTH > 0) {
            canvas.drawRect(
                (rect.left - BORDER_WIDTH).toFloat(),
                (rect.top - BORDER_WIDTH).toFloat(),
                (rect.right + BORDER_WIDTH).toFloat(),
                (rect.bottom + BORDER_WIDTH).toFloat(),
                borderPaint
            )
        }
    }

    private fun hueToPoint(hue: Float): Point {
        val rect = hueRect
        val height = rect.height().toFloat()
        val p = Point()
        p.y = (height - hue * height / 360f + rect.top).toInt()
        p.x = rect.left
        return p
    }

    private fun satValToPoint(sat: Float, `val`: Float): Point {
        val rect = satValRect
        val height = rect.height().toFloat()
        val width = rect.width().toFloat()
        val p = Point()
        p.x = (sat * width + rect.left).toInt()
        p.y = ((1f - `val`) * height + rect.top).toInt()
        return p
    }

    private fun alphaToPoint(alpha: Int): Point {
        val rect = alphaRect
        val width = rect.width().toFloat()
        val p = Point()
        p.x = (width - alpha * width / 0xff + rect.left).toInt()
        p.y = rect.top
        return p
    }

    private fun pointToSatVal(x: Float, y: Float): FloatArray {
        var x = x
        var y = y
        val rect = satValRect
        val result = FloatArray(2)
        val width = rect.width().toFloat()
        val height = rect.height().toFloat()
        x = if (x < rect.left) {
            0f
        } else if (x > rect.right) {
            width
        } else {
            x - rect.left
        }
        y = if (y < rect.top) {
            0f
        } else if (y > rect.bottom) {
            height
        } else {
            y - rect.top
        }
        result[0] = 1f / width * x
        result[1] = 1f - 1f / height * y
        return result
    }

    private fun pointToHue(y: Float): Float {
        var y = y
        val rect = hueRect
        val height = rect.height().toFloat()
        y = if (y < rect.top) {
            0f
        } else if (y > rect.bottom) {
            height
        } else {
            y - rect.top
        }
        return 360f - y * 360f / height
    }

    private fun pointToAlpha(x: Int): Int {
        var x = x
        val rect = alphaRect
        val width = rect.width()
        x = if (x < rect.left) {
            0
        } else if (x > rect.right) {
            width
        } else {
            x - rect.left
        }
        return 0xff - x * 0xff / width
    }





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
        val startX = startTouchPoint!!.x
        val startY = startTouchPoint!!.y
        if (hueRect.contains(startX, startY)) {
            hue = pointToHue(event.y)
            update = true
        } else if (satValRect.contains(startX, startY)) {
            val result = pointToSatVal(event.x, event.y)
            sat = result[0]
            `val` = result[1]
            update = true
        } else if (alphaRect.contains(startX, startY)) {
            alpha = pointToAlpha(event.x.toInt())
            update = true
        }
        return update
    }




    /**
     * Imposto le dimensioni dei rettangoli di contenimento dei
     * selettori di colore
     */
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
        alphaShader = null

        // Clear those bitmap caches since the size may have changed.
        //satValBackgroundCache = null
        //hueBackgroundCache = null

        setUpSatValRect()
        setUpHueRect()
        setUpAlphaRect()
    }

    private fun setUpSatValRect() {
        //Calculate the size for the big color rectangle.
        val dRect = drawingRect
        val left = dRect.left + BORDER_WIDTH
        val top = dRect.top + BORDER_WIDTH
        val bottom = dRect.bottom - BORDER_WIDTH - ALPHA_PANEL_HEIGHT - PANEL_SPACING
        val right = dRect.right - BORDER_WIDTH - PANEL_SPACING - HUE_PANEL_WIDTH
        satValRect = Rect(left, top, right, bottom)
    }

    private fun setUpHueRect() {
        //Calculate the size for the hue slider on the left.
        val dRect = drawingRect
        val left = dRect.right - HUE_PANEL_WIDTH + BORDER_WIDTH
        val top = dRect.top + BORDER_WIDTH
        val bottom = dRect.bottom - BORDER_WIDTH
        val right = dRect.right - BORDER_WIDTH
        hueRect = Rect(left, top, right, bottom)
    }

    private fun setUpAlphaRect() {
        val dRect = drawingRect
        val left = dRect.left + BORDER_WIDTH
        val top = dRect.bottom - ALPHA_PANEL_HEIGHT + BORDER_WIDTH
        val bottom = dRect.bottom - BORDER_WIDTH
        val right = dRect.right - BORDER_WIDTH - PANEL_SPACING - HUE_PANEL_WIDTH
        alphaRect = Rect(left, top, right, bottom)


        if (::alphaBackgroundBitmap.isInitialized) alphaBackgroundBitmap.recycle()
        alphaBackgroundBitmap = Bitmap.createBitmap(alphaRect.width(), alphaRect.height(), Bitmap.Config.ARGB_8888)
        val alphaBackgroundCanvas = Canvas(alphaBackgroundBitmap)

        val numRectanglesHorizontal = ceil((alphaRect.width() / QUADRATO_TRASPARENZA_SIZE).toDouble()).toInt()
        val numRectanglesVertical = ceil((alphaRect.height() / QUADRATO_TRASPARENZA_SIZE).toDouble()).toInt()

        val r = Rect()
        var verticalStartWhite = true
        for (i in 0..numRectanglesVertical) {
            var isWhite = verticalStartWhite
            for (j in 0..numRectanglesHorizontal) {
                r.top = i * QUADRATO_TRASPARENZA_SIZE
                r.left = j * QUADRATO_TRASPARENZA_SIZE
                r.bottom = r.top + QUADRATO_TRASPARENZA_SIZE
                r.right = r.left + QUADRATO_TRASPARENZA_SIZE
                alphaBackgroundCanvas.drawRect(r, if (isWhite) paintWhite else paintGray)
                isWhite = !isWhite
            }
            verticalStartWhite = !verticalStartWhite
        }

        /*alphaPatternDrawable = AlphaPatternDrawable(dpToPx(4))
        alphaPatternDrawable!!.setBounds(
            Math.round(alphaRect.left.toFloat()), Math.round(
                alphaRect.top.toFloat()
            ), Math.round(alphaRect.right.toFloat()),
            Math.round(alphaRect.bottom.toFloat())
        )*/
    }

    fun setOnColorChangedListener(listener: OnColorChangedListener?) {
        onColorChangedListener = listener
    }


    interface OnColorChangedListener {
        fun onColorChanged(newColor: Int)
    }


    inner class BitmapCache{
        lateinit var canvas: Canvas
        lateinit var bitmap: Bitmap
        var value: Float = 0f
    }

    private val HUE_PANEL_WIDTH = dpToPx(30)
    private val ALPHA_PANEL_HEIGHT = dpToPx(20)
    private val PANEL_SPACING = dpToPx(10)
    private val CIRCLE_TRACKER_RADIUS = dpToPx(5)
    private val SLIDER_TRACKER_SIZE = dpToPx(4)
    private val SLIDER_TRACKER_OFFSET = dpToPx(2)
    private val QUADRATO_TRASPARENZA_SIZE = dpToPx(4)

    /**
     * The width in pixels of the border
     * surrounding all color panels.
     */
    private val BORDER_WIDTH = 1

}