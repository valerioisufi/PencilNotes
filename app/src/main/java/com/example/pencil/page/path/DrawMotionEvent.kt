package com.example.pencil.page.path

import android.graphics.Paint
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import com.example.pencil.DrawActivity
import com.example.pencil.R

class DrawMotionEvent {

}

private var path : String = ""

//private var motionTouchEventX = 0f
//private var motionTouchEventY = 0f

private var currentX = 0f
private var currentY = 0f

private fun touchStart(event: MotionEvent) {
    //path.reset()
    path = ""
    path = path + "M " + event.x + " " + event.y + " " //.moveTo(event.x, event.y)

    currentX = event.x
    currentY = event.y

    var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
    var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)

    if(tilt > 0.8f && orientation > -0.3f && orientation < 0.5f){
        paint = Paint(paintEvidenziatore)
        pennello = DrawActivity.Pennello.EVIDENZIATORE
    } else if(tilt > 0.2f && (orientation > 2.5f || orientation < -2.3f)){
        paint = Paint(paintAreaSelezione)
        pennello = DrawActivity.Pennello.AREA_SELEZIONE
    } else{
        pennello = pennelloAttivo
        when(pennelloAttivo){
            DrawActivity.Pennello.MATITA -> paint = Paint(paintMatita)
            DrawActivity.Pennello.PENNA -> paint = Paint(paintPenna)
            DrawActivity.Pennello.EVIDENZIATORE -> paint = Paint(paintEvidenziatore)
            DrawActivity.Pennello.GOMMA -> paint = Paint(paintGomma)
        }
    }

    drawView.newPath(path, paint)
}

private fun touchMove(event: MotionEvent) {
    // QuadTo() adds a quadratic bezier from the last point,
    // approaching control point (x1,y1), and ending at (x2,y2).
    /*textViewData.text =
        event.x.toString() + '\n' + event.y.toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_DISTANCE)
            .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_TILT)
            .toString() + '\n' + event.getAxisValue(MotionEvent.AXIS_ORIENTATION).toString()*/
    //path = path + "Q " + currentX + " " + currentY + " " + (event.x + currentX) / 2 + " " + (event.y + currentY) / 2 + " " //.quadTo(currentX, currentY, (event.x + currentX) / 2, (event.y + currentY) / 2)
    path = path + "L " + event.x + " " + event.y + " "

    currentX = event.x
    currentY = event.y

    // Draw the path in the extra bitmap to cache it.
    drawView.rewritePath(path)
    //drawView.setPathPaint(path, paint)
}

private fun touchUp(event: MotionEvent) {
    //var tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
    //var orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)


    if(pennello == DrawActivity.Pennello.AREA_SELEZIONE){
        paint.color = ResourcesCompat.getColor(resources, R.color.colorAreaDefinitiva, null)
        paint.style = Paint.Style.FILL

        drawView.savePath(path,paint)
    }else{
        drawView.savePath(path, paint)
    }
    // Reset the path so it doesn't get drawn again.
    path = ""
}

private fun hoverMove(event: MotionEvent) {
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

private fun hoverStart(event: MotionEvent) {
    /*paint.color = ResourcesCompat.getColor(resources, R.color.colorEvidenziatore, null)
    paint.strokeWidth = 40f

    path.reset()
    path.moveTo(event.x, event.y)
    currentX = event.x
    currentY = event.y*/
}

private fun hoverUp(event: MotionEvent) {
    /*drawView.savePath(path)
    path.reset()*/
}

// Paint Object
private lateinit var paint : Paint
private var pennello = Pennello.MATITA
private var pennelloAttivo = Pennello.MATITA
private lateinit var paintMatita : Paint
private lateinit var paintPenna: Paint
private lateinit var paintEvidenziatore : Paint
private lateinit var paintGomma : Paint
private lateinit var paintAreaSelezione : Paint

enum class Pennello {
    MATITA,
    PENNA,
    EVIDENZIATORE,
    GOMMA,

    AREA_SELEZIONE
}