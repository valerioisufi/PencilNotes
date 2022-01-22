package com.example.pencil

import android.annotation.SuppressLint
import android.app.Activity
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
import android.view.GestureDetector
import androidx.core.app.ActivityCompat
import com.example.pencil.customView.ColorPickerView
import com.example.pencil.customView.ColorShowView
import com.example.pencil.file.FileManager
import com.example.pencil.document.DrawView
import com.example.pencil.document.path.DrawMotionEvent
import com.example.pencil.document.tool.*
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowInsets
import android.widget.Toast
import kotlinx.coroutines.*
import androidx.activity.OnBackPressedCallback

import androidx.annotation.NonNull





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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_draw)

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
        drawView.changePage(nPage)


//        // 'content' is the root view of your layout xml.
//        val rootView = findViewById<ConstraintLayout>(R.id.drawViewRoot)
//        val treeObserver: ViewTreeObserver = rootView.viewTreeObserver
//        treeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                updateGestureExclusion(this@DrawActivity)
//            }
//        })

        /**
         * Rendo invisible le barre di sistema
         */
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // TODO: The system bars are visible. Make any desired
                // adjustments to your UI, such as showing the action bar or
                // other navigational controls.

                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    delay(1000)
                    hideSystemUI()
                }

            } else {
                // TODO: The system bars are NOT visible. Make any desired
                // adjustments to your UI, such as hiding the action bar or
                // other navigational controls.
            }
        }

        /**
         * Impedisco all'utente di abbandonare l'activity con il tasto back
         */
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@DrawActivity, "Vuoi tornare indietro?", Toast.LENGTH_SHORT).show()
            }
        }
        onBackPressedDispatcher.addCallback(
            this,  // LifecycleOwner
            callback
        )

    }

    override fun onResume() {
        super.onResume()

        hideSystemUI()

//        // Hide the status bar.
//        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//        // Remember that you should never show the action bar if the
//        // status bar is hidden, so hide that too if necessary.
//        actionBar?.hide()
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