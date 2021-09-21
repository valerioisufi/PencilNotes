package com.example.pencil

import android.content.Context
import java.io.File

class FileNotes {
    private var context : Context
    private lateinit var file : File
    private var nomeFile = ""
    private var testoFile = ""

    constructor(context: Context, nomeFile: String){
        this.nomeFile = nomeFile
        this.context = context

    }
    fun openFile(){
        file = File(context.filesDir, this.nomeFile)
    }
}