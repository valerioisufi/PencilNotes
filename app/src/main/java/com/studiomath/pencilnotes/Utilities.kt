package com.studiomath.pencilnotes

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue

fun dpToPx(dipValue: Float, metrics: DisplayMetrics): Int {
    val `val` = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dipValue,
        metrics
    )
    val res = (`val` + 0.5).toInt() // Round
    // Ensure at least 1 pixel if val was > 0
    return if (res == 0 && `val` > 0) 1 else res
}