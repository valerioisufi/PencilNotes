package com.example.pencil.document.drawEvent

import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import com.example.pencil.document.DrawView
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * funzione che si occupa dello scale e dello spostamento
 */
// TODO: 23/01/2022 sarebbe il caso di avviare lo scale solo
//  dopo che sia stato rilevato un movimento significativo
private var startMatrix = Matrix()
private var moveMatrix = Matrix()


private val FIRST_POINTER_INDEX = 0
private val SECOND_POINTER_INDEX = 1

private var downPos = PointersPos(PointF(), PointF())
private var movePos = PointersPos(PointF(), PointF())

private var downDistance = 0f
private var moveDistance = 0f

private var downFocusPos = PointF()
private var moveFocusPos = PointF()

private var translate = PointF(0f, 0f)
private var scaleFactor = 1f


fun matrixScaleTranslate(event: MotionEvent, drawView: DrawView) {
    /**
     * Matrix()
     * https://i-rant.arnaudbos.com/matrices-for-developers/
     * https://i-rant.arnaudbos.com/2d-transformations-android-java/
     */
    when (event.actionMasked) {
        MotionEvent.ACTION_POINTER_DOWN -> {
            downPos.pointer1 = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
            downPos.pointer2 = PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

            downDistance = sqrt((downPos.pointer2.x - downPos.pointer1.x).pow(2) + (downPos.pointer2.y - downPos.pointer1.y).pow(2))
            downFocusPos = PointF((downPos.pointer1.x + downPos.pointer2.x) / 2, (downPos.pointer1.y + downPos.pointer2.y) / 2)

            startMatrix = Matrix(drawView.drawFile.body[drawView.pageAttuale].matrixPage)
            drawView.drawLastPath = false
        }
        MotionEvent.ACTION_MOVE -> {
            movePos.pointer1 = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
            movePos.pointer2 = PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

            moveDistance = sqrt((movePos.pointer2.x - movePos.pointer1.x).pow(2) + (movePos.pointer2.y - movePos.pointer1.y).pow(2))
            moveFocusPos = PointF((movePos.pointer1.x + movePos.pointer2.x) / 2, (movePos.pointer1.y + movePos.pointer2.y) / 2)

            translate = PointF(moveFocusPos.x - downFocusPos.x, moveFocusPos.y - downFocusPos.y)
            scaleFactor = (moveDistance / downDistance)


            moveMatrix = Matrix(startMatrix)

            val f = FloatArray(9)
            moveMatrix.getValues(f)

            /**
             * scale max e scale min
             */
            val lastScaleFactor = f[Matrix.MSCALE_X]

            val scaleMax = 5f
            val scaleMin = 1f
            if (lastScaleFactor * scaleFactor < scaleMin) {
                scaleFactor = scaleMin / lastScaleFactor
            }
            if (lastScaleFactor * scaleFactor > scaleMax) {
                scaleFactor = scaleMax / lastScaleFactor
            }
            moveMatrix.postScale(scaleFactor, scaleFactor, downFocusPos.x, downFocusPos.y)

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
        MotionEvent.ACTION_POINTER_UP -> {
            drawView.draw(redraw = true)

        }

    }
}