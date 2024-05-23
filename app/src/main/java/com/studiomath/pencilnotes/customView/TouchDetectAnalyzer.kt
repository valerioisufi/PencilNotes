package com.studiomath.pencilnotes.customView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.studiomath.pencilnotes.R
import com.studiomath.pencilnotes.dpToPx

class TouchDetectAnalyzer(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var touchAnalyze = false

    var paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        isFilterBitmap = true
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 3f // default: Hairline-width (really thin)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (touchAnalyze) {
            makeTouchAnalyzer(canvas)
            touchAnalyze = false
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        touchAnalyze = true
        mEvent = event
        invalidate()

        return true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        return true
    }

    lateinit var mEvent: MotionEvent
    fun makeTouchAnalyzer(canvas: Canvas) {
        var xPrecision = mEvent.xPrecision
        var yPrecision = mEvent.yPrecision

        var downTime = mEvent.downTime
        var eventTime = mEvent.eventTime

        var inputDevice = mEvent.device
        var inputSource = mEvent.source

        for (i in 0 until mEvent.pointerCount) {
            var toolType = mEvent.getToolType(i)
            var pointerId = mEvent.getPointerId(i)

            /**
             * toolMajor e touchMajor (così come toolMinor e touchMinor)
             * hanno lo stesso valore quando toolType = TOOL_TYPE_FINGER
             * e hanno valore nullo quando toolType = TOOL_TYPE_STYLUS
             */
            var toolMajor = mEvent.getToolMajor(i)
            var toolMinor = mEvent.getToolMinor(i)

            var touchMajor = mEvent.getTouchMajor(i)
            var touchMinor = mEvent.getTouchMinor(i)

            var size = mEvent.getSize(i)

            /**
             * Axis Values
             */
            var pressure = mEvent.getAxisValue(MotionEvent.AXIS_PRESSURE, i)
            var orientation = mEvent.getAxisValue(MotionEvent.AXIS_ORIENTATION, i)
            var tilt = mEvent.getAxisValue(MotionEvent.AXIS_TILT, i)
            var distance = mEvent.getAxisValue(
                MotionEvent.AXIS_DISTANCE,
                i
            ) // non funziona con M-Pencil di Huawei

            var x = mEvent.getX(i)
            var y = mEvent.getY(i)


            var toolRect = RectF(
                x - toolMinor / 2,
                y - toolMajor / 2,
                x + toolMinor / 2,
                y + toolMajor / 2
            )
            var toolPath = Path().apply {
                addOval(toolRect, Path.Direction.CW)
                transform(Matrix().apply {
                    setRotate((orientation * 180 / 3.14).toFloat(), x, y)
                })
            }

            canvas.drawPath(toolPath, Paint(paint).apply {
                color = ResourcesCompat.getColor(resources, R.color.light_blue_600, null)
                style = Paint.Style.STROKE
                strokeWidth = 10f
            })

            if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                var stylusPath = Path().apply {
                    addCircle(x, y, dpToPx(context, 30) * pressure, Path.Direction.CW)
                }

                canvas.drawPath(stylusPath, Paint(paint).apply {
                    color = ResourcesCompat.getColor(resources, R.color.purple_200, null)
                    style = Paint.Style.STROKE
                    strokeWidth = 10f
                })
            }

            if (mEvent.action == MotionEvent.ACTION_CANCEL) {
                canvas.drawText(
                    "CANCEL",
                    x + 150f, y,
                    Paint(paint).apply {
                        textSize = 30f
                        color = ResourcesCompat.getColor(resources, R.color.black, null)
                    }
                )
            }
            for (historyIndex in 1 until mEvent.historySize) {
                /**
                 * toolMajor e touchMajor (così come toolMinor e touchMinor)
                 * hanno lo stesso valore quando toolType = TOOL_TYPE_FINGER
                 * e hanno valore nullo quando toolType = TOOL_TYPE_STYLUS
                 */
                var toolMajorHistorical = mEvent.getHistoricalToolMajor(i, historyIndex)
                var toolMinorHistorical = mEvent.getHistoricalToolMinor(i, historyIndex)

                var touchMajorHistorical = mEvent.getHistoricalTouchMajor(i, historyIndex)
                var touchMinorHistorical = mEvent.getHistoricalTouchMinor(i, historyIndex)

                var sizeHistorical = mEvent.getHistoricalSize(i, historyIndex)

                /**
                 * Axis Values
                 */
                var pressureHistorical =
                    mEvent.getHistoricalAxisValue(MotionEvent.AXIS_PRESSURE, i, historyIndex)
                var orientationHistorical =
                    mEvent.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, i, historyIndex)
                var tiltHistorical =
                    mEvent.getHistoricalAxisValue(MotionEvent.AXIS_TILT, i, historyIndex)
                var distanceHistorical = mEvent.getHistoricalAxisValue(
                    MotionEvent.AXIS_DISTANCE,
                    i,
                    historyIndex
                ) // non funziona con M-Pencil di Huawei

                var xHistorical = mEvent.getHistoricalX(i, historyIndex)
                var yHistorical = mEvent.getHistoricalY(i, historyIndex)
            }

        }
    }

    fun makeInputDeviceAnalyzer(canvas: Canvas) {
        var deviceIds = InputDevice.getDeviceIds()

        for (deviceId in deviceIds) {
            var inputDevice = InputDevice.getDevice(deviceId)
            var descriptor = inputDevice?.descriptor
            var motionRanges = inputDevice?.motionRanges

        }
    }
}