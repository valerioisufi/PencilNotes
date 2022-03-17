package com.example.pencil.document.drawEvent

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import android.view.GestureDetector
import android.view.InputDevice
import android.view.MotionEvent
import com.example.pencil.document.DrawView
import com.example.pencil.drawImpostazioni
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "DrawView"

class DrawMotionEvent(var context: Context, var drawView: DrawView) :
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {
    var listDevices = mutableMapOf<String, InputDevice>()
    var listMotionRanges = mutableMapOf<String, MutableList<InputDevice.MotionRange>>()

    init {
        val deviceIds = InputDevice.getDeviceIds()
        for (deviceId in deviceIds) {
            val inputDevice = InputDevice.getDevice(deviceId)
            val descriptor = inputDevice.descriptor
            val motionRanges = inputDevice.motionRanges

            listDevices[descriptor] = inputDevice
            listMotionRanges[descriptor] = motionRanges
        }
    }


    private var path: String = ""
    private var continueScaleTranslate = false


    //    lateinit var mDetector: GestureDetectorCompat
//    lateinit var mScaleDetector: ScaleGestureDetector
    fun onTouchView(event: MotionEvent) {
        drawView.mEvent = MotionEvent.obtain(event)
//        drawView.draw(makeCursore = true)

        // Let the GestureDetector and the ScaleGestureDetector inspect all events.
//        mDetector.onTouchEvent(event)
//        mScaleDetector.onTouchEvent(event)


        /**
         * gestione degli input provenienti da TOOL_TYPE_STYLUS
         */
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            var descriptorInputDevice = event.device.descriptor

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawView.listTracciati.add(
                        DrawView.Tracciato(
                            MotionEvent.TOOL_TYPE_STYLUS,
                            descriptorInputDevice
                        )
                    )
                    drawView.listTracciati.last().downTime = event.downTime
                    drawView.listTracciati.last().listPoint.add(
                        DrawView.Punto(event.x, event.y).apply {
                            eventTime = event.downTime

                            pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE)
                            orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
                            tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
                        }
                    )
                }

                MotionEvent.ACTION_MOVE -> {
                    for (historyIndex in 1 until event.historySize) {
                        drawView.listTracciati.last().listPoint.add(
                            DrawView.Punto(
                                event.getHistoricalX(historyIndex),
                                event.getHistoricalY(historyIndex)
                            ).apply {
                                eventTime = event.getHistoricalEventTime(historyIndex)

                                pressure = event.getHistoricalAxisValue(
                                    MotionEvent.AXIS_PRESSURE,
                                    historyIndex
                                )
                                orientation = event.getHistoricalAxisValue(
                                    MotionEvent.AXIS_ORIENTATION,
                                    historyIndex
                                )
                                tilt = event.getHistoricalAxisValue(
                                    MotionEvent.AXIS_TILT,
                                    historyIndex
                                )
                            }
                        )
                    }

                    drawView.listTracciati.last().listPoint.add(
                        DrawView.Punto(event.x, event.y).apply {
                            eventTime = event.eventTime

                            pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE)
                            orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)
                            tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
                        }
                    )


                }
            }

            /**
             * gestisco il motionEvent in modo differente per ogni strumento
             */
            val tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
            val orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION)

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (tilt > 0.8f && orientation > -0.3f && orientation < 0.5f) {
                    drawView.strumentoAttivo = DrawView.Pennello.EVIDENZIATORE
                } else if (tilt > 0.2f && (orientation > 2.5f || orientation < -2.3f)) {
//                    drawView.strumentoAttivo = DrawView.Pennello.LAZO
                }
            }

            when (drawView.strumentoAttivo) {
                DrawView.Pennello.PENNA -> drawView.strumentoPenna?.gestioneMotionEvent(
                    drawView,
                    event
                )
                DrawView.Pennello.EVIDENZIATORE -> drawView.strumentoEvidenziatore?.gestioneMotionEvent(
                    drawView,
                    event
                )
                DrawView.Pennello.GOMMA -> drawView.strumentoGomma?.gestioneMotionEvent(
                    drawView,
                    event
                )
            }

        }

        /**
         * controllo il palmRejection
         */
        if (palmRejection(event)) {
            return
        }

        /**
         * gesture per il cambio della pagina
         */
        if (drawImpostazioni.modePenna && event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            drawView.scrollChangePagina(event)
        }

        /**
         * eseguo lo scaling
         */
        if (event.pointerCount == 2) {
            scaleTranslate(event)

//            if(!drawImpostazioni.modePenna) {
//                drawView.strumentoPenna!!.rewritePath(drawView, "")
//            }
//            continueScaleTranslate = true
        }

        if(drawView.strumentoAttivo == DrawView.Pennello.LAZO){
            for (image in drawView.drawFile.body[drawView.pageAttuale].images){

                if(image.rectVisualizzazione.contains(event.x, event.y)){
                    image.rectVisualizzazione.apply {
                        left -= 10f
                        top -= 10f
                        right += 10f
                        bottom += 10f
                    }
                    drawView.draw(redraw = true)
                    drawView.drawFile.writeXML()
                    break
                }

            }
        }
    }

    fun onHoverView(event: MotionEvent) {
        drawView.mEvent = MotionEvent.obtain(event)
        drawView.draw(makeCursore = true)

//        Log.d(TAG, "onHoverView: ${event.action}")
//
//        when (event.action) {
//            MotionEvent.ACTION_HOVER_ENTER -> hoverStart(drawView, event)
//            MotionEvent.ACTION_HOVER_MOVE -> hoverMove(drawView, event)
//            MotionEvent.ACTION_HOVER_EXIT -> hoverUp(drawView, event)
//        }
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

    private val DEBUG_TAG = "Gestures"


    /**
     * GestureDetector
     */
    var beginTouch = PointF()
    override fun onDown(event: MotionEvent): Boolean {
        Log.d(DEBUG_TAG, "onDown: $event")

        beginTouch = PointF(event.x, event.y)

        return true
    }

    override fun onFling(
        event1: MotionEvent,
        event2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        Log.d(DEBUG_TAG, "onFling: $event1 $event2")
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        Log.d(DEBUG_TAG, "onLongPress: $event")
    }

    override fun onScroll(
        event1: MotionEvent,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        Log.d(DEBUG_TAG, "onScroll: $distanceX $distanceY")
//        val f = FloatArray(9)
//        drawView.startMatrix.getValues(f)
//
//        var startTranslate = PointF(f[Matrix.MTRANS_X], f[Matrix.MTRANS_Y])
//        var translate = PointF(event2.x - event1.x, event2.y - event1.y)
//
//        drawView.moveMatrix.setTranslate(translate.x, translate.y)
//        drawView.moveMatrix.preConcat(drawView.startMatrix)
//
//        drawView.draw(redraw = false, scaling = true)
//        drawView.draw(redraw = true, scaling = false)

        return true
    }

    override fun onShowPress(event: MotionEvent) {
        Log.d(DEBUG_TAG, "onShowPress: $event")
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        Log.d(DEBUG_TAG, "onSingleTapUp: $event")
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: $event")
        return true
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        Log.d(DEBUG_TAG, "onDoubleTap: $event")
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: $event")
        return true
    }


    /**
     * funzine che restituisce TRUE quando viene appoggiato sullo schermo il palmo della mano
     */
    // TODO: 23/01/2022 qui devo tener conto del fatto che, quando viene
    //  rilevato il palmo, alcune azioni come oo scale siano già iniziate.
    //  Per cui io dovrei ultimare quelle azioni
    fun palmRejection(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            if (event.getToolMinor(i) / event.getToolMajor(i) < 0.5) {
                return true
            }
        }
        return false
    }

    /**
     * funzione che si occupa dello scale e dello spostamento
     */
    // TODO: 23/01/2022 sarebbe il caso di avviare lo scale solo
    //  dopo che sia stato rilevato un movimento significativo
    private var startMatrix = Matrix()
    var moveMatrix = Matrix()


    private val FIRST_POINTER_INDEX = 0
    private val SECOND_POINTER_INDEX = 1

    private var fStartPos = PointF()
    private var sStartPos = PointF()

    private var fMovePos = PointF()
    private var sMovePos = PointF()

    private var startDistance = 0f
    private var moveDistance = 0f

    private var lastTranslate = PointF(0f, 0f)
    private var lastScaleFactor = 1f

    private var scaleFactor = 1f
    private var translate = PointF(0f, 0f)

    private var startFocusPos = PointF()
    private var moveFocusPos = PointF()


    fun scaleTranslate(event: MotionEvent) {
        /**
         * Matrix()
         * https://i-rant.arnaudbos.com/matrices-for-developers/
         * https://i-rant.arnaudbos.com/2d-transformations-android-java/
         */
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                fStartPos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sStartPos =
                    PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                startDistance =
                    sqrt((sStartPos.x - fStartPos.x).pow(2) + (sStartPos.y - fStartPos.y).pow(2))
                startFocusPos =
                    PointF((fStartPos.x + sStartPos.x) / 2, (fStartPos.y + sStartPos.y) / 2)

                startMatrix = Matrix(moveMatrix)
                drawView.drawLastPath = false
            }
            MotionEvent.ACTION_MOVE -> {
                fMovePos = PointF(event.getX(FIRST_POINTER_INDEX), event.getY(FIRST_POINTER_INDEX))
                sMovePos =
                    PointF(event.getX(SECOND_POINTER_INDEX), event.getY(SECOND_POINTER_INDEX))

                moveDistance =
                    sqrt((sMovePos.x - fMovePos.x).pow(2) + (sMovePos.y - fMovePos.y).pow(2))
                moveFocusPos = PointF((fMovePos.x + sMovePos.x) / 2, (fMovePos.y + sMovePos.y) / 2)

                translate =
                    PointF(moveFocusPos.x - startFocusPos.x, moveFocusPos.y - startFocusPos.y)
                scaleFactor = (moveDistance / startDistance)


                moveMatrix.setTranslate(translate.x, translate.y)
                moveMatrix.preConcat(startMatrix)

                val f = FloatArray(9)
                moveMatrix.getValues(f)
                lastScaleFactor = f[Matrix.MSCALE_X]

                /**
                 * scale max e scale min
                 */
                val scaleMax = 5f
                val scaleMin = 1f
                if (lastScaleFactor * scaleFactor < scaleMin) {
                    scaleFactor = scaleMin / lastScaleFactor
                }
                if (lastScaleFactor * scaleFactor > scaleMax) {
                    scaleFactor = scaleMax / lastScaleFactor
                }
                moveMatrix.postScale(scaleFactor, scaleFactor, moveFocusPos.x, moveFocusPos.y)

//                /**
//                 * translate max/min
//                 */
//                val tempRectPage = drawView.calcPageRect(moveMatrix)
//                val initialWindowRect = drawView.calcPageRect(Matrix())
//                moveMatrix.getValues(f)
//
//                if (tempRectPage.left >= initialWindowRect.left) {
//                    f[Matrix.MTRANS_X] = initialWindowRect.left
//                }
//                if (tempRectPage.top >= initialWindowRect.top) {
//                    f[Matrix.MTRANS_Y] = initialWindowRect.top
//                }
//                if (tempRectPage.right <= initialWindowRect.right) {
//                    f[Matrix.MTRANS_X] += initialWindowRect.right - tempRectPage.right
//                }
//                if (tempRectPage.bottom <= initialWindowRect.bottom) {
//                    f[Matrix.MTRANS_Y] += initialWindowRect.bottom - tempRectPage.bottom
//                }
//
//                moveMatrix.setValues(f)


                moveMatrix.getValues(f)
                lastScaleFactor = f[Matrix.MSCALE_X]
//                Log.d("Scale factor: ", f[Matrix.MSCALE_X].toString())

                drawView.draw(scaling = true)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                lastTranslate = PointF(translate.x, translate.y)
                //lastScaleFactor = scaleFactor
                drawView.draw(redraw = true)
            }

        }
    }


}