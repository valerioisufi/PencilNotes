package com.studiomath.pencilnotes.document.stroke

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.studiomath.pencilnotes.file.DrawViewModel
import java.lang.Integer.max
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


class StrokeRenderer(stroke: DrawViewModel.Stroke) {
    constructor(zIndex: Int, type: DrawViewModel.Stroke.StrokeType) : this(
        DrawViewModel.Stroke(
            zIndex, type
        )
    )

    private val TOLERANCE = 5f

    private val mSegments = 5
    private val mTension = .5f

    private val mTensionVector1 = PointF()
    private val mTensionVector2 = PointF()

    private val mPoints = RingBuffer<DrawViewModel.Stroke.Point>(4)

    fun addPointInternal(point: DrawViewModel.Stroke.Point): Boolean {
        val prevPoint: DrawViewModel.Stroke.Point?
        if (mPoints.size > 0) {
            prevPoint = mPoints.last
//            if ((abs(prevPoint!!.x - point.x) < TOLERANCE) && (abs(prevPoint.y - point.y) < TOLERANCE)) {
//                return false
//            }
        }

        mPoints.add(point)
        return true
    }

    fun renderPoints(canvas: Canvas, paint: Paint): Boolean {
        if (mPoints.size < 4) {
            return false
        }
        var lastX = 0f
        var lastY = 0f

        paint.strokeCap = Paint.Cap.ROUND

        val pn: DrawViewModel.Stroke.Point = mPoints[0]!!
        val p1: DrawViewModel.Stroke.Point = mPoints[1]!!
        val p2: DrawViewModel.Stroke.Point = mPoints[2]!!
        val pp: DrawViewModel.Stroke.Point = mPoints[3]!!

        mTensionVector1.x = (p2.x - pn.x) * mTension
        mTensionVector1.y = (p2.y - pn.y) * mTension
        mTensionVector2.x = (pp.x - p1.x) * mTension
        mTensionVector2.y = (pp.y - p1.y) * mTension

        val segmentsCount: Int = if (mSegments < 0) {
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            max(
                mSegments,
                (abs(sqrt((dx * dx + dy * dy).toDouble())) * (1 / (-mSegments).toFloat()) + 0.5).toInt()
            ) + 1
        } else {
            mSegments
        }

        for (index in 0 until segmentsCount) {
            val progress = index / (segmentsCount - 1).toFloat()

            val pow2 = progress.toDouble().pow(2.0).toFloat()
            val pow3 = pow2 * progress
            val pow23 = pow2 * 3
            val pow32 = pow3 * 2

            val c1 = pow32 - pow23 + 1
            val c2 = pow23 - pow32
            val c3 = pow3 - 2 * pow2 + progress
            val c4 = pow3 - pow2

            val x: Float = c1 * p1.x + c2 * p2.x + c3 * mTensionVector1.x + c4 * mTensionVector2.x
            val y: Float = c1 * p1.y + c2 * p2.y + c3 * mTensionVector1.y + c4 * mTensionVector2.y

            if (index > 0) {
                paint.strokeWidth =
                    (p1.size * p1.pressure!!) + ((p2.size * p2.pressure!!) - (p1.size * p1.pressure!!)) * progress
                canvas.drawLine(lastX, lastY, x, y, paint)
            }

            lastX = x
            lastY = y
        }

        return true
    }


}

class RingBuffer<T>(capacity: Int) {
    var size = 0
        private set
    private var mOffset = 0
    private val mValues: Array<Any?>

    init {
        mValues = arrayOfNulls(capacity)
    }

    val isFilled: Boolean
        get() = size == mValues.size
    val first: T?
        get() = get(0)
    val last: T?
        get() = get(size - 1)

    operator fun get(index: Int): T? {
        return mValues[(mOffset + index) % mValues.size] as T?
    }

    fun add(value: T) {
        val index = if (size < mValues.size) size++ else mOffset++
        mValues[index % mValues.size] = value
    }

    fun clear() {
        for (index in mValues.indices) {
            mValues[index] = null
        }
        size = 0
        mOffset = 0
    }
}