package com.example.pencil

import android.annotation.SuppressLint
import android.app.Activity
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
import android.content.Intent
import android.content.SharedPreferences

import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.TextUtils.replace
import androidx.annotation.WorkerThread
import com.google.android.material.chip.Chip
import android.graphics.pdf.PdfRenderer
import com.example.pencil.file.FileManager
import com.example.pencil.page.DrawView
import com.example.pencil.page.tool.ColorPickerView
import com.example.pencil.page.tool.ColorShowView

private const val TAG = "DrawActivity"
class DrawActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pencil)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw2)


        drawView = findViewById(R.id.drawView)
        textViewData = findViewById(R.id.textViewData)
        commandView = findViewById(R.id.commandView)
        seekBar = findViewById(R.id.seekBar)
        modePennaView = findViewById(R.id.modePennaView)
        colorPicker = findViewById(R.id.colorPicker)
        colorShowView = findViewById(R.id.colorShowView)
        dimensioneTrattoTextView = findViewById(R.id.dimensioneTrattoTextView)
        blurEffect = findViewById(R.id.blurEffect)
        contatoreTextView = findViewById(R.id.contatoreTextView)


        var intent = intent
        val titoloFile = intent.getStringExtra("titoloFile")
        nomeFile = replace(titoloFile, arrayOf(" "), arrayOf("_")).toString()
        cartella = nomeFile
        nomeFile += ".xml"

        drawView.readFile(nomeFile, cartella)
        drawView.readPage(nPage)


        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        modePenna = sharedPref.getBoolean(getString(R.string.mode_penna), true)
        modePennaView.isChecked = modePenna


        //hideSystemUI()

        /*// Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        actionBar?.hide()*/

        paintMatita = Paint().apply {
            color = sharedPref.getInt("colorMatita", ResourcesCompat.getColor(resources, R.color.colorPaint, null))
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
            color = sharedPref.getInt("colorPenna", ResourcesCompat.getColor(resources, R.color.colorPaint, null))
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
            color = sharedPref.getInt("colorEvidenziatore", ResourcesCompat.getColor(resources, R.color.colorEvidenziatore, null))
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 20f // default: Hairline-width (really thin)
        }
        paintGomma = Paint().apply {
            color = sharedPref.getInt("colorGomma", ResourcesCompat.getColor(resources, R.color.white, null))
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = 20f // default: Hairline-width (really thin)
        }
        paintAreaSelezione = Paint().apply {
            color = sharedPref.getInt("colorAreaSelezione", ResourcesCompat.getColor(resources, R.color.colorAreaSelezione, null))
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

        colorShowView.color = paintMatita.color
        textViewData.text = drawViewTop.toString()

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

            if (!modePenna && event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER){
                if(!continueScaleTranslate) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> touchStart(event)
                        MotionEvent.ACTION_MOVE -> touchMove(event)
                        MotionEvent.ACTION_UP -> touchUp(event)
                    }
                } else {
                    when (event.action) {
                        MotionEvent.ACTION_UP -> continueScaleTranslate = false
                    }

                }
            }

            if (event.pointerCount == 2){
                drawView.scaleTranslate(event)
                if(!modePenna) {
                    drawView.rewritePath("")
                }
                continueScaleTranslate = true
            }

            true
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                var progressTemp = (progress * 0.1).toFloat()
                when(pennelloAttivo){
                    Pennello.MATITA -> paintMatita.strokeWidth = progressTemp
                    Pennello.PENNA -> paintPenna.strokeWidth = progressTemp
                    Pennello.EVIDENZIATORE -> paintEvidenziatore.strokeWidth = progressTemp
                    Pennello.GOMMA -> paintGomma.strokeWidth = progressTemp
                }
                dimensioneTrattoTextView.text = progressTemp.toString()
            }
            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {
                Toast.makeText(this@DrawActivity,
                    "La dimesione è: " + (seek.progress * 0.1).toFloat(),
                    Toast.LENGTH_SHORT).show()
                //paint.strokeWidth = seek.progress.toFloat()
            }
        })
        modePennaView.setOnCheckedChangeListener { buttonView, isChecked ->
            run {
                modePenna = isChecked
                with (sharedPref.edit()) {
                    putBoolean(getString(R.string.mode_penna), isChecked)
                    apply()
                }
            }
        }

        colorPicker.setOnColorChangedListener(object : ColorPickerView.OnColorChangedListener{
            override fun onColorChanged(newColor: Int) {
                colorShowView.color = newColor

                when(pennelloAttivo){
                    Pennello.MATITA -> {
                        paintMatita.color = newColor
                        with (sharedPref.edit()) {
                            putInt("colorMatita", newColor)
                            apply()
                        }
                    }
                    Pennello.PENNA -> {
                        paintPenna.color = newColor
                        with (sharedPref.edit()) {
                            putInt("colorPenna", newColor)
                            apply()
                        }
                    }
                    Pennello.EVIDENZIATORE -> {
                        paintEvidenziatore.color = newColor
                        with (sharedPref.edit()) {
                            putInt("colorEvidenziatore", newColor)
                            apply()
                        }
                    }
                    Pennello.GOMMA -> {
                        paintGomma.color = newColor
                        with (sharedPref.edit()) {
                            putInt("colorGomma", newColor)
                            apply()
                        }
                    }
                }
            }
        })

    }

    private lateinit var sharedPref: SharedPreferences

    private lateinit var drawView: DrawView
    private lateinit var textViewData: TextView
    private lateinit var commandView: ConstraintLayout
    private lateinit var seekBar: SeekBar
    private lateinit var modePennaView: Chip
    private lateinit var colorPicker: ColorPickerView
    private lateinit var colorShowView: ColorShowView
    private lateinit var dimensioneTrattoTextView: TextView
    private lateinit var blurEffect: View
    private lateinit var contatoreTextView: TextView

    private var modePenna = true
    private var continueScaleTranslate = false

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
        textViewData.text = commandView.height.toString()

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
        /*textViewData.text =
            event.x.toString() + '\n' + event.y.toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_DISTANCE)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_TILT)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_ORIENTATION).toString()*/
        //path = path + "Q " + currentX + " " + currentY + " " + (event.x + currentX) / 2 + " " + (event.y + currentY) / 2 + " " //.quadTo(currentX, currentY, (event.x + currentX) / 2, (event.y + currentY) / 2)
        path = path + "L " + event.x + " " + event.y + " "

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
        /*textViewData.text =
            event.x.toString() + '\n' + event.y.toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_DISTANCE)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_TILT)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_ORIENTATION).toString()*/

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


    // Tasti
    fun showColorPickerView(view: View){
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

        //var colorShowView = findViewById<ColorShowView>(R.id.colorShowView)

        when(pennelloAttivo){
            Pennello.MATITA -> {
                seekBar.progress = (paintMatita.strokeWidth * 10).toInt()
                colorPicker.color = paintMatita.color
            }
            Pennello.PENNA -> {
                seekBar.progress = (paintPenna.strokeWidth * 10).toInt()
                colorPicker.color = paintPenna.color
            }
            Pennello.EVIDENZIATORE -> {
                seekBar.progress = (paintEvidenziatore.strokeWidth * 10).toInt()
                colorPicker.color = paintEvidenziatore.color
            }
            Pennello.GOMMA -> {
                seekBar.progress = (paintGomma.strokeWidth * 10).toInt()
                colorPicker.color = paintGomma.color
            }
        }

        dimensioneTrattoTextView.text = (seekBar.progress * 0.1).toFloat().toString()
    }

    fun choosePennello(view: View){
        var id = resources.getResourceEntryName(view.id)
        when(id){
            "Matita" -> {
                pennelloAttivo = Pennello.MATITA
                colorShowView.color = paintMatita.color
            }
            "Penna" -> {
                pennelloAttivo = Pennello.PENNA
                colorShowView.color = paintPenna.color
            }
            "Evidenziatore" -> {
                pennelloAttivo = Pennello.EVIDENZIATORE
                colorShowView.color = paintEvidenziatore.color
            }
            "Gomma" -> {
                pennelloAttivo = Pennello.GOMMA
                colorShowView.color = paintGomma.color
            }
        }

    }

    fun showImpostazioniView(view: View){
        var impostazioniLayout = findViewById<ConstraintLayout>(R.id.impostazioniLayout)
        if(impostazioniLayout.visibility == View.VISIBLE){
            impostazioniLayout.visibility = View.INVISIBLE
            blurEffect.visibility = View.INVISIBLE
        } else{
            impostazioniLayout.visibility = View.VISIBLE

            var bitmap = drawView.drawToBitmap(Bitmap.Config.ARGB_8888)
            drawView.drawToBitmap(Bitmap.Config.ARGB_8888)
            bitmap = blurBitmap(bitmap, this)
            val d: Drawable = BitmapDrawable(resources, bitmap)
            blurEffect.background = d
            blurEffect.visibility = View.VISIBLE
        }

        dimensioneTrattoTextView.text = (seekBar.progress * 0.1).toFloat().toString()
    }

    fun closeViewLayout(view: View){
        var impostazioniLayout = findViewById<ConstraintLayout>(R.id.impostazioniLayout)
        if(impostazioniLayout.visibility == View.VISIBLE){
            impostazioniLayout.visibility = View.INVISIBLE
        }

        var pencilLayout = findViewById<ConstraintLayout>(R.id.pencilLayout)
        if(pencilLayout.visibility == View.VISIBLE){
            pencilLayout.visibility = View.INVISIBLE
        }

        blurEffect.visibility = View.INVISIBLE
    }

    var cartella = ""
    var nomeFile = ""
    var nPage = 0
    fun redo(view: View){
        nPage += 1
        drawView.changePage(nPage)

        contatoreTextView.text = ("n." + nPage)

        //textViewData.text = drawView.drawFile.fileManager.file.readText()
    }
    fun undo(view: View){
        if (nPage > 0) {
            nPage -= 1
            drawView.changePage(nPage)

            contatoreTextView.text = ("n." + nPage)
        }
    }

    val PICK_PDF_FILE = 2
    fun addObject(view: View){
        // Request code for selecting a PDF document.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            //putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        startActivityForResult(intent, PICK_PDF_FILE)
    }
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == PICK_PDF_FILE
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                val id = drawView.addRisorsa(cartella, ".pdf")
                val inputStream = contentResolver.openInputStream(uri)
                val outputFile = FileManager(this, id + ".pdf", cartella)
                val outputStream = outputFile.file.outputStream()
                //Log.d(TAG, "onActivityResult: " + uri + ";" + uri.path)

                val buffer = ByteArray(1024)
                var n = 0
                if (inputStream != null) {
                    while (inputStream.read(buffer).also { n = it } != -1) outputStream.write(buffer, 0, n)
                }

                inputStream?.close()
                outputStream.close()


                // Perform operations on the document using its URI.
                // create a new renderer
                val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                val renderer = PdfRenderer(parcelFileDescriptor!!)

                // let us just render all pages
                val pageCount = renderer.pageCount

                var indexPage = nPage
                for (indexPdf in 0 until pageCount){
                    drawView.addBackgroundPdf(id, indexPdf, indexPage)
                    indexPage++
                }

                /*for (i in 0 until pageCount) {
                    val page: PdfRenderer.Page = renderer.openPage(1)

                    var bitmapTemp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    // say we render for showing on the screen
                    page.render(bitmapTemp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    // do stuff with the bitmap
                    drawView.backgroundPage = bitmapTemp
                    //bitmapTemp.recycle()

                    // close the page
                    page.close()
                }*/

                // close the renderer
                renderer.close()


            }
        }
    }
}