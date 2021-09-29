package com.example.pencil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils.split
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pencil)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home2)

        createRecyclerView()

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

    lateinit var recyclerView: RecyclerView
    lateinit var fileNotesList : FileNotes
    lateinit var arrayString: MutableList<String>

    fun addFileBottomSheet(view: View){
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

    fun createRecyclerView(){
        fileNotesList = FileNotes(this, "fileNotesList.txt")
        fileNotesList.openFile()
        fileNotesList.readFile()


        arrayString = split(fileNotesList.getTextFile(), "\n").toMutableList()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = FileNotesAdapter(this, arrayString)

        var layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        var dividerItemDecoration = DividerItemDecoration(recyclerView.context, layoutManager.orientation);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    fun newFileNotes(view : View){
        val editTextTitolo = findViewById<EditText>(R.id.editTextTitolo)
        val editTextSottotitolo = findViewById<EditText>(R.id.editTextSottotitolo)
        val editTextData = findViewById<EditText>(R.id.editTextData)

        var text = fileNotesList.getTextFile()
        if(text != "") {
            text += "\n"
        }
        var textTemporaneo = "" + editTextTitolo.editableText + ";" + editTextSottotitolo.editableText + ";" + editTextData.editableText
        text += textTemporaneo

        fileNotesList.setTextFile(text)
        fileNotesList.writeFile()

        arrayString.add(textTemporaneo)
        arrayString.size
        recyclerView.adapter!!.notifyItemChanged(arrayString.size - 1)

        //recyclerView.adapter!!.notifyDataSetChanged()

        //adapter.notifyDataSetChanged()
    }

    fun newActivity(view : View) {
        val intent = Intent(this, DrawActivity::class.java)
        startActivity(intent)
    }
}