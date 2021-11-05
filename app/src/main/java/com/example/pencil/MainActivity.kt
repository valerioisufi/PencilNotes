package com.example.pencil

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils.split
import android.util.Log
import android.util.Xml
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.textfield.TextInputEditText
import java.io.StringWriter
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pencil)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home2)

        recentiFile = FileManager(this, "recentiFile.txt")
        Log.d(TAG, "onCreate: " + recentiFile.text)

        recentiRecyclerData(recentiFile.text)

        createRecyclerHomeLayout()
        createRecyclerFolderLayout()

        // When the AppBarLayout progress changes, snap MotionLayout to the current progress
        val listener = AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
            // convert offset into % scrolled
            val seekPosition = -verticalOffset/appBar.totalScrollRange.toFloat()

            // inform MotionLayout of the animation progress
            val toolbarMotionLayout = findViewById<MotionLayout>(R.id.toolbarMotionLayout)
            toolbarMotionLayout.progress = seekPosition
        }
        val appBarLayout = findViewById<AppBarLayout>(R.id.appBarLayout)
        appBarLayout.addOnOffsetChangedListener(listener)
    }

    private lateinit var recyclerHome: RecyclerView
    private lateinit var recyclerFolder: RecyclerView

    private lateinit var switchMotionLayout: MotionLayout
    private lateinit var viewMotionLayout: MotionLayout

    lateinit var recentiFile : FileManager
    private var recentiData: MutableList<MutableMap<String,String>> = mutableListOf()


    fun createRecyclerHomeLayout(){
        recyclerHome = findViewById(R.id.recyclerHomeLayout)
        var adapter  = RecentiAdapter(this, recentiData)
        recyclerHome.adapter = adapter

        var layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerHome.layoutManager = layoutManager

        adapter.setOnItemClickListener(object : RecentiAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext, DrawActivity::class.java)
                intent.putExtra("titoloFile", recentiData[position]["titolo"])
                startActivity(intent)
            }

            override fun onMoreInfoClick(position: Int) {
                dettagliFileDialog(position, recentiData[position])
            }
        })
    }

    fun createRecyclerFolderLayout(){
        recyclerFolder = findViewById(R.id.recyclerFolderLayout)
        recyclerFolder.adapter = RecentiAdapter(this, recentiData)

        var layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerFolder.layoutManager = layoutManager

        switchMotionLayout = findViewById(R.id.switchMotionLayout)
        viewMotionLayout = findViewById(R.id.viewMotionLayout)
        switchMotionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
            }

            override fun onTransitionCompleted(
                motionLayout: MotionLayout?,
                currentId: Int
            ) {
                when(switchMotionLayout.currentState){
                    R.id.home_active -> {
                        viewMotionLayout.transitionToState(R.id.viewHome)

                    }
                    R.id.folder_active -> {
                        viewMotionLayout.transitionToState(R.id.viewFolder)

                    }
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }
        })
    }

    fun recentiRecyclerData(text : String){
        if(text != "") {
            var mListItem = split(text, "\n").toMutableList()

            var tempDate = GregorianCalendar.getInstance(TimeZone.getDefault())
            var today = GregorianCalendar(
                tempDate.get(Calendar.YEAR),
                tempDate.get(Calendar.MONTH),
                tempDate.get(Calendar.DAY_OF_MONTH)
            )

            var lastWeek = GregorianCalendar.getInstance(TimeZone.getDefault())
            lastWeek.add(Calendar.DAY_OF_MONTH, -7)
            var lastMonth = GregorianCalendar.getInstance(TimeZone.getDefault())
            lastMonth.add(Calendar.MONTH, -1)
            var older = GregorianCalendar.getInstance(TimeZone.getDefault())

            Log.d(TAG, "recentiRecyclerData: "+ lastWeek.get(Calendar.YEAR) + lastWeek.get(Calendar.MONTH) + lastWeek.get(Calendar.DAY_OF_MONTH))

            var todayText = true
            var lastWeekText = true
            var lastMonthText = true
            var olderText = true
            for (item in mListItem) {
                val listInf = split(item, ";").toList()
                val listDate = split(listInf[2], "#").toList()

                val date =
                    GregorianCalendar(listDate[0].toInt(), listDate[1].toInt() - 1, listDate[2].toInt())
                if (todayText && date.compareTo(today) >= 0) {
                    val listToAdd = mutableMapOf(Pair("type", "text"), Pair("textToWrite", "Oggi"))
                    recentiData.add(listToAdd)
                    todayText = false

                } else if (lastWeekText && date.compareTo(lastWeek) >= 0 && date.compareTo(today) < 0) {
                    recentiData.add(mutableMapOf(Pair("type", "divider")))
                    val listToAdd = mutableMapOf(Pair("type", "text"), Pair("textToWrite", "Scorsa settimana"))
                    recentiData.add(listToAdd)
                    lastWeekText = false

                } else if (lastMonthText && date.compareTo(lastMonth) >= 0 && date.compareTo(lastWeek) < 0) {
                    recentiData.add(mutableMapOf(Pair("type", "divider")))
                    val listToAdd = mutableMapOf(Pair("type", "text"), Pair("textToWrite", "Scorso mese"))
                    recentiData.add(listToAdd)
                    lastMonthText = false

                } else if (olderText && date.compareTo(lastMonth) < 0) {
                    recentiData.add(mutableMapOf(Pair("type", "divider")))
                    val listToAdd = mutableMapOf(Pair("type", "text"), Pair("textToWrite", "Meno recenti"))
                    recentiData.add(listToAdd)
                    olderText = false

                }

                val listToAdd = mutableMapOf(
                    Pair("type", "file"),
                    Pair("titolo", listInf[0]),
                    Pair("sottotitolo", listInf[1]),
                    Pair("data", listInf[2])
                )
                recentiData.add(listToAdd)
            }
        } else{
            val listToAdd = mutableMapOf(Pair("type", "text"), Pair("textToWrite", "Ancora nessun file"))
            recentiData.add(listToAdd)
        }
    }

    fun newActivity(view : View) {
        val intent = Intent(this, DrawActivity::class.java)
        startActivity(intent)
    }

    fun dettagliFileDialog(position : Int, data: MutableMap<String, String>) {
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

        titolo.text = data["titolo"]
        sottotitolo.text = data["sottotitolo"]

        val eliminaButton = dialog.findViewById<ConstraintLayout>(R.id.eliminaDialogDettagliFile)
        val rinominaButton = dialog.findViewById<ConstraintLayout>(R.id.rinominaDialogDettagliFile)
        val spostaButton = dialog.findViewById<ConstraintLayout>(R.id.spostaDialogDettagliFile)
        val modificaButton = dialog.findViewById<ConstraintLayout>(R.id.modificaDialogDettagliFile)

        eliminaButton.setOnClickListener {
            recentiData.removeAt(position)
            recyclerHome.adapter!!.notifyItemRemoved(position)

            recentiFile.removeLine(data["titolo"]!! + ";" + data["sottotitolo"]!! + ";" + data["data"]!!)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun newFileDialog(view: View){
        var dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_new_file)

        var window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.CENTER)
        window.attributes.windowAnimations = R.style.DialogAnimation

        dialog.setCancelable(true)
        window.setLayout(resources.displayMetrics.widthPixels - dpToPx(32, resources.displayMetrics), WRAP_CONTENT)

        dialog.show()


        val buttonConfermaNewFile = dialog.findViewById<Button>(R.id.buttonConfermaNewFile)
        buttonConfermaNewFile.setOnClickListener {
            val inputTitoloNewfile = dialog.findViewById<TextInputEditText>(R.id.inputTitoloNewfile)
            val inputSottotitoloNewFile = dialog.findViewById<TextInputEditText>(R.id.inputSottotitoloNewFile)

            var nowDate = GregorianCalendar.getInstance(TimeZone.getDefault())
            var nowDateString =
                "" + nowDate.get(Calendar.YEAR) + "#" + (nowDate.get(Calendar.MONTH) + 1) + "#" + nowDate.get(
                    Calendar.DAY_OF_MONTH
                )
            Log.d("calendar", nowDateString)


            val textTemporaneo =
                "" + inputTitoloNewfile.editableText + ";" + inputSottotitoloNewFile.editableText + ";" + nowDateString
            recentiFile.addLine(textTemporaneo, 0)

            recentiData.clear()
            recentiRecyclerData(recentiFile.text)

            recyclerHome.adapter!!.notifyItemChanged(0)
            recyclerHome.adapter!!.notifyItemInserted(1)

            dialog.dismiss()
        }
    }
}