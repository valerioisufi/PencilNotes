package com.studiomath.pencilnotes.document

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.transform
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.dpToPx
import com.studiomath.pencilnotes.file.DrawViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val TAG = "DrawView"

/**
 * TODO: document your custom view class.
 */
@SuppressLint("ViewConstructor")
class DrawView(context: Context, val drawViewModel: DrawViewModel) : View(context) {

    /**
     * Funzioni per impostare il DrawView
     */


    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(drawViewModel.drawViewBitmap, 0f, 0f, null)


//        if (redrawOnDraw) {
//            canvas.drawBitmap(onDrawBitmap, 0f, 0f, null)
//
//        } else if (scalingOnDraw) {
//
//
//        } else if (changePageOnDraw) {
//
//
//        } else {
//            canvas.drawBitmap(onDrawBitmap, 0f, 0f, null)
//
//        }
//
//        if (makeCursoreOnDraw) {
////            makeCursore(canvas)
//        }
//
//        if (dragAndDropOnDraw) {
//            canvas.drawARGB(50, 255, 0, 0)
//            dragAndDropOnDraw = false
//        }

        /**
         * make lastPath
         */
//        val pageRect =
//            if (scalingOnDraw) scalingPageRect else if (::redrawPageRect.isInitialized) redrawPageRect else calcPageRect()
//
//        canvas.clipRect(pageRect)
//        if (drawLastPath) {
//            drawLastPathPaint.apply {
//                color = lastPath.paint.color
//
//                strokeWidth = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(
//                    lastPath.paint.strokeWidth,
//                    pageRect.width().toInt()
//                )
//            }
//
//            canvas.drawPath(stringToPath(lastPath.path), drawLastPathPaint)
////            val errorCalc = drawFile.body[pageAttuale].dimensioni.calcPxFromPt(0.01f, redrawPageRect.width().toInt())
////            canvas.drawPath(
////                stringToPath(pathFitCurve(lastPath.path, errorCalc)),
////                drawLastPathPaint
////            )
//        }
    }




    /**
     * onSizeChanged
     */
    private var widthView: Int = 0
    private var heightView: Int = 0

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        drawViewModel.onSizeChanged(width, height)

        drawViewModel.onDrawBitmapChanged = {
            // The ViewModel raises an event, do something here about it...
            invalidate()
        }
    }

}