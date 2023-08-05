package com.studiomath.pencilnotes.document.drawEvent

import android.content.Context
import android.view.InputDevice
import android.view.MotionEvent
import com.studiomath.pencilnotes.document.DrawView
import com.studiomath.pencilnotes.drawImpostazioni

private const val TAG = "DrawView"

class DrawMotionEvent(var context: Context, var drawView: DrawView) {
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

    fun onTouchView(event: MotionEvent) {
        drawView.mEvent = MotionEvent.obtain(event)

        if (event.action == MotionEvent.ACTION_DOWN) continueScaleTranslate = false

        /**
         * gestione degli input provenienti da TOOL_TYPE_STYLUS
         */
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || (event.pointerCount == 1 && !drawImpostazioni.modePenna && !continueScaleTranslate)) {
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
//                    drawView.strumentoAttivo = DrawView.Pennello.EVIDENZIATORE
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
                else -> {}
            }

            return

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
//        if (drawImpostazioni.modePenna && event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
//            drawView.scrollChangePagina(event)
//        }

        /**
         * eseguo lo scaling
         */
        if((event.pointerCount == 1 || event.pointerCount == 2) && event.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS){
            matrixTransformation(event, drawView)

            if(!drawImpostazioni.modePenna) {
                drawView.drawLastPath = false
            }
            continueScaleTranslate = true
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


}