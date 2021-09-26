package com.example.pencil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils.split
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
        var text = fileNotesList.getTextFile()
        if(text != ""){
            text += "\nNuovo blocco note;Sottotitolo;Data"
        }else{
            text = "Nuovo blocco note;Sottotitolo;Data"
        }
        fileNotesList.setTextFile(text)
        fileNotesList.writeFile()

        arrayString.add("Nuovo blocco note;Sottotitolo;Data")
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