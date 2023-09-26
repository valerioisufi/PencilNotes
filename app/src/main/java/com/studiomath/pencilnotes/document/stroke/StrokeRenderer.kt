package com.studiomath.pencilnotes.document.stroke

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.SystemClock
import com.studiomath.pencilnotes.file.DrawViewModel
import java.lang.Integer.max
import kotlin.math.abs
import kotlin.math.sqrt


class StrokeRenderer(stroke: DrawViewModel.Stroke) {
    constructor(zIndex: Int, type: DrawViewModel.Stroke.StrokeType) : this(
        DrawViewModel.Stroke(
            zIndex, type
        )
    )

    private val TOLERANCE = 5f

    private val mSegments = -10
    private val mTension = .5f
    private val mMinWidth = 3f
    private val mMaxWidth = 20f
    private val mWidthType = WidthType.FasterThinner
    private val mPaint: Paint? = null
    private val mBitmap: Bitmap? = null
    private val mCanvas: Canvas? = null
    private val mTensionVector1 = PointF()
    private val mTensionVector2 = PointF()
    private val mPoints = RingBuffer<CurvePoint>(4)

    private fun addPointInternal(x: Float, y: Float): Boolean {
        val prevPoint: CurvePoint?
        if (mPoints.size > 0) {
            prevPoint = mPoints.last
            if ((abs(prevPoint!!.x - x) < TOLERANCE) && (abs(prevPoint.y - y) < TOLERANCE)) {
                return false
            }
        } else {
            prevPoint = null
        }
        val point: CurvePoint = if (mPoints.isFilled) mPoints.first!! else CurvePoint()
        point.x = x
        point.y = y
        point.time = SystemClock.elapsedRealtime()
        if (prevPoint != null) {
            point.width = mMaxWidth / mWidthType.apply(point.velocity(prevPoint))
            point.width = Math.max(point.width, mMinWidth)
            point.width = Math.min(point.width, mMaxWidth)
            if (mPoints.size == 1) {
                prevPoint.width = point.width
            }
        } else {
            point.width = mMaxWidth
        }
        mPoints.add(point)
        return true
    }

    private fun renderPoints(): Boolean {
        if (mPoints.size < 4) {
            return false
        }
        var lastX = 0f
        var lastY = 0f
        mPaint!!.strokeCap = Paint.Cap.ROUND
        val pn: CurvePoint = mPoints[0]!!
        val p1: CurvePoint = mPoints[1]!!
        val p2: CurvePoint = mPoints[2]!!
        val pp: CurvePoint = mPoints[3]!!
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
            val pow2 = Math.pow(progress.toDouble(), 2.0).toFloat()
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
                mPaint.strokeWidth = p1.width + (p2.width - p1.width) * progress
                mCanvas!!.drawLine(lastX, lastY, x, y, mPaint)
            }
            lastX = x
            lastY = y
        }
        return true
    }

    internal class CurvePoint {
        var x = 0f
        var y = 0f
        var time: Long = 0
        var width = 0f
        fun distance(point: CurvePoint?): Float {
            val dx = x - point!!.x
            val dy = y - point.y
            return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        }

        fun velocity(point: CurvePoint?): Float {
            return distance(point) / abs(time - point!!.time)
        }
    }

    enum class WidthType {
        FasterThinner {
            override fun apply(`val`: Float): Float {
                return `val`
            }
        },
        FasterThicker {
            override fun apply(`val`: Float): Float {
                return 1 / `val`
            }
        };

        abstract fun apply(`val`: Float): Float
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