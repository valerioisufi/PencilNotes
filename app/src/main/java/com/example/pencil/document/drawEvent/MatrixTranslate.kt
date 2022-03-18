package com.example.pencil.document.drawEvent

import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import com.example.pencil.document.DrawView

/**
 * funzione che si occupa dello scale e dello spostamento
 */
// TODO: 23/01/2022 sarebbe il caso di avviare lo scale solo
//  dopo che sia stato rilevato un movimento significativo
private var startMatrix = Matrix()
private var moveMatrix = Matrix()


private val FIRST_POINTER_INDEX = 0

private var downPos = PointF()
private var movePos = PointF()

private var translate = PointF(0f, 0f)


fun matrixTranslate(event: MotionEvent, drawView: DrawView) {
    /**
     * Matrix()
     * https://i-rant.arnaudbos.com/matrices-for-developers/
     * https://i-rant.arnaudbos.com/2d-transformations-android-java/
     */
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            downPos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))

            startMatrix = Matrix(drawView.drawFile.body[drawView.pageAttuale].matrixPage)
            drawView.drawLastPath = false
        }
        MotionEvent.ACTION_MOVE -> {
            movePos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
            translate = PointF(movePos.x - downPos.x, movePos.y - downPos.y)

            moveMatrix = Matrix(startMatrix)

            val f = FloatArray(9)
            moveMatrix.getValues(f)

            /**
             * translate max/min
             */
            val pageRectNow = drawView.calcPageRect(matrix = moveMatrix)
            val pageRectModel = drawView.calcPageRect(matrix = Matrix(), paddingDp = 8)

            if (pageRectNow.left + translate.x >= pageRectModel.left) {
                translate.x = pageRectModel.left - pageRectNow.left
            }
            if (pageRectNow.top + translate.y >= pageRectModel.top) {
                translate.y = pageRectModel.top - pageRectNow.top
            }
            if (pageRectNow.right + translate.x <= pageRectModel.right) {
                translate.x = pageRectModel.right - pageRectNow.right
            }
            if (pageRectNow.bottom + translate.y <= pageRectModel.bottom) {
                translate.y = pageRectModel.bottom - pageRectNow.bottom
            }

            moveMatrix.postTranslate(translate.x, translate.y)

            drawView.drawFile.body[drawView.pageAttuale].matrixPage = Matrix(moveMatrix)
            drawView.draw(scaling = true)

        }
        MotionEvent.ACTION_UP -> {

            drawView.draw(redraw = true)

        }

    }
}