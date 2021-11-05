package com.example.pencil

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class PencilFileXml(context: Context, nomeFile: String) {
    private var file = FileManager(context, nomeFile)

    fun readXML(){
        val inputStream = file.file.inputStream()

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        parser.nextTag()
    }


}