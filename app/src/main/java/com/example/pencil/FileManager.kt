package com.example.pencil

import android.content.Context
import android.text.TextUtils.split
import java.io.File
import java.util.regex.Pattern
import android.util.Xml

import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.StringWriter
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException


class FileManager(context: Context, nomeFile: String) {
    // creo l'oggetto File(), lo apro e lo leggo
    var file: File = File(context.filesDir, nomeFile)
    var justCreated = false

    // variabili che ospitano il testo contenuto nel file
    var text = ""
        get() = field
        set(value) {
            field = value
            listLine = split(field, "\n").toMutableList()
            writeToFile()
        }
    private lateinit var listLine: MutableList<String>

    init {
        if (!file.exists()) {
            file.createNewFile()
            justCreated = true
        }

        if (file.extension == "txt") {
            readFromFile()
        }

        if(file.extension == "xml" && justCreated){
            //createXMLFile()
        }
    }


    // funzioni per la lettura e scrittura su file
    private fun readFromFile() {
        text = file.readText()
    }

    fun writeToFile() {
        file.writeText(text)
    }

    // funzioni che utilizzano la lista come metodo di modifica
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

    /*@Throws(
        IllegalArgumentException::class,
        IllegalStateException::class,
        IOException::class
    )*/
    fun createXMLFile() {
        val xmlSerializer = Xml.newSerializer()
        val writer = StringWriter()
        xmlSerializer.setOutput(writer)

        //Start Document
        xmlSerializer.startDocument("UTF-8", true)
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        xmlSerializer.startTag("", "data")

        xmlSerializer.text("")

        //End tag <file>
        xmlSerializer.endTag("", "data")
        xmlSerializer.endDocument()

        text = writer.toString()
    }


}