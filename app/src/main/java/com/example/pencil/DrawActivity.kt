package com.example.pencil

import android.annotation.SuppressLint
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import android.graphics.Bitmap
import android.content.Context

import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.WorkerThread


class DrawActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw)

        drawView = findViewById(R.id.drawView)
        textView = findViewById(R.id.textView)
        commandView = findViewById(R.id.commandView)
        seekBar = findViewById(R.id.seekBar)
        colorPicker = findViewById(R.id.colorPicker)
        dimensioneTrattoTextView = findViewById(R.id.dimensioneTrattoTextView)
        blurEffect = findViewById(R.id.blurEffect)

        hideSystemUI()

        paintMatita = Paint().apply {
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
        paintPenna = Paint().apply {
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
        paintEvidenziatore = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.colorEvidenziatore, null)
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 40f // default: Hairline-width (really thin)
        }
        paintGomma = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.white, null)
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 30f // default: Hairline-width (really thin)
        }
        paintAreaSelezione = Paint().apply {
            color = ResourcesCompat.getColor(resources, R.color.colorAreaSelezione, null)
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.FILL // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 3f // default: Hairline-width (really thin)
        }

        //paint = drawView.getPaint()
        //density = resources.displayMetrics.density
        //drawViewTop = 80 * density

        textView.text = drawViewTop.toString()

        drawView.setOnHoverListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> hoverStart(event)
                MotionEvent.ACTION_HOVER_MOVE -> hoverMove(event)
                MotionEvent.ACTION_HOVER_EXIT -> hoverUp(event)
            }
            true
        }

        drawView.setOnTouchListener { v, event ->

            for (i in 0..event.pointerCount - 1) {
                if (event.getToolType(i) == MotionEvent.TOOL_TYPE_STYLUS) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> touchStart(event)
                        MotionEvent.ACTION_MOVE -> touchMove(event)
                        MotionEvent.ACTION_UP -> touchUp(event)
                    }
                }
            }

            if (event.pointerCount == 2){
                drawView.scaleTranslate(event)
            }

            true
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                when(pennelloAttivo){
                    Pennello.MATITA -> paintMatita.strokeWidth = progress.toFloat()
                    Pennello.PENNA -> paintPenna.strokeWidth = progress.toFloat()
                    Pennello.EVIDENZIATORE -> paintEvidenziatore.strokeWidth = progress.toFloat()
                    Pennello.GOMMA -> paintGomma.strokeWidth = progress.toFloat()
                }
                dimensioneTrattoTextView.text = seekBar.progress.toString()
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {
                Toast.makeText(this@DrawActivity,
                    "La dimesione è: " + seek.progress,
                    Toast.LENGTH_SHORT).show()
                //paint.strokeWidth = seek.progress.toFloat()
            }
        })

        colorPicker.setOnColorChangedListener(object : ColorPickerView.OnColorChangedListener{
            override fun onColorChanged(newColor: Int) {
                when(pennelloAttivo){
                    Pennello.MATITA -> paintMatita.color = newColor
                    Pennello.PENNA -> paintPenna.color = newColor
                    Pennello.EVIDENZIATORE -> paintEvidenziatore.color = newColor
                    Pennello.GOMMA -> paintGomma.color = newColor
                }
            }
        })

    }


    private lateinit var drawView: DrawView
    private lateinit var textView: TextView
    private lateinit var commandView: View
    private lateinit var seekBar: SeekBar
    private lateinit var colorPicker: ColorPickerView
    private lateinit var dimensioneTrattoTextView: TextView
    private lateinit var blurEffect: View

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, drawView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, drawView).show(WindowInsetsCompat.Type.systemBars())
    }

    //private var density : Float = 1f
    private var drawViewTop: Int = 0
    fun height(view: View) {
        textView.text = commandView.height.toString()

    }


    private var path : String = ""

    //private var motionTouchEventX = 0f
    //private var motionTouchEventY = 0f

    private var currentX = 0f
    private var currentY = 0f

    private fun touchStart(event: MotionEvent) {
        //path.reset()
        path = ""
        path = path + "M " + event.x + " " + event.y + " " //.moveTo(event.x, event.y)

        currentX = event.x
        currentY = event.y

        var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
        var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)

        if(tilt > 0.8f && orientation > -0.3f && orientation < 0.5f){
            paint = Paint(paintEvidenziatore)
            pennello = Pennello.EVIDENZIATORE
        } else if(tilt > 0.2f && (orientation > 2.5f || orientation < -2.3f)){
            paint = Paint(paintAreaSelezione)
            pennello = Pennello.AREA_SELEZIONE
        } else{
            pennello = pennelloAttivo
            when(pennelloAttivo){
                Pennello.MATITA -> paint = Paint(paintMatita)
                Pennello.PENNA -> paint = Paint(paintPenna)
                Pennello.EVIDENZIATORE -> paint = Paint(paintEvidenziatore)
                Pennello.GOMMA -> paint = Paint(paintGomma)
            }
        }

        drawView.newPath(path, paint)
    }

    private fun touchMove(event: MotionEvent) {
        // QuadTo() adds a quadratic bezier from the last point,
        // approaching control point (x1,y1), and ending at (x2,y2).
        textView.text =
            event.x.toString() + '\n' + event.y.toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_DISTANCE)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_TILT)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_ORIENTATION).toString()
        path = path + "Q " + currentX + " " + currentY + " " + (event.x + currentX) / 2 + " " + (event.y + currentY) / 2 + " " //.quadTo(currentX, currentY, (event.x + currentX) / 2, (event.y + currentY) / 2)



        currentX = event.x
        currentY = event.y

        // Draw the path in the extra bitmap to cache it.
        drawView.rewritePath(path)
        //drawView.setPathPaint(path, paint)
    }

    private fun touchUp(event: MotionEvent) {
        //var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
        //var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)


        if(pennello == Pennello.AREA_SELEZIONE){
            paint.color = ResourcesCompat.getColor(resources, R.color.colorAreaDefinitiva, null)
            paint.style = Paint.Style.FILL

            drawView.savePath(path,paint)
        }else{
            drawView.savePath(path, paint)
        }
        // Reset the path so it doesn't get drawn again.
        path = ""
    }

    private fun hoverMove(event: MotionEvent) {
        textView.text =
            event.x.toString() + '\n' + event.y.toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_DISTANCE)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_TILT)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_ORIENTATION).toString()

        /*if(event.getAxisValue(MotionEvent.AXIS_TILT) > 1.3f && event.getAxisValue(MotionEvent.AXIS_ORIENTATION) > -0.5f && event.getAxisValue(MotionEvent.AXIS_ORIENTATION) < 0.5f){
            path.quadTo(currentX, currentY, (event.x + currentX) / 2, (event.y + currentY) / 2)
            currentX = event.x
            currentY = event.y

            // Draw the path in the extra bitmap to cache it.
            drawView.setPath(path)
        }*/
    }

    private fun hoverStart(event: MotionEvent) {
        /*paint.color = ResourcesCompat.getColor(resources, R.color.colorEvidenziatore, null)
        paint.strokeWidth = 40f

        path.reset()
        path.moveTo(event.x, event.y)
        currentX = event.x
        currentY = event.y*/
    }

    private fun hoverUp(event: MotionEvent) {
        /*drawView.savePath(path)
        path.reset()*/
    }



    // Paint Object
    private lateinit var paint : Paint
    private var pennello = Pennello.MATITA
    private var pennelloAttivo = Pennello.MATITA
    private lateinit var paintMatita : Paint
    private lateinit var paintPenna: Paint
    private lateinit var paintEvidenziatore : Paint
    private lateinit var paintGomma : Paint
    private lateinit var paintAreaSelezione : Paint

    enum class Pennello {
        MATITA,
        PENNA,
        EVIDENZIATORE,
        GOMMA,

        AREA_SELEZIONE
    }


    /**
     * Blurs the given Bitmap image
     * @param bitmap Image to blur
     * @param applicationContext Application context
     * @return Blurred bitmap image
     */
    @WorkerThread
    fun blurBitmap(bitmap: Bitmap, applicationContext: Context): Bitmap {
        lateinit var rsContext: RenderScript
        try {

            // Create the output bitmap
            val output = Bitmap.createBitmap(
                bitmap.width, bitmap.height, bitmap.config)

            // Blur the image
            rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG)
            val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
            val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)
            val theIntrinsic = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
            theIntrinsic.apply {
                setRadius(10f)
                theIntrinsic.setInput(inAlloc)
                theIntrinsic.forEach(outAlloc)
            }
            outAlloc.copyTo(output)

            return output
        } finally {
            rsContext.finish()
        }
    }

    // Tasti
    fun showPanel(view: View){
        var pencilLayout = findViewById<ConstraintLayout>(R.id.pencilLayout)
        if(pencilLayout.visibility == View.VISIBLE){
            pencilLayout.visibility = View.INVISIBLE
            blurEffect.visibility = View.INVISIBLE
        } else{
            pencilLayout.visibility = View.VISIBLE

            var bitmap = drawView.drawToBitmap(Bitmap.Config.ARGB_8888)
            drawView.drawToBitmap(Bitmap.Config.ARGB_8888)
            bitmap = blurBitmap(bitmap, this)
            val d: Drawable = BitmapDrawable(resources, bitmap)
            blurEffect.background = d
            blurEffect.visibility = View.VISIBLE

        }
        var id = resources.getResourceEntryName(view.id)
        when(id){
            "Matita" -> pennelloAttivo = Pennello.MATITA
            "Penna" -> pennelloAttivo = Pennello.PENNA
            "Evidenziatore" -> pennelloAttivo = Pennello.EVIDENZIATORE
            "Gomma" -> pennelloAttivo = Pennello.GOMMA
        }

        when(pennelloAttivo){
            Pennello.MATITA -> {
                seekBar.progress = paintMatita.strokeWidth.toInt()
                colorPicker.color = paintMatita.color
            }
            Pennello.PENNA -> {
                seekBar.progress = paintPenna.strokeWidth.toInt()
                colorPicker.color = paintPenna.color
            }
            Pennello.EVIDENZIATORE -> {
                seekBar.progress = paintEvidenziatore.strokeWidth.toInt()
                colorPicker.color = paintEvidenziatore.color
            }
            Pennello.GOMMA -> {
                seekBar.progress = paintGomma.strokeWidth.toInt()
                colorPicker.color = paintGomma.color
            }
        }

        dimensioneTrattoTextView.text = seekBar.progress.toString()
    }
}