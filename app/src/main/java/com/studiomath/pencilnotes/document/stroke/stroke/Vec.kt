package com.studiomath.pencilnotes.document.stroke.stroke

import com.studiomath.pencilnotes.file.DrawViewModel.Stroke.Point
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Negate a vector.
 * @param A
 * @internal
 */
fun neg(A: Point): Point {
    return Point(-A.x, -A.y)
}

/**
 * Add vectors.
 * @param A
 * @param B
 * @internal
 */
fun add(A: Point, B: Point): Point {
    return Point(A.x + B.x, A.y + B.y)
}

/**
 * Subtract vectors.
 * @param A
 * @param B
 * @internal
 */
fun sub(A: Point, B: Point): Point {
    return Point(A.x - B.x, A.y - B.y)
}

/**
 * Vector multiplication by scalar
 * @param A
 * @param n
 * @internal
 */
fun mul(A: Point, n: Float): Point {
    return Point(A.x * n, A.y * n)
}

/**
 * Vector division by scalar.
 * @param A
 * @param n
 * @internal
 */
fun div(A: Point, n: Float): Point {
    return Point(A.x / n, A.y / n)
}

/**
 * Perpendicular rotation of a vector A
 * @param A
 * @internal
 */
fun per(A: Point): Point {
    return Point(A.y, -A.x)
}

/**
 * Dot product
 * @param A
 * @param B
 * @internal
 */
fun dpr(A: Point, B: Point): Float {
    return A.x * B.x+ A.y * B.y
}

/**
 * Get whether two vectors are equal.
 * @param A
 * @param B
 * @internal
 */
fun isEqual(A: Point, B: Point): Boolean {
    return A.x == B.x && A.y == B.y
}

/**
 * Length of the vector
 * @param A
 * @internal
 */
fun len(A: Point): Float {
    return hypot(A.x, A.y)
}

/**
 * Length of the vector squared
 * @param A
 * @internal
 */
fun len2(A: Point): Float {
    return A.x * A.x + A.y * A.y
}

/**
 * Dist length from A to B squared.
 * @param A
 * @param B
 * @internal
 */
fun dist2(A: Point, B: Point): Float {
    return len2(sub(A, B))
}

/**
 * Get normalized / unit vector.
 * @param A
 * @internal
 */
fun uni(A: Point): Point {
    return div(A, len(A))
}

/**
 * Dist length from A to B
 * @param A
 * @param B
 * @internal
 */
fun dist(A: Point, B: Point): Float {
    return hypot(A.y - B.y, A.x - B.x)
}

/**
 * Mean between two vectors or mid vector between two vectors
 * @param A
 * @param B
 * @internal
 */
fun med(A: Point, B: Point): Point {
    return mul(add(A, B), 0.5f)
}

/**
 * Rotate a vector around another vector by r (radians)
 * @param A vector
 * @param C center
 * @param r rotation in radians
 * @internal
 */
fun rotAround(A: Point, C: Point, r: Float): Point {
    val s = sin(r)
    val c = cos(r)

    val px = A.x - C.x
    val py = A.y - C.y

    val nx = px * c - py * s
    val ny = px * s + py * c

    return Point(nx + C.x, ny + C.y)
}

/**
 * Interpolate vector A to B with a scalar t
 * @param A
 * @param B
 * @param t scalar
 * @internal
 */
fun lrp(A: Point, B: Point, t: Float): Point {
    return add(A, mul(sub(B, A), t))
}

/**
 * Project a point A in the direction B by a scalar c
 * @param A
 * @param B
 * @param c
 * @internal
 */
fun prj(A: Point, B: Point, c: Float): Point {
    return add(A, mul(B, c))
}