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

private var downPos = PointF()
private var movePos = PointF()

data class PointersPos(var pointer1: PointF, var pointer2: PointF)
private var downPointers = PointersPos(PointF(), PointF())
private var movePointers = PointersPos(PointF(), PointF())

private var downDistance = 0f
private var moveDistance = 0f

private var downFocusPos = PointF()
private var moveFocusPos = PointF()

private var translate = PointF(0f, 0f)
private var scaleFactor = 1f

private var isScaling = false

fun matrixTransformation(event: MotionEvent, drawView: DrawView) {
    /**
     * Matrix()
     * https://i-rant.arnaudbos.com/matrices-for-developers/
     * https://i-rant.arnaudbos.com/2d-transformations-android-java/
     */
    if(event.pointerCount == 1 && !isScaling){
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
                val pageRectModel = drawView.calcPageRect(matrix = Matrix())

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

    if(event.pointerCount == 2){
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                isScaling = true

                downPointers.pointer1 =
                    PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                downPointers.pointer2 =
                    PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                downDistance = sqrt(
                    (downPointers.pointer2.x - downPointers.pointer1.x).pow(2) + (downPointers.pointer2.y - downPointers.pointer1.y).pow(
                        2
                    )
                )
                downFocusPos = PointF(
                    (downPointers.pointer1.x + downPointers.pointer2.x) / 2,
                    (downPointers.pointer1.y + downPointers.pointer2.y) / 2
                )

                startMatrix = Matrix(drawView.drawFile.body[drawView.pageAttuale].matrixPage)
                drawView.drawLastPath = false
            }
            MotionEvent.ACTION_MOVE -> {
                movePointers.pointer1 =
                    PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                movePointers.pointer2 =
                    PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                moveDistance = sqrt(
                    (movePointers.pointer2.x - movePointers.pointer1.x).pow(2) + (movePointers.pointer2.y - movePointers.pointer1.y).pow(
                        2
                    )
                )
                moveFocusPos = PointF(
                    (movePointers.pointer1.x + movePointers.pointer2.x) / 2,
                    (movePointers.pointer1.y + movePointers.pointer2.y) / 2
                )

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
                val pageRectModel = drawView.calcPageRect(matrix = Matrix())

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

    if (isScaling && event.actionMasked == MotionEvent.ACTION_UP) isScaling = false
}