package com.studiomath.innernotes.document.tool

import android.content.Context
import android.graphics.Paint
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.studiomath.innernotes.R
import com.studiomath.innernotes.document.DrawView
import com.studiomath.innernotes.document.page.GestionePagina
import com.studiomath.innernotes.document.path.pathFitCurve
import com.studiomath.innernotes.dpToPx
import com.studiomath.innernotes.sharedPref
import kotlin.math.abs

class StrumentoGomma(var context: Context, var view: ImageView) {
    // variabili con i valori dell'oggetto, stroke (pt) e color
    var strokeWidthStrumento = sharedPref.getFloat("strokeGomma", 10f)
    var colorStrumento = sharedPref.getInt(
        "colorGomma",
        ResourcesCompat.getColor(view.resources, R.color.white, null)
    )

    init {
        view.setColorFilter(colorStrumento, android.graphics.PorterDuff.Mode.MULTIPLY)
    }

    /**
     * Gestione del MotionEvent
     */

    private var path = ""
    private var currentX = 0f
    private var currentY = 0f
    fun gestioneMotionEvent(v: DrawView, event: MotionEvent) {
        fun touchStart(v: DrawView, event: MotionEvent) {
            path = ""
            path += "M ${event.x} ${event.y} "

            var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
            var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)

            newPath(v, path, getPaint())
        }

        fun touchMove(v: DrawView, event: MotionEvent) {
            path += "L ${event.x} ${event.y} "

            currentX = event.x
            currentY = event.y

            // Draw the path in the extra bitmap to cache it.
            rewritePath(v, path)
        }

        fun touchUp(v: DrawView, event: MotionEvent) {
            savePath(v, path, getPaint())

            // Reset the path so it doesn't get drawn again.
            path = ""
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
        v.lastPath = DrawView.InfPath(path, paint, v.redrawPageRect)
        v.drawLastPath = true
    }

    fun rewritePath(v: DrawView, path: String) {
        v.lastPath.path = path

        v.draw(false, false)
    }

    fun savePath(
        v: DrawView,
        path: String,
        paint: Paint,
        type: GestionePagina.Tracciato.TypeTracciato = GestionePagina.Tracciato.TypeTracciato.GOMMA
    ) {
        var errorCalc = v.drawFile.body[v.pageAttuale].dimensioni.calcPxFromPt(
            v.maxError.toFloat(),
            v.redrawPageRect.width().toInt()
        )
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

        v.makeSingleTracciato(v.lastPath.path, v.lastPath.paint)
        v.draw(false, false)

        v.drawFile.writeXML()
    }


    fun drawStrumento() {

    }

    fun getPaint(): Paint {
        val paintTemp = Paint().apply {
            color = colorStrumento
            // Smooths out edges of what is drawn without affecting shape.
            isAntiAlias = true
            // Dithering affects how colors with higher-precision than the device are down-sampled.
            isDither = true
            style = Paint.Style.STROKE // default: FILL
            strokeJoin = Paint.Join.ROUND // default: MITER
            strokeCap = Paint.Cap.ROUND // default: BUTT
            strokeWidth = strokeWidthStrumento
        }
        return paintTemp
    }
}