package com.studiomath.pencilnotes.document.motion

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import com.studiomath.pencilnotes.document.DrawManager
import com.studiomath.pencilnotes.document.DrawManager.DrawAttachments
import com.studiomath.pencilnotes.document.DrawManager.DrawAttachments.DrawMode
import com.studiomath.pencilnotes.document.DrawViewModel
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt



/**
 * scale e translate
 */


class OnScaleTranslate(
    private var drawViewModel: DrawViewModel
) {
    var touchSlop = drawViewModel.configuration.scaledTouchSlop
    var minVelocity = drawViewModel.configuration.scaledMinimumFlingVelocity
    var maximumScrollOffset = drawViewModel.configuration.scaledMaximumFlingVelocity

    var velocityTracker: VelocityTracker? = null
    var startMatrix = Matrix()


    class MatrixTransformation() {
        var pointers = mutableListOf<PointF>()
            set(value) {
                field = value

                if (value.size == 1) {
                    distance = 1f
                    focusPos = PointF(
                        pointers[0].x,
                        pointers[0].y
                    )

                } else if (value.size == 2) {
                    distance = sqrt((pointers[1].x - pointers[0].x).pow(2) + (pointers[1].y - pointers[0].y).pow(2))
                    focusPos = PointF(
                        (pointers[0].x + pointers[1].x) / 2,
                        (pointers[0].y + pointers[1].y) / 2
                    )

                }
            }

        var distance = 1f
        var focusPos = PointF()
    }

    var down = MatrixTransformation()
    var move = MatrixTransformation()

    val FIRST_POINTER_INDEX = 0
    val SECOND_POINTER_INDEX = 1

    var translate = PointF(0f, 0f)
    var scaleFactor = 1f

    var excessX = 0f
    var excessY = 0f

    var isScaling = false
    var continueScaleTranslate = false

    fun onInterceptScaleTranslate(event: MotionEvent): Boolean{
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */
        return continueScaleTranslate
    }

    fun onScaleTranslate(event: MotionEvent) {
        if (velocityTracker == null){
            velocityTracker = VelocityTracker.obtain()
        }
        /**
         * funzione che si occupa dello scale e dello spostamento
         */

        /**
         * Matrix()
         * https://i-rant.arnaudbos.com/matrices-for-developers/
         * https://i-rant.arnaudbos.com/2d-transformations-android-java/
         */
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker!!.addMovement(event)

                down.pointers = mutableListOf(
                    PointF(
                        event.getX(FIRST_POINTER_INDEX),
                        event.getY(FIRST_POINTER_INDEX)
                    )
                )

                startMatrix =
                    Matrix(drawViewModel.drawManager.moveMatrix)
//                    drawLastPath = false

                drawViewModel.drawManager.scroller.forceFinished(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                velocityTracker!!.addMovement(event)
                isScaling = true

                down.pointers = mutableListOf(
                    PointF(
                        event.getX(FIRST_POINTER_INDEX),
                        event.getY(FIRST_POINTER_INDEX)
                    ),
                    PointF(
                        event.getX(SECOND_POINTER_INDEX),
                        event.getY(SECOND_POINTER_INDEX)
                    )
                )

                startMatrix =
                    Matrix(drawViewModel.drawManager.moveMatrix)

            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker!!.addMovement(event)

                if (event.pointerCount == 1) {
                    if (isScaling) {
                    }

                    move.pointers = mutableListOf(
                        PointF(
                            event.getX(FIRST_POINTER_INDEX),
                            event.getY(FIRST_POINTER_INDEX)
                        )
                    )

                } else if (event.pointerCount == 2) {
                    move.pointers = mutableListOf(
                        PointF(
                            event.getX(FIRST_POINTER_INDEX),
                            event.getY(FIRST_POINTER_INDEX)
                        ),
                        PointF(
                            event.getX(SECOND_POINTER_INDEX),
                            event.getY(SECOND_POINTER_INDEX)
                        )
                    )

                }


                translate = PointF(
                    move.focusPos.x - down.focusPos.x,
                    move.focusPos.y - down.focusPos.y
                )
                scaleFactor =
                    (move.distance / down.distance)


                val tempMatrix = Matrix(startMatrix)

                val f = FloatArray(9)
                tempMatrix.getValues(f)

                /**
                 * scale max e scale min
                 */
                val lastScaleFactor = f[Matrix.MSCALE_X]

                val scaleMax = 5f
                val scaleMin = 0.5f
                if (lastScaleFactor * scaleFactor < scaleMin) {
                    scaleFactor = scaleMin / lastScaleFactor
                }
                if (lastScaleFactor * scaleFactor > scaleMax) {
                    scaleFactor = scaleMax / lastScaleFactor
                }
                tempMatrix.postScale(
                    scaleFactor,
                    scaleFactor,
                    down.focusPos.x,
                    down.focusPos.y
                )

                tempMatrix.postTranslate(
                    translate.x,
                    translate.y
                )

                drawViewModel.drawManager.apply {
                    val result = calcPage.applyBounds(tempMatrix, calcPage.contentRect, windowRect)
                    excessX = result.first
                    excessY = result.second

                    var transformedContentRect =
                        RectF(drawViewModel.drawManager.calcPage.contentRect)
                    drawViewModel.drawManager.moveMatrix.mapRect(transformedContentRect)

                    if (transformedContentRect.width() < drawViewModel.drawManager.windowRect.width() && !isScaling){
                        excessX = 0f
                    }
                    Log.d("BOUNCE", "Eccesso X: $excessX, Eccesso Y: $excessY")

                    elasticMatrix = calcPage.applyElasticEffect(excessX, excessY)
                }

                drawViewModel.drawManager.moveMatrix =
                    Matrix(tempMatrix)

                Log.d("SCALE_TRANSLATE", "onDrawView: moveMatrix = ${drawViewModel.drawManager.moveMatrix}")
                drawViewModel.drawManager.requestDraw(
                    DrawAttachments(drawMode = DrawMode.SCALE_TRANSLATE)
                )

            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.actionIndex == 1) {
                    down.pointers = mutableListOf(
                        PointF(
                            event.getX(FIRST_POINTER_INDEX),
                            event.getY(FIRST_POINTER_INDEX)
                        )
                    )
                } else if (event.actionIndex == 0) {
                    down.pointers = mutableListOf(
                        PointF(
                            event.getX(SECOND_POINTER_INDEX),
                            event.getY(SECOND_POINTER_INDEX)
                        )
                    )
                }

                startMatrix =
                    Matrix(drawViewModel.drawManager.moveMatrix)
                isScaling = false

            }

            MotionEvent.ACTION_UP -> {
                velocityTracker!!.computeCurrentVelocity(1000)

                drawViewModel.drawManager.calcPage.applyBounds(
                    drawViewModel.drawManager.moveMatrix,
                    drawViewModel.drawManager.calcPage.contentRect,
                    drawViewModel.drawManager.windowRect
                )
                Log.d("BOUNCE_BACK", "moveMatrix after applyBounds: ${drawViewModel.drawManager.moveMatrix}")



                Log.d("FLING", "excessX: $excessX, excessY: $excessY")

                if (excessX != 0f || excessY != 0f) {
                    Log.d("FLING", "Disabilitato: fuori dai limiti")
                    Log.d("BOUNCE_BACK", "1, moveMatrix: ${drawViewModel.drawManager.moveMatrix}, startAnimateMatrix: ${drawViewModel.drawManager.startAnimateMatrix}, elasticMatrix: ${drawViewModel.drawManager.elasticMatrix}")

                    drawViewModel.drawManager.startAnimateMatrix.set(drawViewModel.drawManager.moveMatrix)

                    // Invece di fare il fling, applica un'animazione di rimbalzo
                    drawViewModel.drawManager.calcPage.startBounceBackAnimation(
                        excessX, excessY, drawViewModel.drawManager.elasticMatrix,
                        updateCallback = {
                            drawViewModel.drawManager.moveMatrix.set(drawViewModel.drawManager.startAnimateMatrix)
                            Log.d("BOUNCE_BACK", "moveMatrix: ${drawViewModel.drawManager.moveMatrix}, startAnimateMatrix: ${drawViewModel.drawManager.startAnimateMatrix}, elasticMatrix: ${drawViewModel.drawManager.elasticMatrix}")
                            // Ridisegna la view
                            drawViewModel.drawManager.requestDraw(
                                DrawAttachments(drawMode = DrawMode.ANIMATE).apply {
                                    animationType = DrawAttachments.AnimationType.BOUNCE_BACK
                                }
                            )
                        },
                        onEndCallback = {
                            drawViewModel.drawManager.requestDraw(
                                DrawAttachments(drawMode = DrawMode.UPDATE).apply {
                                    update = DrawAttachments.Update.DRAW_BITMAP
                                }
                            )
                        }
                    )


                } else {
                    // Calcola i limiti normali del fling
                    var transformedContentRect =
                        RectF(drawViewModel.drawManager.calcPage.contentRect)
                    drawViewModel.drawManager.moveMatrix.mapRect(transformedContentRect)

                    Log.d("FLING", "transformedContentRect: $transformedContentRect")

                    var startPointScroller = floatArrayOf(0f, 0f)
                    drawViewModel.drawManager.moveMatrix.mapPoints(startPointScroller)


                    val xOffset = startPointScroller[0] - transformedContentRect.left
                    // Quanto può scorrere a sinistra
                    var minX =
                        (drawViewModel.drawManager.windowRect.width() - transformedContentRect.width() + xOffset).coerceAtMost(
                            xOffset
                        ).toInt()
                    // Quanto può scorrere a destra
                    var maxX = xOffset.toInt()

                    var velocityX = velocityTracker!!.xVelocity.toInt()

                    if (transformedContentRect.width() < drawViewModel.drawManager.windowRect.width()) {
                        minX = Int.MIN_VALUE
                        maxX = Int.MAX_VALUE
                        velocityX = 0
                    }

                    val yOffset = startPointScroller[1] - transformedContentRect.top
                    val minY =
                        (drawViewModel.drawManager.windowRect.height() - transformedContentRect.height() + yOffset).coerceAtMost(
                            0f
                        ).toInt()
                    val maxY = yOffset.toInt()
                    val velocityY = velocityTracker!!.yVelocity.toInt()

                    if (abs(velocityX) > minVelocity || abs(velocityY) > minVelocity) {
                        drawViewModel.drawManager.scroller.fling(
                            startPointScroller[0].toInt(), startPointScroller[1].toInt(),
                            velocityX, velocityY,
                            minX, maxX, minY, maxY,
                            200, 200
                        )

                        Log.d("FLING", "Avviato con velocità X: $velocityX, Y: $velocityY")
                    } else {
                        Log.d("FLING", "Velocità insufficiente per il fling")
                    }

                    drawViewModel.drawManager.startAnimateMatrix =
                        Matrix(drawViewModel.drawManager.moveMatrix)

                    drawViewModel.drawManager.requestDraw(
                        DrawAttachments(drawMode = DrawMode.ANIMATE).apply {
                            animationType = DrawAttachments.AnimationType.FLING
                        }
                    )
                }

                velocityTracker!!.recycle()
                velocityTracker = null

            }
        }

        continueScaleTranslate = true
    }

}