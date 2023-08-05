package com.studiomath.pencilnotes.file

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer

private const val TAG = "FileFolderXml"
class FileFolderXml(context: Context, nomeFile: String, cartellaFile: String = "") {
    var fileManager = FileManager(context, nomeFile, cartellaFile)

    /**
     * ["directory"][pos elemento: Int]["attributo"]
     */
    var data = mutableMapOf<String, MutableList<MutableMap<String, String>>>()


    /**
     * Funzione per la lettura dei file XML
     */
    fun readXML() {
        if (fileManager.justCreated) {
            writeXML()
        }

        val inputStream = fileManager.file.inputStream()

        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        dataReader(parser)
    }

    /**
     * Ogni volta che in una funzione raggiungo un start_tag
     * e quindi richiamo un'altra funzione, mi assicuro che nella funzione
     * chiamata raggiungo sempre l'end_tag corrispettivo
     */
    private fun dataReader(parser: XmlPullParser) {
        while (!(parser.depth == 1 && parser.eventType == XmlPullParser.END_TAG)) {
            // start_tag data e tag successivi
            parser.nextTag()

            fun funReader(parser: XmlPullParser, root: String) {
                val startDepht = parser.depth

                while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
                    parser.nextTag()

                    if (parser.name == "file" && parser.eventType == XmlPullParser.START_TAG) {
                        val fileAttribute = mutableMapOf<String, String>()
                        fileAttribute["nome"] = parser.getAttributeValue(null, "nome")
                        fileAttribute["type"] = "file"

                        data[root]!!.add(fileAttribute)

                    } else if (parser.name == "folder" && parser.eventType == XmlPullParser.START_TAG) {
                        val fileAttribute = mutableMapOf<String, String>()
                        fileAttribute["nome"] = parser.getAttributeValue(null, "nome")
                        fileAttribute["type"] = "folder"

                        data[root]!!.add(fileAttribute)

                        data["$root${parser.getAttributeValue(null, "nome")}/"] = mutableListOf()
                        funReader(parser, "$root${parser.getAttributeValue(null, "nome")}/")

                    }
                }
            }

            data["/"] = mutableListOf()
            funReader(parser, "/")

        }
    }


    /**
     * Funzione per la scrittura dei file XML
     */
    fun writeXML() {
        val outputStreamWriter = fileManager.file.writer()

        val serializer = Xml.newSerializer()
        serializer.setOutput(outputStreamWriter)

        //Start Document
        serializer.startDocument("UTF-8", true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        serializer.startTag("", "data")

        fun funWriter(serializer: XmlSerializer, root: String) {

            for (element in data[root]!!) {
                if (element["type"] == "file") {
                    serializer.startTag("", "file")
                    serializer.attribute(null, "nome", element["nome"])
                    serializer.endTag("", "file")

                } else if (element["type"] == "folder") {
                    serializer.startTag("", "folder")
                    serializer.attribute(null, "nome", element["nome"])

                    funWriter(serializer, "$root${element["nome"]}/")

                    serializer.endTag("", "folder")

                }

            }
        }

        if (!data["/"].isNullOrEmpty()) {
            funWriter(serializer, "/")
        }

        //End tag <file>
        serializer.endTag("", "data")
        serializer.endDocument()
    }

}