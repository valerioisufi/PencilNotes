package com.example.pencil.page.tool

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.pencil.DrawActivity
import com.example.pencil.R
import com.example.pencil.dpToPx
import com.example.pencil.page.path.stringToPath
import org.w3c.dom.Text
import kotlin.math.log

/**
 * TODO: document your custom view class.
 */
class StrumentoView(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {
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
    var sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    var typeStrumento = Pennello.PENNA
    lateinit var viewObject: Any

    init {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.StrumentoView, 0, 0
        )

        // Gets you the 'value' number - 0 or 666 in your example
        if (a.hasValue(R.styleable.StrumentoView_typeStrumento)) {
            val value = a.getInt(R.styleable.StrumentoView_typeStrumento, 0)
            typeStrumento = Pennello.fromInt(value)
        }

        viewObject = when(typeStrumento){
            Pennello.PENNA -> StrumentoPenna(context, attrs)
            Pennello.GOMMA -> StrumentoEvidenziatore(context, attrs)
            Pennello.EVIDENZIATORE -> StrumentoEvidenziatore(context, attrs)
            Pennello.LAZO -> StrumentoPenna(context, attrs)
            Pennello.TESTO -> StrumentoPenna(context, attrs)
        }

        a.recycle()
    }


    inner class StrumentoPenna(context: Context, attrs: AttributeSet){
        // variabili con i valori dell'oggetto, stroke (pt) e color
        var strokeWidth = sharedPref.getFloat("strokePenna", 2.5f)
        var color = sharedPref.getInt("colorPenna", ResourcesCompat.getColor(resources, R.color.colorPaint, null))

        init {
            setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY)

            setOnLongClickListener {
                strumentiDialog()
                return@setOnLongClickListener true
            }
        }

        fun strumentiDialog() {
            var dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_draw_paint)

            var window = dialog.window!!
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setGravity(Gravity.CENTER)
            window.attributes.windowAnimations = R.style.DialogAnimation

            dialog.setCancelable(true)
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val coloreTrattoColorPickerView = dialog.findViewById<ColorPickerView>(R.id.dialogDrawPaint_coloreTrattoColorPickerView)
            val dimensioneTrattoSeekbar = dialog.findViewById<SeekBar>(R.id.dialogDrawPaint_dimensioneTrattoSeekbar)
            val dimensioneTrattoTextView = dialog.findViewById<TextView>(R.id.dialogDrawPaint_dimensioneTrattoTextView)

            coloreTrattoColorPickerView.color = color
            dimensioneTrattoSeekbar.progress = (strokeWidth * 10).toInt()
            dimensioneTrattoTextView.text = (strokeWidth.toString() + "pt")

            dialog.show()

            dimensioneTrattoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    var progressTemp = (progress * 0.1).toFloat()
                    strokeWidth = progressTemp

                    dimensioneTrattoTextView.text = (progressTemp.toString() + "pt")

                    with (sharedPref.edit()) {
                        putFloat("strokePenna", strokeWidth)
                        apply()
                    }
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {
                    Toast.makeText(context,
                        "La dimesione è: " + (seek.progress * 0.1 ) + "pt",
                        Toast.LENGTH_SHORT).show()
                    //paint.strokeWidth = seek.progress.toFloat()
                }
            })

            coloreTrattoColorPickerView.setOnColorChangedListener(object : ColorPickerView.OnColorChangedListener{
                override fun onColorChanged(newColor: Int) {
                    //colorShowView.color = newColor
                    setColorFilter(newColor, android.graphics.PorterDuff.Mode.MULTIPLY)
                    color = newColor

                    with (sharedPref.edit()) {
                        putInt("colorPenna", newColor)
                        apply()
                    }
                }
            })
        }
    }

    inner class StrumentoEvidenziatore(context: Context, attrs: AttributeSet){
        // variabili con i valori dell'oggetto, stroke (pt) e color
        var strokeWidth = sharedPref.getFloat("strokeEvidenziatore", 10f)
        var color = sharedPref.getInt("colorEvidenziatore", ResourcesCompat.getColor(resources, R.color.colorEvidenziatore, null))

        init {
            setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY)

            setOnLongClickListener {
                strumentiDialog()
                return@setOnLongClickListener true
            }
        }

        fun strumentiDialog() {
            var dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_draw_paint)

            var window = dialog.window!!
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setGravity(Gravity.CENTER)
            window.attributes.windowAnimations = R.style.DialogAnimation

            dialog.setCancelable(true)
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val coloreTrattoColorPickerView = dialog.findViewById<ColorPickerView>(R.id.dialogDrawPaint_coloreTrattoColorPickerView)
            val dimensioneTrattoSeekbar = dialog.findViewById<SeekBar>(R.id.dialogDrawPaint_dimensioneTrattoSeekbar)
            val dimensioneTrattoTextView = dialog.findViewById<TextView>(R.id.dialogDrawPaint_dimensioneTrattoTextView)

            coloreTrattoColorPickerView.color = color
            dimensioneTrattoTextView.text = (strokeWidth.toString() + "pt")

            dialog.show()

            dimensioneTrattoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    var progressTemp = (progress * 0.1).toFloat()
                    strokeWidth = progressTemp

                    dimensioneTrattoTextView.text = (progressTemp.toString() + "pt")

                    with (sharedPref.edit()) {
                        putFloat("strokeEvidenziatore", strokeWidth)
                        apply()
                    }
                }
                override fun onStartTrackingTouch(seek: SeekBar) {}
                override fun onStopTrackingTouch(seek: SeekBar) {
                    Toast.makeText(context,
                        "La dimesione è: " + (seek.progress * 0.1 ) + "pt",
                        Toast.LENGTH_SHORT).show()
                    //paint.strokeWidth = seek.progress.toFloat()
                }
            })

            coloreTrattoColorPickerView.setOnColorChangedListener(object : ColorPickerView.OnColorChangedListener{
                override fun onColorChanged(newColor: Int) {
                    //colorShowView.color = newColor
                    setColorFilter(newColor, android.graphics.PorterDuff.Mode.MULTIPLY)
                    color = newColor

                    with (sharedPref.edit()) {
                        putInt("colorEvidenziatore", color)
                        apply()
                    }
                }
            })
        }
    }



    /*override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // TODO: consider storing these as member variables to reduce allocations per draw cycle.
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        var paint = Paint().apply {
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
        when(typeStrumento){
            Pennello.PENNA ->{
                var string = "m23.5779,0.8926c0.2009,0.2087 0.123,0.5275 0.0953,0.792 -0.1065,1.017 -0.508,2.0016 -0.9894,2.9038 -0.8402,1.5746 -2.0191,2.9639 -3.2486,4.2561 -0.9263,0.9736 -1.6323,1.7093 -2.6246,2.5673 -0.7647,0.6613 -1.5774,0.1015 -1.9387,-0.2419 -0.3454,-0.3284 -0.5658,-0.5418 -1.0725,-1.0931M23.583,0.8981c-0.1957,-0.2135 -0.5188,-0.1557 -0.7845,-0.1446 -1.0217,0.0427 -2.0294,0.382 -2.9599,0.8061 -1.624,0.7402 -3.0843,1.83 -4.4508,2.9763 -1.0295,0.8636 -1.8079,1.5223 -2.7263,2.459 -0.7078,0.7219 -0.1999,1.568 0.1203,1.95 0.3061,0.3652 0.5054,0.5985 1.0239,1.1386"
                var path = stringToPath(string)
                canvas.drawPath(path, paint)

            }
            Pennello.GOMMA ->{

            }
            Pennello.EVIDENZIATORE ->{

            }
            Pennello.LAZO ->{

            }
            Pennello.TESTO ->{

            }
        }

    }*/





    /*private var _exampleString: String? = null // TODO: use a default from R.string...
    private var _exampleColor: Int = Color.RED // TODO: use a default from R.color...
    private var _exampleDimension: Float = 0f // TODO: use a default from R.dimen...

    private lateinit var textPaint: TextPaint
    private var textWidth: Float = 0f
    private var textHeight: Float = 0f

    *//**
     * The text to draw
     *//*
    var exampleString: String?
        get() = _exampleString
        set(value) {
            _exampleString = value
            invalidateTextPaintAndMeasurements()
        }

    *//**
     * The font color
     *//*
    var exampleColor: Int
        get() = _exampleColor
        set(value) {
            _exampleColor = value
            invalidateTextPaintAndMeasurements()
        }

    *//**
     * In the example view, this dimension is the font size.
     *//*
    var exampleDimension: Float
        get() = _exampleDimension
        set(value) {
            _exampleDimension = value
            invalidateTextPaintAndMeasurements()
        }

    *//**
     * In the example view, this drawable is drawn above the text.
     *//*
    var exampleDrawable: Drawable? = null


    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.StrumentoView, defStyle, 0
        )

        _exampleString = a.getString(
            R.styleable.StrumentoView_exampleString
        )
        _exampleColor = a.getColor(
            R.styleable.StrumentoView_exampleColor,
            exampleColor
        )
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        _exampleDimension = a.getDimension(
            R.styleable.StrumentoView_exampleDimension,
            exampleDimension
        )

        if (a.hasValue(R.styleable.StrumentoView_exampleDrawable)) {
            exampleDrawable = a.getDrawable(
                R.styleable.StrumentoView_exampleDrawable
            )
            exampleDrawable?.callback = this
        }

        a.recycle()

        // Set up a default TextPaint object
        textPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            textAlign = Paint.Align.LEFT
        }

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements()
    }

    private fun invalidateTextPaintAndMeasurements() {
        textPaint.let {
            it.textSize = exampleDimension
            it.color = exampleColor
            textWidth = it.measureText(exampleString)
            textHeight = it.fontMetrics.bottom
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        exampleString?.let {
            // Draw the text.
            canvas.drawText(
                it,
                paddingLeft + (contentWidth - textWidth) / 2,
                paddingTop + (contentHeight + textHeight) / 2,
                textPaint
            )
        }

        // Draw the example drawable on top of the text.
        exampleDrawable?.let {
            it.setBounds(
                paddingLeft, paddingTop,
                paddingLeft + contentWidth, paddingTop + contentHeight
            )
            it.draw(canvas)
        }
    }*/
}