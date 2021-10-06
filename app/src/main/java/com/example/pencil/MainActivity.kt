package com.example.pencil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils.split
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Adapter
import android.widget.EditText
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pencil)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home2)

        recentiFile = FileNotes(this, "recentiFile.txt")
        recentiFile.openFile()
        recentiFile.readFile()

        recentiRecyclerData(recentiFile.getTextFile())

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

    lateinit var recentiFile : FileNotes
    private var recentiData: MutableList<MutableMap<String,String>> = mutableListOf()

    fun addFileBottomSheet(view: View){
        val newFileMotionLayout = findViewById<MotionLayout>(R.id.newFileMotionLayout)
        when(newFileMotionLayout.currentState){
            R.id.close -> {
                newFileMotionLayout.transitionToState(R.id.open)
            }
            R.id.open -> {
                newFileMotionLayout.transitionToState(R.id.close)
            }
        }
    }

    fun filterBottomSheet(view: View){
        val floatingActionButton = findViewById<MotionLayout>(R.id.floatingActionButton)
        val newFileMotionLayout = findViewById<MotionLayout>(R.id.newFileMotionLayout)
        when(newFileMotionLayout.currentState){
            R.id.close -> {
                floatingActionButton.visibility = View.INVISIBLE
                newFileMotionLayout.visibility = View.VISIBLE
                newFileMotionLayout.transitionToState(R.id.open)
            }
            R.id.open -> {
                floatingActionButton.visibility = View.VISIBLE
                newFileMotionLayout.transitionToState(R.id.close)
            }
        }

        newFileMotionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
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
                if(newFileMotionLayout.currentState == R.id.close){
                    newFileMotionLayout.visibility = View.INVISIBLE
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

    fun createRecyclerHomeLayout(){
        recyclerHome = findViewById(R.id.recyclerHomeLayout)
        var adapter  = FileNotesAdapter(this, recentiData)
        recyclerHome.adapter = adapter

        var layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerHome.layoutManager = layoutManager

        adapter.setOnItemClickListener(object : FileNotesAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val intent = Intent(applicationContext, DrawActivity::class.java)
                intent.putExtra("titoloFile", recentiData[position]["titolo"])
                startActivity(intent)
            }
        })
    }

    fun createRecyclerFolderLayout(){
        recyclerFolder = findViewById(R.id.recyclerFolderLayout)
        recyclerFolder.adapter = FileNotesAdapter(this, recentiData)

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

    fun newFileNotes(view : View){
        val editTextTitolo = findViewById<EditText>(R.id.editTextTitolo)
        val editTextSottotitolo = findViewById<EditText>(R.id.editTextSottotitolo)
        val editTextData = findViewById<EditText>(R.id.editTextData)

        var nowDate = GregorianCalendar.getInstance(TimeZone.getDefault())
        var nowDateString = "" + nowDate.get(Calendar.YEAR) + "#" + (nowDate.get(Calendar.MONTH) + 1) + "#" + nowDate.get(Calendar.DAY_OF_MONTH)
        Log.d("calendar", nowDateString)

        var text = recentiFile.getTextFile()
        if(text != "") {
            text = "\n" + text
        }
        var textTemporaneo = "" + editTextTitolo.editableText + ";" + editTextSottotitolo.editableText + ";" + nowDateString
        text = textTemporaneo + text


        recentiFile.setTextFile(text)
        recentiFile.writeFile()

        recentiData.clear()
        recentiRecyclerData(text)
        recyclerHome.adapter!!.notifyItemChanged(1)

        //recyclerHome.adapter!!.notifyDataSetChanged()

        //adapter.notifyDataSetChanged()
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

            var todayText = true
            var lastWeekText = true
            var lastMonthText = true
            var olderText = true
            for (item in mListItem) {
                val listInf = split(item, ";").toList()
                val listDate = split(listInf[2], "#").toList()

                val date =
                    GregorianCalendar(listDate[0].toInt(), listDate[1].toInt(), listDate[2].toInt())
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
}