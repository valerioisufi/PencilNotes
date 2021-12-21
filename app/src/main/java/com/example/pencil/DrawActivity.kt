package com.example.pencil

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

import android.text.TextUtils.replace
import com.google.android.material.chip.Chip
import android.graphics.pdf.PdfRenderer
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.example.pencil.file.FileManager
import com.example.pencil.document.DrawView
import com.example.pencil.document.page.GestionePagina
import com.example.pencil.document.path.DrawMotionEvent
import com.example.pencil.document.tool.*

lateinit var sharedPref: SharedPreferences
lateinit var drawImpostazioni: DrawImpostazioni

private const val TAG = "DrawActivity"
class DrawActivity : AppCompatActivity() {
    lateinit var contatoreTextView: TextView
    lateinit var drawView: DrawView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pencil)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw2)

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        drawView = findViewById(R.id.drawView)
        drawImpostazioni = findViewById(R.id.drawImpostazioni)

        val drawMotionEvent = DrawMotionEvent(this, drawView)
        drawView.setDrawMotioEvent(drawMotionEvent)


        /*textViewData = findViewById(R.id.textViewData)
        commandView = findViewById(R.id.commandView)
        seekBar = findViewById(R.id.seekBar)
        modePennaView = findViewById(R.id.modePennaView)
        colorPicker = findViewById(R.id.colorPicker)
        colorShowView = findViewById(R.id.colorShowView)
        dimensioneTrattoTextView = findViewById(R.id.dimensioneTrattoTextView)
        blurEffect = findViewById(R.id.blurEffect)
        contatoreTextView = findViewById(R.id.contatoreTextView)*/

        //gestionePagina = GestionePagina(this)
        contatoreTextView = findViewById(R.id.contatoreTextView)

        drawView.strumentoPenna = StrumentoPenna(this, findViewById(R.id.strumento_penna))
        drawView.strumentoEvidenziatore = StrumentoEvidenziatore(this, findViewById(R.id.strumento_evidenziatore))
        drawView.strumentoGomma = StrumentoGomma(this, findViewById(R.id.strumento_gomma))
        drawView.strumentoLazo = StrumentoLazo(this, findViewById(R.id.strumento_lazo))
        drawView.strumentoTesto = StrumentoTesto(this, findViewById(R.id.strumento_testo))


        var intent = intent
        val titoloFile = intent.getStringExtra("titoloFile")
        nomeFile = replace(titoloFile, arrayOf(" "), arrayOf("_")).toString()
        cartella = nomeFile
        nomeFile += ".xml"

        drawView.readFile(nomeFile, cartella)
        drawView.readPage(nPage)


        //hideSystemUI()

        /*// Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        actionBar?.hide()*/


        //paint = drawView.getPaint()
        //density = resources.displayMetrics.density
        //drawViewTop = 80 * density




    }

    var cartella = ""
    var nomeFile = ""
    var nPage = 0

    private lateinit var textViewData: TextView
    private lateinit var commandView: ConstraintLayout
    private lateinit var seekBar: SeekBar
    private lateinit var modePennaView: Chip
    private lateinit var colorPicker: ColorPickerView
    private lateinit var colorShowView: ColorShowView
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


    fun choosePennello(view: View){
        var id = resources.getResourceEntryName(view.id)
        when(id){
            "strumento_penna" -> {
                drawView.strumentoAttivo = DrawView.Pennello.PENNA
            }
            "strumento_evidenziatore" -> {
                drawView.strumentoAttivo = DrawView.Pennello.EVIDENZIATORE
            }
            "strumento_gomma" -> {
                drawView.strumentoAttivo = DrawView.Pennello.GOMMA
            }
            "strumento_lazo" -> {
                drawView.strumentoAttivo = DrawView.Pennello.LAZO
            }
            "strumento_testo" -> {
                drawView.strumentoAttivo = DrawView.Pennello.TESTO
            }
        }

    }

    /*// Tasti
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
    }*/


    /*fun closeViewLayout(view: View){
        var impostazioniLayout = findViewById<ConstraintLayout>(R.id.impostazioniLayout)
        if(impostazioniLayout.visibility == View.VISIBLE){
            impostazioniLayout.visibility = View.INVISIBLE
        }

        var pencilLayout = findViewById<ConstraintLayout>(R.id.pencilLayout)
        if(pencilLayout.visibility == View.VISIBLE){
            pencilLayout.visibility = View.INVISIBLE
        }

        blurEffect.visibility = View.INVISIBLE
    }*/

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

        ActivityCompat.startActivityForResult(this, intent, PICK_PDF_FILE, null)
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
}