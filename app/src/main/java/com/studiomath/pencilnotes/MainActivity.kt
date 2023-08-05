package com.studiomath.pencilnotes

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studiomath.pencilnotes.file.FileFolderXml
import com.google.android.material.textfield.TextInputEditText
import java.io.File


private const val TAG = "MainActivity"

open class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pencil)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                rootSequence.removeLast()
                var titoloRoot = ""
                if (rootSequence.isEmpty()) {
                    titoloRoot = getString(R.string.activity_home_titolo)
                } else {
                    titoloRoot = rootSequence.last()
                }
                activity_home_titolo_tv.text = titoloRoot

                adapter.dataSet = fileFolder.data[getRoot()]
                activity_home_sfoglia_rv.adapter!!.notifyDataSetChanged()
                if (rootSequence.isEmpty()) isEnabled = false
            }
        }
        onBackPressedDispatcher.addCallback(
            this,  // LifecycleOwner
            callback
        )

        // The callback can be enabled or disabled here or in the lambda

        fileFolder = FileFolderXml(this, "fileFolderList.xml")
        fileFolder.readXML()

        /**
         * activity_home_sfoglia_rv
         */
        activity_home_sfoglia_rv = findViewById(R.id.activity_home_sfoglia_rv)

        adapter = FileFolderAdapter(this, fileFolder.data["/"])
        activity_home_sfoglia_rv.adapter = adapter

        var layoutManager = LinearLayoutManager(this)
        activity_home_sfoglia_rv.layoutManager = layoutManager

        adapter.setOnFolderClickListener(object : FileFolderAdapter.OnFolderClickListener {
            override fun onItemClick(position: Int) {
                activity_home_titolo_tv.text = fileFolder.data[getRoot()]!![position]["nome"]

                callback.isEnabled = true
                rootSequence.add(fileFolder.data[getRoot()]!![position]["nome"]!!)

                adapter.dataSet = fileFolder.data[getRoot()]
                activity_home_sfoglia_rv.adapter!!.notifyDataSetChanged()
            }

            override fun onMoreInfoClick(position: Int) {
                dettagliFileDialog(position)
            }

        })

        adapter.setOnFileClickListener(object : FileFolderAdapter.OnFileClickListener {
            override fun onItemClick(position: Int) {

                val intent = Intent(applicationContext, DrawActivity::class.java)
                intent.putExtra("titoloFile", fileFolder.data[getRoot()]!![position]["nome"])
                intent.putExtra("cartellaFile", "documenti${getRoot()}")
                startActivity(intent)
            }

            override fun onMoreInfoClick(position: Int) {
                dettagliFileDialog(position)
            }

        })


        /**
         * activity_home_root_tv
         */
        activity_home_titolo_tv = findViewById(R.id.activity_home_titolo_tv)

    }

    private lateinit var activity_home_titolo_tv: TextView
    private lateinit var adapter: FileFolderAdapter
    private lateinit var activity_home_sfoglia_rv: RecyclerView
    lateinit var fileFolder: FileFolderXml

    var rootSequence = mutableListOf<String>()
    fun getRoot(): String {
        var root = "/"
        for(sequence in rootSequence){
            root += "$sequence/"
        }
        return root
    }


    /**
     * Dialog
     */
    fun newFolderDialog(view: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_new_folder)

        val window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.CENTER)
        window.attributes.windowAnimations = R.style.DialogAnimation

        dialog.setCancelable(true)
        window.setLayout(
            resources.displayMetrics.widthPixels - dpToPx(
                32,
                resources.displayMetrics
            ), WRAP_CONTENT
        )

        dialog.show()

        val dialog_new_folder_conferma_button =
            dialog.findViewById<Button>(R.id.dialog_new_folder_conferma_button)
        dialog_new_folder_conferma_button.setOnClickListener {
            dialog.dismiss()

            val dialog_new_folder_nome_textInputEditText =
                dialog.findViewById<TextInputEditText>(R.id.dialog_new_folder_nome_textInputEditText)
            var nome = dialog_new_folder_nome_textInputEditText.editableText.toString()

            fileFolder.data[getRoot()]?.add(0, mutableMapOf(Pair("type", "folder"), Pair("nome", nome)))
            fileFolder.data["${getRoot()}$nome/"] = mutableListOf()
            activity_home_sfoglia_rv.adapter!!.notifyItemInserted(0)
            fileFolder.writeXML()
        }
    }

    fun newFileDialog(view: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_new_file)

        val window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.CENTER)
        window.attributes.windowAnimations = R.style.DialogAnimation

        dialog.setCancelable(true)
        window.setLayout(
            resources.displayMetrics.widthPixels - dpToPx(
                32,
                resources.displayMetrics
            ), WRAP_CONTENT
        )

        dialog.show()

        val dialog_new_file_conferma_button =
            dialog.findViewById<Button>(R.id.dialog_new_file_conferma_button)
        dialog_new_file_conferma_button.setOnClickListener {
            dialog.dismiss()

            val dialog_new_file_nome_textInputEditText =
                dialog.findViewById<TextInputEditText>(R.id.dialog_new_file_nome_textInputEditText)
            var nome = dialog_new_file_nome_textInputEditText.editableText.toString()

            fileFolder.data[getRoot()]?.add(0, mutableMapOf(Pair("type", "file"), Pair("nome", nome)))
            activity_home_sfoglia_rv.adapter!!.notifyItemInserted(0)
            fileFolder.writeXML()
        }

