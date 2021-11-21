package com.example.pencil

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer

class PencilFileXml(context: Context, nomeFile: String, cartellaFile: String = "") {
    var fileManager = FileManager(context, nomeFile, cartellaFile)

    //var data : MutableMap<String, String> = mutableMapOf()
    data class Page(var widthMm: Int = 0, var heightMm: Int = 0, var risoluzionePxInch: Int = 0){

        var background : MutableMap<String, String>? = null
        var pathPenna = mutableListOf<MutableMap<String, String>>()
        var pathEvidenziatore = mutableListOf<MutableMap<String, String>>()
    }

    var head = mutableMapOf<String, MutableMap<String, String>>()
    var body = mutableListOf<Page>()


    /**
     * Funzione per la lettura dei file XML
     */
    fun readXML(){
        if(fileManager.justCreated){
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
    private fun dataReader(parser: XmlPullParser){
        while(!(parser.depth == 1 && parser.eventType == XmlPullParser.END_TAG)){
            // start_tag data e tag successivi
            parser.nextTag()

            if(parser.name == "head"){
                headReader(parser)
            } else if(parser.name == "body"){
                bodyReader(parser)
            }
        }
    }

    private fun headReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while(!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)){
            parser.nextTag()

            if(parser.name == "risorsa"){
                val risorsaDepht = parser.depth

                var id = parser.getAttributeValue(null, "id")
                val risorsaMap = mutableMapOf<String, String>()

                while(!(parser.depth == risorsaDepht && parser.eventType == XmlPullParser.END_TAG)){
                    parser.nextTag()

                    if(parser.name == "path"){
                        risorsaMap[parser.name] = parser.nextText()
                    }else if(parser.name == "type"){
                        risorsaMap[parser.name] = parser.nextText()
                    }
                }

                head[id] = risorsaMap

            }
        }
    }
    private fun bodyReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while(!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)){
            parser.nextTag()

            if(parser.name == "page"){
                //val data_modifica = parser.getAttributeValue(null, "data_modifica")
                val widthMm = parser.getAttributeValue(null, "widthMm").toInt()
                val heightMm = parser.getAttributeValue(null, "heightMm").toInt()
                val risoluzioneDpi = parser.getAttributeValue(null, "risoluzionePxInch").toInt()

                body.add(Page(widthMm, heightMm, risoluzioneDpi))

                pageReader(parser)
            }

        }
    }

    private fun pageReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while(!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)){
            parser.nextTag()

            if(parser.name == "path_penna"){
                pathReader(parser, parser.name)
            }else if(parser.name == "path_evidenziatore"){
                pathReader(parser, parser.name)
            }else if(parser.name == "background"){
                body.last().background = risorsaReader(parser)
            }

        }
    }

    private fun pathReader(parser: XmlPullParser, type: String) {
        val startDepht = parser.depth
        while(!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)){
            parser.nextTag()

            if(parser.name == "elemento"){
                val elementoDepht = parser.depth
                val elementoMap = mutableMapOf<String, String>()

                while(!(parser.depth == elementoDepht && parser.eventType == XmlPullParser.END_TAG)){
                    parser.nextTag()

                    if(parser.name == "path"){
                        elementoMap[parser.name] = parser.nextText()
                    }else if(parser.name == "style"){
                        elementoMap[parser.name] = parser.nextText()
                    }else if(parser.name == "rect"){
                        elementoMap[parser.name] = parser.nextText()
                    }
                }

                if(type == "path_penna"){
                    body.last().pathPenna.add(elementoMap)
                }else if(type == "path_evidenziatore"){
                    body.last().pathEvidenziatore.add(elementoMap)
                }
            }

        }
    }

    private fun risorsaReader(parser: XmlPullParser): MutableMap<String, String> {
        val startDepht = parser.depth
        val risorsaMap = mutableMapOf<String, String>()

        while(!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)){
            parser.nextTag()

            if(parser.name == "id"){
                risorsaMap[parser.name] = parser.nextText()
            }else if(parser.name == "index"){
                risorsaMap[parser.name] = parser.nextText()
            }
        }

        return risorsaMap
    }


    /**
     * Funzione per la scrittura dei file XML
     */
    fun writeXML(){
        val outputStreamWriter = fileManager.file.writer()

        val serializer = Xml.newSerializer()
        serializer.setOutput(outputStreamWriter)

        //Start Document
        serializer.startDocument("UTF-8", true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        serializer.startTag("", "data")

        headWriter(serializer)
        bodyWriter(serializer)

        //End tag <file>
        serializer.endTag("", "data")
        serializer.endDocument()
    }

    private fun headWriter(serializer: XmlSerializer){
        serializer.startTag("", "head")

        for(idRisorsa in head.keys){
            serializer.startTag("", "risorsa")
            serializer.attribute(null, "id", idRisorsa)

            serializer.startTag("", "path")
            serializer.text(head[idRisorsa]?.get("path"))
            serializer.endTag("", "path")

            serializer.startTag("", "type")
            serializer.text(head[idRisorsa]?.get("type"))
            serializer.endTag("", "type")

            serializer.endTag("", "risorsa")
        }

        serializer.endTag("", "head")
    }
    private fun bodyWriter(serializer: XmlSerializer){
        serializer.startTag("", "body")

        for(page in body){
            serializer.startTag("", "page")
            serializer.attribute(null, "widthMm", page.widthMm.toString())
            serializer.attribute(null, "heightMm", page.heightMm.toString())
            serializer.attribute(null, "risoluzionePxInch", page.risoluzionePxInch.toString())

            if(page.background != null){
                // background
                serializer.startTag("", "background")
                    serializer.startTag("", "id")
                    serializer.text(page.background!!["id"])
                    serializer.endTag("", "id")

                    serializer.startTag("", "index")
                    serializer.text(page.background!!["index"])
                    serializer.endTag("", "index")
                serializer.endTag("", "background")
            }

            // path_penna
            serializer.startTag("", "path_penna")
            for(elemento in page.pathPenna){
                serializer.startTag("", "elemento")

                serializer.startTag("", "path")
                serializer.text(elemento["path"])
                serializer.endTag("", "path")

                serializer.startTag("", "style")
                serializer.text(elemento["style"])
                serializer.endTag("", "style")

                serializer.startTag("", "rect")
                serializer.text(elemento["rect"])
                serializer.endTag("", "rect")

                serializer.endTag("", "elemento")
            }
            serializer.endTag("", "path_penna")

            // path_evidenziatore
            serializer.startTag("", "path_evidenziatore")
            for(elemento in page.pathEvidenziatore){
                serializer.startTag("", "elemento")

                serializer.startTag("", "path")
                serializer.text(elemento["path"])
                serializer.endTag("", "path")

                serializer.startTag("", "style")
                serializer.text(elemento["style"])
                serializer.endTag("", "style")

                serializer.startTag("", "rect")
                serializer.text(elemento["rect"])
                serializer.endTag("", "rect")

                serializer.endTag("", "elemento")
            }
            serializer.endTag("", "path_evidenziatore")


            serializer.endTag("", "page")
        }

        serializer.endTag("", "body")
    }


    /**
     * Page function
     */
    fun getPage(index: Int): Page {
        return body[index]
    }
    fun setPage(index: Int, page : Page){
        body[index] = page
    }
    fun newPage(index: Int = body.lastIndex + 1, widthMm: Int, heightMm: Int, risoluzionePxInch: Int){
        body.add(index, Page(widthMm, heightMm, risoluzionePxInch))
    }
}