package com.example.pencil.document.page

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.text.TextUtils
import android.view.View
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.pencil.document.DrawView
import com.example.pencil.file.FileManager
import com.example.pencil.file.PencilFileXml

class GestionePagina(var context: Context, drawView: DrawView) {
    data class InfPath(var path: String, var paint: Paint, var rect: RectF)

    lateinit var page: Page
    var nPage = 0
    //private var pathList = mutableListOf<InfPath>()
    private var lastPath: InfPath = InfPath("", drawView.paint, RectF())
    lateinit var drawFile: PencilFileXml

    var widthPagePredefinito = 210
    var heightPagePredefinito = 297
    var risoluzionePagePredefinito = 300

    data class Page(var dimensioni: Dimensioni){
        //var background : Bitmap? = null
        var pathPenna = mutableListOf<InfPath>()
        var pathEvidenziatore = mutableListOf<InfPath>()
    }

    fun readFile(nomeFile: String, cartellaFile: String){
        drawFile = PencilFileXml(context, nomeFile, cartellaFile)
        drawFile.readXML()
    }
    fun changePage(index: Int){
        nPage = index
        readPage(nPage)
    }
    fun readPage(index: Int = nPage) {
        if(drawFile.body.lastIndex < index){
            drawFile.newPage(index, widthPagePredefinito, heightPagePredefinito, risoluzionePagePredefinito)
        }
        val pageTemp = drawFile.getPage(index)
        val dimensioni = Dimensioni(widthPagePredefinito.toFloat(), heightPagePredefinito.toFloat(), risoluzionePagePredefinito.toFloat())
        page = Page(dimensioni)

        fun stringToPaint(paintS: String): Paint {
            val paintList = TextUtils.split(paintS, "#").toList()
            val paintTemp = Paint().apply {
                color = paintList[0].toInt()
                // Smooths out edges of what is drawn without affecting shape.
                isAntiAlias = true
                // Dithering affects how colors with higher-precision than the device are down-sampled.
                isDither = true
                style = when (paintList[1]) {
                    "STROKE" -> Paint.Style.STROKE
                    "FILL" -> Paint.Style.FILL
                    else -> Paint.Style.FILL_AND_STROKE
                }
                strokeJoin = Paint.Join.ROUND // default: MITER
                strokeCap = Paint.Cap.ROUND // default: BUTT
                strokeWidth = paintList[2].toFloat()
            }
            return paintTemp
        }
        fun stringToRect(rectS: String): RectF {
            val rectList = TextUtils.split(rectS, "#").toList()
            val rectTemp = RectF().apply {
                left = rectList[0].toFloat()
                top = rectList[1].toFloat()
                right = rectList[2].toFloat()
                bottom = rectList[3].toFloat()
            }
            return rectTemp
        }
        for(elemento in pageTemp.pathPenna){
            val pathS = elemento["path"]
            val paintS = elemento["style"]
            val rectS = elemento["rect"]

            val paintTemp = stringToPaint(paintS!!)
            val rectTemp = stringToRect(rectS!!)

            val infPath = InfPath(pathS!!, paintTemp, rectTemp)
            page.pathPenna.add(infPath)
        }
        for(elemento in pageTemp.pathEvidenziatore){
            val pathS = elemento["path"]
            val paintS = elemento["style"]
            val rectS = elemento["rect"]

            val paintTemp = stringToPaint(paintS!!)
            val rectTemp = stringToRect(rectS!!)

            val infPath = InfPath(pathS!!, paintTemp, rectTemp)
            page.pathEvidenziatore.add(infPath)
        }

//        redraw = true
//        invalidate()
    }

    fun writePage(index: Int = nPage) {
        fun paintToString(paint: Paint): String {
            val paintS =
                paint.color.toString() + "#" +
                        when (paint.style) {
                            Paint.Style.STROKE -> "STROKE"
                            Paint.Style.FILL -> "FILL"
                            Paint.Style.FILL_AND_STROKE -> "FILL_AND_STROKE"
                        } + "#" +
                        paint.strokeWidth.toString()
            return paintS
        }
        fun rectToString(rect: RectF): String {
            val rectS = rect.left.toString() + "#" + rect.top.toString() + "#" + rect.right.toString() + "#" + rect.bottom.toString()
            return rectS
        }

        val pageTemp = PencilFileXml.Page(widthPagePredefinito, heightPagePredefinito, risoluzionePagePredefinito)
        for(elemento in page.pathPenna){
            val elementoMap = mutableMapOf<String, String>()
            elementoMap["path"] = elemento.path
            elementoMap["style"] = paintToString(elemento.paint)
            elementoMap["rect"] = rectToString(elemento.rect)

            pageTemp.pathPenna.add(elementoMap)
        }
        for(elemento in page.pathEvidenziatore){
            val elementoMap = mutableMapOf<String, String>()
            elementoMap["path"] = elemento.path
            elementoMap["style"] = paintToString(elemento.paint)
            elementoMap["rect"] = rectToString(elemento.rect)

            pageTemp.pathEvidenziatore.add(elementoMap)
        }

        drawFile.body[index].pathPenna = pageTemp.pathPenna
        drawFile.body[index].pathEvidenziatore = pageTemp.pathEvidenziatore
    }

}