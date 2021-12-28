package com.example.pencil.document.tool

import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.example.pencil.R
import com.example.pencil.document.DrawView
import com.example.pencil.document.page.GestionePagina
import com.example.pencil.document.path.pathFitCurve
import com.example.pencil.document.path.polygonClippingAlgorithm
import com.example.pencil.document.path.stringToPath
import com.example.pencil.sharedPref

const val TAG = "StrumentoGomma"

class StrumentoGomma(var context: Context, var view: ImageView) {
    // variabili con i valori dell'oggetto, stroke (pt) e color
    var strokeWidth = sharedPref.getFloat("strokeGomma", 10f)
    var color = ResourcesCompat.getColor(view.resources, R.color.white, null)

    init {
        view.setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY)
    }


    var polygonA = mutableListOf<MutableList<Double>>()
    var polygonB = mutableListOf<MutableList<Double>>()
    var isPolygonA = true
    /**
     * Gestione del MotionEvent
     */
    private var path = ""
    private var currentX = 0f
    private var currentY = 0f

    fun gestioneMotionEvent(v: DrawView, event: MotionEvent){
        fun touchStart(v: DrawView, event: MotionEvent) {
            path = ""
            path = path + "M " + event.x + " " + event.y + " " //.moveTo(event.x, event.y)

            if(isPolygonA) polygonA.add(mutableListOf(event.x.toDouble(), event.y.toDouble()))
            else polygonB.add(mutableListOf(event.x.toDouble(), event.y.toDouble()))

            currentX = event.x
            currentY = event.y

            var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
            var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)

            newPath(v, path, getPaint())
        }
        fun touchMove(v: DrawView, event: MotionEvent) {
            path = path + "L " + event.x + " " + event.y + " "

            if(isPolygonA) polygonA.add(mutableListOf(event.x.toDouble(), event.y.toDouble()))
            else polygonB.add(mutableListOf(event.x.toDouble(), event.y.toDouble()))

            currentX = event.x
            currentY = event.y

            // Draw the path in the extra bitmap to cache it.
            rewritePath(v, path)
        }
        fun touchUp(v: DrawView, event: MotionEvent) {
            path += "Z"

            if(isPolygonA)polygonA.add(mutableListOf(polygonA[0][0], polygonA[0][1]))
            else {
                polygonB.add(mutableListOf(polygonB[0][0], polygonB[0][1]))

                var polygonList = polygonClippingAlgorithm(polygonA, polygonB, false, true)
                var path = ""
                for (polygon in polygonList){
                    var firstPoint = true
                    for (point in polygon) {
                        if (firstPoint) {
                            path = "M " + point[0] + " " + point[1] + " "
                            firstPoint = false
                        }else{
                            path = path + "L " + point[0] + " " + point[1] + " "
                        }
                    }
                    path += "Z"
                    savePath(v, path, getPaint())
                }
            }

            // Reset the path so it doesn't get drawn again.
            path = ""

            isPolygonA = false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart(v, event)
            MotionEvent.ACTION_MOVE -> touchMove(v, event)
            MotionEvent.ACTION_UP -> touchUp(v, event)
        }
    }

    /**
     * Gestione dei drawEvent
     */
    fun newPath(v: DrawView, path: String, paint: Paint/*, type: String = "Penna"*/) {
        v.lastPath = DrawView.InfPath(path, paint, v.pageRect)
        v.drawLastPath = true
    }

    fun rewritePath(v: DrawView, path: String) {
        v.lastPath.path = path

        v.invalidate()
    }

    fun savePath(v: DrawView, path: String, paint: Paint, type: GestionePagina.Tracciato.TypeTracciato = GestionePagina.Tracciato.TypeTracciato.PENNA) {
        var errorCalc = v.drawFile.body[v.pageAttuale].dimensioni.calcSpessore(v.maxError.toFloat(), v.pageRect.width().toInt())
        v.lastPath.path = pathFitCurve(path, errorCalc)
        v.lastPath.paint = paint

        var tracciato = GestionePagina.Tracciato(type).apply {
            pathString = v.lastPath.path
            paintObject = v.lastPath.paint
            rectObject = v.lastPath.rect
        }
        v.drawFile.body[v.pageAttuale].tracciati.add(tracciato)
        v.drawFile.body[v.pageAttuale].tracciati.last().objectToString()

        v.drawLastPath = false
        var paint = Paint(v.lastPath.paint)
        paint.strokeWidth = v.drawFile.body[v.pageAttuale].dimensioni.calcSpessore(v.lastPath.paint.strokeWidth, v.pageRect.width().toInt()).toFloat()

        //var pathTemp = pathFitCurve(lastPath.path, maxError)
        v.pageCanvas.drawPath(stringToPath(v.lastPath.path), paint)
        v.invalidate()

        v.drawFile.writeXML()
    }


    fun getPaint(): Paint {
        val paintTemp = Paint().apply {
            color = ResourcesCompat.getColor(view.resources, R.color.colorEvidenziatore, null)
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.FILL // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
        }
        return paintTemp
    }
}