package com.example.pencil.document.page

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.content.res.ResourcesCompat
import com.example.pencil.R
import com.example.pencil.dpToPx

class RigaturaQuadrettatura(val context: Context, var type: Type = Type.Bianco) {
    enum class Type{
        Bianco, Rigatura1R
    }

    fun makeRigaturaQuadrettatura(canvas: Canvas, dimensioni: Dimensioni, rect: RectF){
        var paintLinea = Paint().apply {
            color = ResourcesCompat.getColor(context.resources, R.color.riga, null)
            isAntiAlias = true
            isDither = true
            strokeWidth = dimensioni.calcPxFromMm(
                0.2f,
                rect.width().toInt()
            )
        }
        var padding = dimensioni.calcPxFromMm(12f, rect.width().toInt())

        when(type){
            Type.Bianco -> {}
            Type.Rigatura1R -> {
                var pointLeft = PointF(
                    rect.left + padding,
                    rect.top + dimensioni.calcPxFromMm(31f, rect.width().toInt()),
                )
                var pointRight = PointF(
                    rect.right - padding,
                    pointLeft.y
                )

                // first line
                canvas.drawLine(
                    pointLeft.x, pointLeft.y,
                    pointRight.x, pointRight.y,
                    paintLinea
                )

                // other 30 lines
                for (i in 1..30){
                    pointLeft.y += dimensioni.calcPxFromMm(8f, rect.width().toInt())
                    pointRight.y = pointLeft.y

                    canvas.drawLine(
                        pointLeft.x, pointLeft.y,
                        pointRight.x, pointRight.y,
                        paintLinea
                    )
                }
            }


        }
    }
}