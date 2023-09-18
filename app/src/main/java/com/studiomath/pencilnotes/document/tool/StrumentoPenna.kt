package com.studiomath.pencilnotes.document.tool

import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.customView.ColorWheel
import com.studiomath.pencilnotes.document.DrawView
import com.studiomath.pencilnotes.document.path.pathFitCurve

class StrumentoPenna(var context: Context, var view: ImageView){
//    // variabili con i valori dell'oggetto, stroke (pt) e color
//    var strokeWidthStrumento = sharedPref.getFloat("strokePenna", 2.5f)
//    var colorStrumento = sharedPref.getInt("colorPenna", ResourcesCompat.getColor(view.resources, R.color.strumento_penna, null))
//
//    init {
//        view.setColorFilter(colorStrumento, android.graphics.PorterDuff.Mode.MULTIPLY)
//
//        view.setOnLongClickListener {
//            strumentiDialog()
//            return@setOnLongClickListener true
//        }
//    }
//
//
//    fun getPaint(): Paint {
//        val paintTemp = Paint().apply {
//            color = colorStrumento
//            // Smooths out edges of what is drawn without affecting shape.
//            isAntiAlias = true
//            // Dithering affects how colors with higher-precision than the device are down-sampled.
//            isDither = true
//            style = Paint.Style.STROKE // default: FILL
//            strokeJoin = Paint.Join.ROUND // default: MITER
//            strokeCap = Paint.Cap.ROUND // default: BUTT
//            strokeWidth = strokeWidthStrumento
//        }
//        return paintTemp
//    }
//
//    fun strumentiDialog() {
//        var dialog = Dialog(context)
//        dialog.setContentView(R.layout.dialog_draw_paint)
//
//        var window = dialog.window!!
//        window.setBackgroundDrawableResource(android.R.color.transparent)
//        window.setGravity(Gravity.CENTER)
//        window.attributes.windowAnimations = R.style.DialogAnimation
//
//        dialog.setCancelable(true)
//        window.setLayout(
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )
//
//        val coloreTrattoColorPickerView = dialog.findViewById<ColorWheel>(R.id.dialogDrawPaint_coloreTrattoColorPickerView)
//        val dimensioneTrattoSeekbar = dialog.findViewById<SeekBar>(R.id.dialogDrawPaint_dimensioneTrattoSeekbar)
//        val dimensioneTrattoTextView = dialog.findViewById<TextView>(R.id.dialogDrawPaint_dimensioneTrattoTextView)
//
//        coloreTrattoColorPickerView.color = colorStrumento
//        dimensioneTrattoSeekbar.progress = (strokeWidthStrumento * 10).toInt()
//        dimensioneTrattoTextView.text = (strokeWidthStrumento.toString() + "pt")
//
//        dialog.show()
//
//        dimensioneTrattoSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
//                var progressTemp = (progress * 0.1).toFloat()
//                strokeWidthStrumento = progressTemp
//
//                dimensioneTrattoTextView.text = (progressTemp.toString() + "pt")
//
//                with (sharedPref.edit()) {
//                    putFloat("strokePenna", strokeWidthStrumento)
//                    apply()
//                }
//            }
//            override fun onStartTrackingTouch(seek: SeekBar) {}
//            override fun onStopTrackingTouch(seek: SeekBar) {
//            }
//        })
//
//        coloreTrattoColorPickerView.setOnColorChangedListener(object : ColorWheel.OnColorChangedListener{
//            override fun onColorChanged(newColor: Int) {
//                //colorShowView.color = newColor
//                view.setColorFilter(newColor, android.graphics.PorterDuff.Mode.MULTIPLY)
//                colorStrumento = newColor
//
//                with (sharedPref.edit()) {
//                    putInt("colorPenna", newColor)
//                    apply()
//                }
//            }
//        })
//    }
//
//
//    /**
//     * Funzionalità penna
//     */
//
//    fun drawMotionTracciato(v: DrawView){
//
//    }
//
//
//    /**
//     * Gestione del MotionEvent
//     */
//    private var path = ""
//    private var currentX = 0f
//    private var currentY = 0f
//    fun gestioneMotionEvent(v: DrawView, event: MotionEvent){
//        fun touchStart(v: DrawView, event: MotionEvent) {
//            path = ""
//            path += "M ${event.x} ${event.y} "
//
//            currentX = event.x
//            currentY = event.y
//
//            var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
//            var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
//
//            newPath(v, path, getPaint())
//        }
//        fun touchMove(v: DrawView, event: MotionEvent) {
//            path += "L ${event.x} ${event.y} "
//
//            currentX = event.x
//            currentY = event.y
//
//            // Draw the path in the extra bitmap to cache it.
//            rewritePath(v, path)
//        }
//        fun touchUp(v: DrawView, event: MotionEvent) {
//            savePath(v, path, getPaint())
//
//            // Reset the path so it doesn't get drawn again.
//            path = ""
//        }
//
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> touchStart(v, event)
//            MotionEvent.ACTION_MOVE -> touchMove(v, event)
//            MotionEvent.ACTION_UP -> touchUp(v, event)
//        }
//    }
//
//    /**
//     * Gestione dei drawEvent
//     */
//    fun newPath(v: DrawView, path: String, paint: Paint/*, type: String = "Penna"*/) {
//        v.lastPath = DrawView.InfPath(path, paint, v.redrawPageRect)
//        v.drawLastPath = true
//    }
//
//    fun rewritePath(v: DrawView, path: String) {
//        v.lastPath.path = path
//
//        v.draw(false, false)
//    }
//
//    fun savePath(v: DrawView, path: String, paint: Paint, type: GestionePagina.Tracciato.TypeTracciato = GestionePagina.Tracciato.TypeTracciato.PENNA) {
//        var errorCalc = v.drawFile.body[v.pageAttuale].dimensioni.calcPxFromPt(v.maxError.toFloat(), v.redrawPageRect.width().toInt())
//        v.lastPath.path = pathFitCurve(path, errorCalc)
//        v.lastPath.paint = paint
//
//        var tracciato = GestionePagina.Tracciato(type).apply {
//            pathString = v.lastPath.path
//            paintObject = v.lastPath.paint
//            rectObject = v.lastPath.rect
//        }
//        v.drawFile.body[v.pageAttuale].tracciati.add(tracciato)
//        v.drawFile.body[v.pageAttuale].tracciati.last().objectToString()
//
//        v.drawLastPath = false
//
//        //var pathTemp = pathFitCurve(lastPath.path, maxError)
//        v.makeSingleTracciato(v.lastPath.path, v.lastPath.paint)
//        v.draw(false, false)
//
//        v.drawFile.writeXML()
//    }
}