//        val buttonConfermaNewFile = dialog.findViewById<Button>(R.id.dialog_new_folder_conferma_button)
//        buttonConfermaNewFile.setOnClickListener {
//            val inputTitoloNewfile = dialog.findViewById<TextInputEditText>(R.id.inputTitoloNewfile)
//            val inputSottotitoloNewFile = dialog.findViewById<TextInputEditText>(R.id.inputSottotitoloNewFile)
//
//            var nowDate = GregorianCalendar.getInstance(TimeZone.getDefault())
//            var nowDateString =
//                "" + nowDate.get(Calendar.YEAR) + "#" + (nowDate.get(Calendar.MONTH) + 1) + "#" + nowDate.get(
//                    Calendar.DAY_OF_MONTH
//                )
//            Log.d("calendar", nowDateString)
//
//
//            val textTemporaneo =
//                "" + inputTitoloNewfile.editableText + ";" + inputSottotitoloNewFile.editableText + ";" + nowDateString
//            recentiFile.addLine(textTemporaneo, 0)
//
//            recentiData.clear()
//            recentiRecyclerData(recentiFile.text)
//
//            recyclerHome.adapter!!.notifyItemChanged(0)
//            recyclerHome.adapter!!.notifyItemInserted(1)
//
//            dialog.dismiss()
//        }
    }

    fun dettagliFileDialog(position: Int) {
        var dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_dettagli_file)

        var window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.BOTTOM)
        window.attributes.windowAnimations = R.style.DialogAnimation

        dialog.setCancelable(true)
        window.setLayout(resources.displayMetrics.widthPixels, WRAP_CONTENT)

        val titolo = dialog.findViewById<TextView>(R.id.titoloDialogDettagliFile)
        val sottotitolo = dialog.findViewById<TextView>(R.id.sottotitoloDialogDettagliFile)

        titolo.text = fileFolder.data[getRoot()]!![position]["nome"]
//        sottotitolo.text = data["sottotitolo"]

        val eliminaButton = dialog.findViewById<ConstraintLayout>(R.id.eliminaDialogDettagliFile)
        val rinominaButton = dialog.findViewById<ConstraintLayout>(R.id.rinominaDialogDettagliFile)
        val spostaButton = dialog.findViewById<ConstraintLayout>(R.id.spostaDialogDettagliFile)
        val modificaButton = dialog.findViewById<ConstraintLayout>(R.id.modificaDialogDettagliFile)

        eliminaButton.setOnClickListener {
            val nomeFile = "${fileFolder.data[getRoot()]!![position]["nome"]}.xml"
            val cartellaFile = "documenti${getRoot()}${fileFolder.data[getRoot()]!![position]["nome"]}"

            val file = File(this.filesDir, "$cartellaFile/$nomeFile")
            val cartella = File(this.filesDir, cartellaFile)

            if(file.delete() && cartella.delete()){
                fileFolder.data[getRoot()]?.removeAt(position)
                activity_home_sfoglia_rv.adapter!!.notifyItemRemoved(position)
                fileFolder.writeXML()
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}