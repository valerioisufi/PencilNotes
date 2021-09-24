package com.example.pencil

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils.split
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        //setSupportActionBar(findViewById(R.id.appTollbar))
        /*var filesNames: Array<String> = this.fileList()

        for(i in filesNames){
            fileNotesList.add(FileNotes(this, i))
        }*/

        fileNotesList = FileNotes(this, "fileNotesList.txt")
        fileNotesList.openFile()
        fileNotesList.readFile()


        arrayString = split(fileNotesList.getTextFile(), "\n").toMutableList()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = FileNotesAdapter(this, arrayString)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)


        /*
        listView = findViewById(R.id.listView)
        adapter = FileNotesAdapter(this, fileNotesList.getTextFile())
        listView.adapter = adapter*/
    }

    lateinit var recyclerView: RecyclerView
    lateinit var fileNotesList : FileNotes
    lateinit var arrayString: MutableList<String>

    //lateinit var arrayString : Array<String>
    //var fileNotesList = mutableListOf<FileNotes>()

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