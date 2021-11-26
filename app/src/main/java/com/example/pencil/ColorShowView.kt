package com.example.pencil

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlin.math.ceil

/**
 * TODO: document your custom view class.
 */
class ColorShowView(context: Context, attrs: AttributeSet) : View(context, attrs) {

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

            invalidate()
        }


    /**
     * Funzione onDraw
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(colorBackgroundBitmap, null, drawingRect, null)
        circlePath.rewind()
        circlePath.addCircle((width/2).toFloat(), (height/2).toFloat(), (drawingRect.width()/2).toFloat(), Path.Direction.CW)

        paintColor.color = color
        canvas.drawPath(circlePath, paintColor)
        canvas.drawPath(circlePath, paintBorder)
    }

    var paintBorder = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.gn_border_page, null)
        style = Paint.Style.STROKE
        strokeWidth = BORDER_WIDTH.toFloat()
        isAntiAlias = true
    }
    var paintColor = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val paintWhite = Paint().apply {
        color = -0x1
    }
    val paintGray = Paint().apply {
        color = -0x343435
    }


    /**
     * Imposto le dimensioni dei rettangoli di contenimento dei
     * selettori di colore
     */
    private lateinit var colorBackgroundBitmap: Bitmap
    private lateinit var drawingRect: Rect
    var circlePath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        drawingRect = Rect()
        drawingRect.left = paddingLeft + BORDER_WIDTH
        drawingRect.right = w - paddingRight - BORDER_WIDTH
        drawingRect.top = paddingTop + BORDER_WIDTH
        drawingRect.bottom = h - paddingBottom - BORDER_WIDTH


        if (::colorBackgroundBitmap.isInitialized) colorBackgroundBitmap.recycle()
        colorBackgroundBitmap = Bitmap.createBitmap(drawingRect.width(), drawingRect.height(), Bitmap.Config.ARGB_8888)
        val colorBackgroundCanvas = Canvas(colorBackgroundBitmap)

        circlePath.addCircle((width/2 - BORDER_WIDTH - paddingLeft).toFloat(), (height/2 - BORDER_WIDTH - paddingTop).toFloat(), (drawingRect.width()/2).toFloat(), Path.Direction.CW)
        colorBackgroundCanvas.clipPath(circlePath)

        val numRectanglesHorizontal = ceil((drawingRect.width() / QUADRATO_TRASPARENZA_SIZE).toDouble()).toInt()
        val numRectanglesVertical = ceil((drawingRect.height() / QUADRATO_TRASPARENZA_SIZE).toDouble()).toInt()

        val r = Rect()
        var verticalStartWhite = true
        for (i in 0..numRectanglesVertical) {
            var isWhite = verticalStartWhite
            for (j in 0..numRectanglesHorizontal) {
                r.top = i * QUADRATO_TRASPARENZA_SIZE
                r.left = j * QUADRATO_TRASPARENZA_SIZE
                r.bottom = r.top + QUADRATO_TRASPARENZA_SIZE
                r.right = r.left + QUADRATO_TRASPARENZA_SIZE
                colorBackgroundCanvas.drawRect(r, if (isWhite) paintWhite else paintGray)
                isWhite = !isWhite
            }
            verticalStartWhite = !verticalStartWhite
        }
    }


    private val QUADRATO_TRASPARENZA_SIZE = dpToPx(8)

    /**
     * The width in pixels of the border
     * surrounding all color panels.
     */
    private val BORDER_WIDTH = dpToPx(2)

}