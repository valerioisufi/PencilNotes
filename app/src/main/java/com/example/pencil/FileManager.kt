package com.example.pencil

import android.content.Context
import android.text.TextUtils.split
import java.io.File

class FileManager(context: Context, nomeFile: String) {
    // creo l'oggetto File(), lo apro e lo leggo
    private var file: File = File(context.filesDir, nomeFile)

    // variabili che ospitano il testo contenuto nel file
    var text = ""
        get() = field
        set(value) {
            field = value
            listLine = split(field, "\n").toMutableList()
        }
    private lateinit var listLine: MutableList<String>

    init {
        if (!file.exists()) {
            file.createNewFile()
        }

        readFromFile()
    }


    // funzioni per la lettura e scrittura su file
    private fun readFromFile() {
        text = file.readText()
    }

    fun writeToFile() {
        file.writeText(text)
    }

    private fun transformLinesToText() {
        var tempText = ""

        var firstLine = true
        for (line in listLine) {
            if (!firstLine) {
                tempText += "\n"
            } else firstLine = false

            tempText += line
        }

        text = tempText
        writeToFile()
    }


    fun addLine(textLine: String, position: Int) {
        listLine.add(position, textLine)
        transformLinesToText()
    }
    fun addLine(textLine: String) {
        listLine.add(textLine)
        transformLinesToText()
    }


    fun removeLine(position: Int) {
        listLine.removeAt(position)
        transformLinesToText()
    }
    fun removeLine(element: String) {
        listLine.remove(element)
        transformLinesToText()
    }


    fun changeLine(textLine: String, position: Int){
        listLine[position] = textLine
        transformLinesToText()
    }
    fun changeLine(textLine: String, element: String) {
        val index = listLine.indexOf(element)
        listLine[index] = textLine
        transformLinesToText()
    }


}