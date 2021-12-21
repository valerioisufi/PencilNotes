package com.example.pencil.document.path

import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.pencil.DrawActivity
import com.example.pencil.R
import com.example.pencil.document.DrawView
import com.example.pencil.drawImpostazioni

class DrawMotionEvent(var context: Context, var drawView: DrawView){
    private var path : String = ""
    private var continueScaleTranslate = false

    private var currentX = 0f
    private var currentY = 0f

    //var sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    //var modePenna = sharedPref.getBoolean(context.getString(R.string.mode_penna), true)

    fun onTouchView(v: DrawView, event: MotionEvent) {
        for (i in 0..event.pointerCount - 1) {
            if (event.getToolType(i) == MotionEvent.TOOL_TYPE_STYLUS) {

                val tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
                val orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)

                if(tilt > 0.8f && orientation > -0.3f && orientation < 0.5f){
                    //paint = drawStrumento.getPaint()
                    drawView.strumentoAttivo = DrawView.Pennello.EVIDENZIATORE
                } else if(tilt > 0.2f && (orientation > 2.5f || orientation < -2.3f)){
                    //paint = Paint(paintAreaSelezione)
                    drawView.strumentoAttivo = DrawView.Pennello.LAZO
                }

                when(drawView.strumentoAttivo){
                    DrawView.Pennello.PENNA -> drawView.strumentoPenna?.gestioneMotionEvent(v, event)
                    DrawView.Pennello.EVIDENZIATORE -> drawView.strumentoEvidenziatore?.gestioneMotionEvent(v, event)
                    DrawView.Pennello.GOMMA -> drawView.strumentoGomma?.gestioneMotionEvent(v, event)
                }

            }
        }

        if (!drawImpostazioni.modePenna && event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER){
            if(!continueScaleTranslate) {
                when(drawView.strumentoAttivo){
                    DrawView.Pennello.PENNA -> drawView.strumentoPenna?.gestioneMotionEvent(v, event)
                    DrawView.Pennello.EVIDENZIATORE -> drawView.strumentoEvidenziatore?.gestioneMotionEvent(v, event)
                    DrawView.Pennello.GOMMA -> drawView.strumentoGomma?.gestioneMotionEvent(v, event)
                }
            } else {
                when (event.action) {
                    MotionEvent.ACTION_UP -> continueScaleTranslate = false
                    // TODO: 04/12/2021 MotionEvent.ACTION_CANCEL
                }
            }
        }

        if (event.pointerCount == 2){
            v.scaleTranslate(event)
            if(!drawImpostazioni.modePenna) {
                v.rewritePath("")
            }
            continueScaleTranslate = true
        }
    }

    fun onHoverView(v: DrawView, event: MotionEvent){
        when (event.action) {
            MotionEvent.ACTION_HOVER_ENTER -> hoverStart(v, event)
            MotionEvent.ACTION_HOVER_MOVE -> hoverMove(v, event)
            MotionEvent.ACTION_HOVER_EXIT -> hoverUp(v, event)
        }
    }





    private fun hoverMove(v: DrawView, event: MotionEvent) {
        /*textViewData.text =
            event.x.toString() + '\n' + event.y.toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_DISTANCE)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_TILT)
                .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_ORIENTATION).toString()*/

        /*if(event.getAxisValue(MotionEvent.AXIS_TILT) > 1.3f && event.getAxisValue(MotionEvent.AXIS_ORIENTATION) > -0.5f && event.getAxisValue(MotionEvent.AXIS_ORIENTATION) < 0.5f){
            path.quadTo(currentX, currentY, (event.x + currentX) / 2, (event.y + currentY) / 2)
            currentX = event.x
            currentY = event.y

            // Draw the path in the extra bitmap to cache it.
            drawView.setPath(path)
        }*/
    }

    private fun hoverStart(v: DrawView, event: MotionEvent) {
        /*paint.color = ResourcesCompat.getColor(resources, R.color.colorEvidenziatore, null)
        paint.strokeWidth = 40f

        path.reset()
        path.moveTo(event.x, event.y)
        currentX = event.x
        currentY = event.y*/
    }

    private fun hoverUp(v: DrawView, event: MotionEvent) {
        /*drawView.savePath(path)
        path.reset()*/
    }

}