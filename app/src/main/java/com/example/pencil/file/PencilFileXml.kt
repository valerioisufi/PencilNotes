package com.example.pencil.file

import android.content.Context
import android.util.Xml
import com.example.pencil.document.page.GestionePagina
import com.example.pencil.document.page.heightPagePredefinito
import com.example.pencil.document.page.widthPagePredefinito
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer

class PencilFileXml(context: Context, nomeFile: String, cartellaFile: String = "") {
    var fileManager = FileManager(context, nomeFile, cartellaFile)

    var head = mutableMapOf<String, MutableMap<String, String>>()
    var body = mutableListOf<GestionePagina>()


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

            if (parser.name == "head") {
                headReader(parser)
            } else if (parser.name == "body") {
                bodyReader(parser)
            }
        }
    }

    private fun headReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
            parser.nextTag()

            if (parser.name == "risorsa") {
                val risorsaDepht = parser.depth

                var id = parser.getAttributeValue(null, "id")
                val risorsaMap = mutableMapOf<String, String>()

                while (!(parser.depth == risorsaDepht && parser.eventType == XmlPullParser.END_TAG)) {
                    parser.nextTag()

                    if (parser.name == "path") {
                        risorsaMap[parser.name] = parser.nextText()
                    } else if (parser.name == "type") {
                        risorsaMap[parser.name] = parser.nextText()
                    }
                }

                head[id] = risorsaMap

            }
        }
    }

    private fun bodyReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
            parser.nextTag()

            if (parser.name == "page") {
                //val data_modifica = parser.getAttributeValue(null, "data_modifica")
                val widthMm = parser.getAttributeValue(null, "widthMm").toFloat()
                val heightMm = parser.getAttributeValue(null, "heightMm").toFloat()
                //val risoluzioneDpi = parser.getAttributeValue(null, "risoluzionePxInch").toInt()

                body.add(GestionePagina(widthMm, heightMm, body.lastIndex + 1))

                pageReader(parser)
            }

        }
    }

    private fun pageReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
            parser.nextTag()

            if (parser.name == "tracciati") {
                tracciatiReader(parser)
            } else if (parser.name == "images") {
                imagesReader(parser)

            } else if (parser.name == "background") {
                body.last().background = risorsaReader(parser)
            }

        }
    }

    private fun tracciatiReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
            parser.nextTag()

            if (parser.name == "elemento") {
                val elementoDepht = parser.depth
                val tracciato = GestionePagina.Tracciato(
                    GestionePagina.Tracciato.TypeTracciato.valueOf(
                        parser.getAttributeValue(
                            null,
                            "type"
                        )
                    )
                )

                while (!(parser.depth == elementoDepht && parser.eventType == XmlPullParser.END_TAG)) {
                    parser.nextTag()

                    if (parser.name == "path") {
                        tracciato.pathString = parser.nextText()
                    } else if (parser.name == "style") {
                        tracciato.paintString = parser.nextText()
                    } else if (parser.name == "rectVisualizzazione") {
                        tracciato.rectString = parser.nextText()
                    }
                }

                body.last().tracciati.add(tracciato)
            }

        }
    }
    private fun imagesReader(parser: XmlPullParser) {
        val startDepht = parser.depth
        while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
            parser.nextTag()

            if (parser.name == "elemento") {
                body.last().images.add(risorsaReader(parser))

            }

        }
    }

    private fun risorsaReader(parser: XmlPullParser): GestionePagina.Image {
        val startDepht = parser.depth
        val risorsaImage = GestionePagina.Image(
            GestionePagina.Image.TypeImage.valueOf(
                parser.getAttributeValue(
                    null,
                    "type"
                )
            )
        )

        if (risorsaImage.type == GestionePagina.Image.TypeImage.PDF) {
            while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
                parser.nextTag()

                if (parser.name == "id") {
                    risorsaImage.id = parser.nextText()
                } else if (parser.name == "index") {
                    risorsaImage.index = parser.nextText().toInt()
                }
            }

        } else if (risorsaImage.type == GestionePagina.Image.TypeImage.JPG || risorsaImage.type == GestionePagina.Image.TypeImage.PNG){

            while (!(parser.depth == startDepht && parser.eventType == XmlPullParser.END_TAG)) {
                parser.nextTag()

                if (parser.name == "id") {
                    risorsaImage.id = parser.nextText()

                } else if (parser.name == "rectVisualizzazione") {
                    risorsaImage.rectVisualizzazione = readRectXML(parser)

                } else if (parser.name == "rectRitaglio") {
                    risorsaImage.rectRitaglio = readRectXML(parser)

                }
            }
        }

        return risorsaImage
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

        headWriter(serializer)
        bodyWriter(serializer)

        //End tag <file>
        serializer.endTag("", "data")
        serializer.endDocument()
    }

    private fun headWriter(serializer: XmlSerializer) {
        serializer.startTag("", "head")

        for (idRisorsa in head.keys) {
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

    private fun bodyWriter(serializer: XmlSerializer) {
        serializer.startTag("", "body")

        for (page in body) {
            serializer.startTag("", "page")
            serializer.attribute(null, "widthMm", page.widthMm.toString())
            serializer.attribute(null, "heightMm", page.heightMm.toString())
            //serializer.attribute(null, "risoluzionePxInch", page.risoluzionePxInch.toString())

            if (page.background != null) {
                // background
                serializer.startTag("", "background")
                serializer.attribute(null, "type", page.background!!.type.name)

                serializer.startTag("", "id")
                serializer.text(page.background!!.id)
                serializer.endTag("", "id")

                serializer.startTag("", "index")
                serializer.text(page.background!!.index.toString())
                serializer.endTag("", "index")

                serializer.endTag("", "background")
            }

            /**
             * Tracciati
             */
            serializer.startTag("", "tracciati")
            for (elemento in page.tracciati) {
                serializer.startTag("", "elemento")
                serializer.attribute(null, "type", elemento.type.name)

                serializer.startTag("", "path")
                serializer.text(elemento.pathString)
                serializer.endTag("", "path")

                serializer.startTag("", "style")
                serializer.text(elemento.paintString)
                serializer.endTag("", "style")

                serializer.startTag("", "rectVisualizzazione")
                serializer.text(elemento.rectString)
                serializer.endTag("", "rectVisualizzazione")

                serializer.endTag("", "elemento")
            }
            serializer.endTag("", "tracciati")

            /**
             * Images
             */
            serializer.startTag("", "images")
            for (elemento in page.images) {
                serializer.startTag("", "elemento")
                serializer.attribute(null, "type", elemento.type.name)

                serializer.startTag("", "id")
                serializer.text(elemento.id)
                serializer.endTag("", "id")

                serializer.startTag("", "rectVisualizzazione")
                writeRectXML(serializer, elemento.rectVisualizzazione)
                serializer.endTag("", "rectVisualizzazione")

                serializer.startTag("", "rectRitaglio")
                writeRectXML(serializer, elemento.rectRitaglio)
                serializer.endTag("", "rectRitaglio")

                serializer.endTag("", "elemento")
            }
            serializer.endTag("", "images")

            /**
             * End
             */
            serializer.endTag("", "page")
        }

        serializer.endTag("", "body")
    }


    /**
     * Page function
     */
    fun getPage(index: Int): GestionePagina {
        if (body.lastIndex < index) {
            newPage(index, widthPagePredefinito, heightPagePredefinito)
        } else {
            body[index].tracciatiStringToObject()
        }

        return body[index]
    }

    // TODO: 25/12/2021 non so se la funzione seguente serva realmente
    fun setPage(index: Int, page: GestionePagina) {
        body[index] = page
    }

    // TODO: 25/12/2021 la funzione seguente è stata resa obsoleta in virtù della funzione getPage() 
    fun newPage(index: Int = body.lastIndex + 1, widthMm: Float, heightMm: Float) {
        body.add(index, GestionePagina(widthMm, heightMm, index))
    }

    fun preparePageIndex(index: Int){
        if (body.lastIndex < index) {
            newPage(index, widthPagePredefinito, heightPagePredefinito)
        } else {
            body[index].tracciatiStringToObject()
        }
    }

    fun getPageIndex(index: Int): Int {
        if (body.lastIndex < index) {
            newPage(index, widthPagePredefinito, heightPagePredefinito)
        } else {
            body[index].tracciatiStringToObject()
        }

        return index
    }
}