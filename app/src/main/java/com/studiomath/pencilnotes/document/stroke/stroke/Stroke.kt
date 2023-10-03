package com.studiomath.pencilnotes.document.stroke.stroke

import com.studiomath.pencilnotes.file.DrawViewModel.Stroke.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The options object for `getStroke` or `getStrokePoints`.
 *
 * @property size La dimensione di base (diametro) del tratto.
 * @property thinning L'effetto della pressione sulla dimensione del tratto.
 * @property smoothing Quanto ammorbidire i bordi del tratto.
 * @property streamline La riduzione del tratto.
 * @property easing Una funzione di easing da applicare alla pressione di ogni punto.
 * @property simulatePressure Se simulare la pressione in base alla velocità.
 * @property start Opzioni per l'inizio del tratto (cap, affinamento, easing).
 * @property end Opzioni per la fine del tratto (cap, affinamento, easing).
 * @property last Se gestire i punti come tratto completato.
 */
data class StrokeOptions(
    val size: Float = 16f,
    val thinning: Float = 0.5f,
    val smoothing: Float = 0.5f,
    val streamline: Float = 0.32f,
    val easing: ((pressure: Float) -> Float) = Easing.linear,
    val simulatePressure: Boolean = false,

    val start: StartOptions = StartOptions(),
    val end: EndOptions = EndOptions(),
    val last: Boolean = false
)

/**
 * Opzioni per l'inizio del tratto (cap, affinamento, easing).
 *
 * @property cap Se applicare un cap all'inizio.
 * @property taper L'affinamento all'inizio.
 * @property easing Una funzione di easing per l'inizio.
 */
data class StartOptions(
    val cap: Boolean = true,
    val taper: Float = 0f,
    val easing: ((distance: Float) -> Float) = Easing.easeOutQuad
)

/**
 * Opzioni per la fine del tratto (cap, affinamento, easing).
 *
 * @property cap Se applicare un cap alla fine.
 * @property taper L'affinamento alla fine.
 * @property easing Una funzione di easing per la fine.
 */
data class EndOptions(
    val cap: Boolean = true,
    val taper: Float = 0f,
    val easing: ((distance: Float) -> Float) = Easing.easeOutCubic
)

/**
 * The points returned by `getStrokePoints`, and the input for `getStrokeOutlinePoints`.
 *
 * @property point Il punto del tratto.
 * @property vector Il vettore.
 * @property pressure La pressione.
 * @property distance La distanza.
 * @property runningLength La lunghezza totale finora.
 */
data class StrokePoint(
    val point: Point,
    var vector: Point,
    var pressure: Float,
    val distance: Float,
    val runningLength: Float,
)


/**
 * ## getStroke
 * @description Get an array of points describing a polygon that surrounds the input points.
 * @param points An array of points (as `[x, y, pressure]` or `{x, y, pressure}`). Pressure is optional in both cases.
 *
 * @param options (optional) An object with options.
 */
fun getStroke(
    points: MutableList<Point>,
    options: StrokeOptions = StrokeOptions()
): List<Point> {
    return getStrokeOutlinePoints(getStrokePoints(points, options), options)
}

/**
 * ## getStrokePoints
 * @description Get an array of points as objects with an adjusted point, pressure, vector, distance, and runningLength.
 * @param points An array of points (as `[x, y, pressure]` or `{x, y, pressure}`). Pressure is optional in both cases.
 * @param options (optional) An object with options.
 */
fun getStrokePoints(
    points: MutableList<Point>,
    options: StrokeOptions
): List<StrokePoint> {
    val streamline = options.streamline
    val size = options.size
    val isComplete = options.last

    // If we don't have any points, return an empty array.
    if (points.isEmpty()) return emptyList()

    // Find the interpolation level between points.
    val t = 0.15f + (1 - streamline) * 0.85f

    // Whatever the input is, make sure that the points are in number[][].
    var pts = points.toMutableList()

    // Add extra points between the two, to help avoid "dash" lines
    // for strokes with tapered start and ends. Don't mutate the
    // input array!
    if (pts.size == 2) {
        val lastPoint = points[1]
        pts = points.dropLast(1).toMutableList()
        for (i in 0..5) {
            points.add(lrp(pts[0], lastPoint, i / 4f))
        }
    }

    // If there's only one point, add another point at a 1pt offset.
    // Don't mutate the input array!
    if (pts.size == 1) {
        pts.add(
            Point(
                pts[0].x + 1f,
                pts[0].y + 1f
            ).apply { pressure = pts[0].pressure }
        )
    }

    // The strokePoints array will hold the points for the stroke.
    // Start it out with the first point, which needs no adjustment.
    val strokePoints: MutableList<StrokePoint> = mutableListOf(
        StrokePoint(
            point = Point(pts[0].x, pts[0].y),
            pressure = if (pts[0].pressure >= 0f) pts[0].pressure else 0.25f,
            vector = Point(1f, 1f),
            distance = 0f,
            runningLength = 0f
        )
    )

    // A flag to see whether we've already reached out minimum length
    var hasReachedMinimumLength = false

    // We use the runningLength to keep track of the total distance
    var runningLength = 0f

    // We're set this to the latest point, so we can use it to calculate
    // the distance and vector of the next point.
    var prev = strokePoints[0]

    val max = pts.size - 1

    // Iterate through all of the points, creating StrokePoints.
    for (i in pts.indices) {
        if (i == 0) continue
        val point =
            if (isComplete && i == max)
            // If we're at the last point, and `options.last` is true,
            // then add the actual input point.
                Point(pts[i].x, pts[i].y)
            else
            // Otherwise, using the t calculated from the streamline
            // option, interpolate a new point between the previous
            // point the current point.
                lrp(prev.point, pts[i], t)

        // If the new point is the same as the previous point, skip ahead.
        if (isEqual(prev.point, point)) continue

        // How far is the new point from the previous point?
        val distance = dist(point, prev.point)

        // Add this distance to the total "running length" of the line.
        runningLength += distance

        // At the start of the line, we wait until the new point is a
        // certain distance away from the original point, to avoid noise
        if (i < max && !hasReachedMinimumLength) {
            if (runningLength < size) continue
            hasReachedMinimumLength = true
            // TODO: Backfill the missing points so that tapering works correctly.
        }
        // Create a new strokepoint (it will be the new "previous" one).
        prev = StrokePoint(
            // The adjusted point
            point = point,
            // The input pressure (or .5 if not specified)
            pressure = if (pts[i].pressure >= 0f) pts[i].pressure else 0.5f,
            // The vector from the current point to the previous point
            vector = uni(sub(prev.point, point)),
            // The distance between the current point and the previous point
            distance = distance,
            // The total distance so far
            runningLength = runningLength,
        )

        // Push it to the strokePoints array.
        strokePoints.add(prev)
    }

    // Set the vector of the first point to be the same as the second point.
    strokePoints[0].vector = strokePoints.getOrNull(1)?.vector ?: Point(0f, 0f)

    return strokePoints
}


/**
 * Compute a radius based on the pressure.
 *
 * @param size - The base size.
 * @param thinning - Thinning factor.
 * @param pressure - The pressure value.
 * @param easing - Easing function (default is linear).
 * @return The computed radius.
 */
fun getStrokeRadius(
    size: Float,
    thinning: Float,
    pressure: Float,
    easing: (Float) -> Float = { it }
): Float {
    return size * easing(0.5f - thinning * (0.5f - pressure))
}


// This is the rate of change for simulated pressure. It could be an option.
const val RATE_OF_PRESSURE_CHANGE = 0.275f

// Browser strokes seem to be off if PI is regular, a tiny offset seems to fix it
const val FIXED_PI = PI.toFloat() + 0.0001f

/**
 * ## getStrokeOutlinePoints
 * @description Get an array of points (as `[x, y]`) representing the outline of a stroke.
 * @param points An array of StrokePoints as returned from `getStrokePoints`.
 * @param options (optional) An object with options.
 */
fun getStrokeOutlinePoints(
    points: List<StrokePoint>,
    options: StrokeOptions
): List<Point> {
    val size = options.size
    val smoothing = options.smoothing
    val thinning = options.thinning
    val simulatePressure = options.simulatePressure
    val easing = options.easing

    val start = options.start
    val end = options.end

    val isComplete = options.last

    val capStart = start.cap
    val capEnd = end.cap

    // We can't do anything with an empty array or a stroke with negative size.
    if (points.isEmpty() || size <= 0) {
        return emptyList()
    }

    // The total length of the line
    val totalLength = points[points.size - 1].runningLength

    val taperStart = start.taper
    val taperEnd = end.taper

    // The minimum allowed distance between points (squared)
    val minDistance = (size * smoothing).pow(2)

    // Our collected left and right points
    val leftPts: MutableList<Point> = mutableListOf()
    val rightPts: MutableList<Point> = mutableListOf()

    // Previous pressure (start with average of first five pressures,
    // in order to prevent fat starts for every line. Drawn lines
    // almost always start slow!
    var prevPressure = points.subList(0, 10).fold(points[0].pressure) { acc, curr ->
        var pressure = curr.pressure
        if (simulatePressure) {
            val sp = min(1f, curr.distance / size)
            val rp = min(1f, 1 - sp)
            pressure = min(1f, acc + (rp - acc) * (sp * RATE_OF_PRESSURE_CHANGE))
        }
        (acc + pressure) / 2
    }

    // The current radius
    var radius = getStrokeRadius(
        size,
        thinning,
        points[points.size - 1].pressure,
        easing
    )

    // The radius of the first saved point
    var firstRadius: Float? = null

    // Previous vector
    var prevVector = points[0].vector

    // Previous left and right points
    var pl = points[0].point
    var pr = pl

    // Temporary left and right points
    var tl = pl
    var tr = pr

    // Keep track of whether the previous point is a sharp corner
    // ... so that we don't detect the same corner twice
    var isPrevPointSharpCorner = false

    // let short = true

    /*
      Find the outline's left and right points

      Iterating through the points and populate the rightPts and leftPts arrays,
      skipping the first and last pointsm, which will get caps later on.
    */

    for (i in points.indices) {
        var pressure = points[i].pressure
        val point = points[i].point
        val vector = points[i].vector
        val distance = points[i].distance
        val runningLength = points[i].runningLength

        // Removes noise from the end of the line
        if (i < points.size - 1 && totalLength - runningLength < 3) {
            continue
        }

        /*
          Calculate the radius

          If not thinning, the current point's radius will be half the size; or
          otherwise, the size will be based on the current (real or simulated)
          pressure.
        */

        if (thinning > 0) {
            if (simulatePressure) {
                // If we're simulating pressure, then do so based on the distance
                // between the current point and the previous point, and the size
                // of the stroke. Otherwise, use the input pressure.
                val sp = min(1f, distance / size)
                val rp = min(1f, 1 - sp)
                pressure = min(
                    1f,
                    prevPressure + (rp - prevPressure) * (sp * RATE_OF_PRESSURE_CHANGE)
                )
            }

            radius = getStrokeRadius(size, thinning, pressure, easing)
        } else {
            radius = size / 2
        }

        if (firstRadius == null) {
            firstRadius = radius
        }

        /*
          Apply tapering

          If the current length is within the taper distance at either the
          start or the end, calculate the taper strengths. Apply the smaller
          of the two taper strengths to the radius.
        */

        val ts =
            if (runningLength < taperStart)
                start.easing(runningLength / taperStart)
            else 1f

        val te =
            if (totalLength - runningLength < taperEnd)
                end.easing((totalLength - runningLength) / taperEnd)
            else 1f

        radius = max(0.01f, radius * min(ts, te))

        /* Add points to left and right */

        /*
          Handle sharp corners

          Find the difference (dot product) between the current and next vector.
          If the next vector is at more than a right angle to the current vector,
          draw a cap at the current point.
        */

        val nextVector = (if (i < points.size - 1) points[i + 1] else points[i]).vector
        val nextDpr = if (i < points.size - 1) dpr(vector, nextVector) else 1f
        val prevDpr = dpr(vector, prevVector)

        val isPointSharpCorner = prevDpr < 0 && !isPrevPointSharpCorner
        val isNextPointSharpCorner = nextDpr != null && nextDpr < 0

        if (isPointSharpCorner || isNextPointSharpCorner) {
            // It's a sharp corner. Draw a rounded cap and move on to the next point
            // Considering saving these and drawing them later? So that we can avoid
            // crossing future points.

            val offset = mul(per(prevVector), radius)

            val step = 1f / 13f
            var t = 0f
            while (t <= 1) {
                tl = rotAround(sub(point, offset), point, FIXED_PI * t)
                leftPts.add(tl)

                tr = rotAround(add(point, offset), point, FIXED_PI * -t)
                rightPts.add(tr)

                t += step
            }

            pl = tl
            pr = tr

            if (isNextPointSharpCorner) {
                isPrevPointSharpCorner = true
            }
            continue
        }

        isPrevPointSharpCorner = false

        // Handle the last point
        if (i == points.size - 1) {
            val offset = mul(per(vector), radius)
            leftPts.add(sub(point, offset))
            rightPts.add(add(point, offset))
            continue
        }

        /*
          Add regular points

          Project points to either side of the current point, using the
          calculated size as a distance. If a point's distance to the
          previous point on that side greater than the minimum distance
          (or if the corner is kinda sharp), add the points to the side's
          points array.
        */

        val offset = mul(per(lrp(nextVector, vector, nextDpr)), radius)

        tl = sub(point, offset)

        if (i <= 1 || dist2(pl, tl) > minDistance) {
            leftPts.add(tl)
            pl = tl
        }

        tr = add(point, offset)

        if (i <= 1 || dist2(pr, tr) > minDistance) {
            rightPts.add(tr)
            pr = tr
        }

        // Set variables for next iteration
        prevPressure = pressure
        prevVector = vector
    }

    /*
      Drawing caps

      Now that we have our points on either side of the line, we need to
      draw caps at the start and end. Tapered lines don't have caps, but
      may have dots for very short lines.
    */

    val firstPoint = Point(points[0].point.x, points[0].point.y)

    val lastPoint =
        if (points.size > 1)
            Point(points[points.size - 1].point.x, points[points.size - 1].point.y)
        else add(points[0].point, Point(1f, 1f))

    val startCap: MutableList<Point> = mutableListOf()
    val endCap: MutableList<Point> = mutableListOf()

    /*
      Draw a dot for very short or completed strokes

      If the line is too short to gather left or right points and if the line is
      not tapered on either side, draw a dot. If the line is tapered, then only
      draw a dot if the line is both very short and complete. If we draw a dot,
      we can just return those points.
    */

    if (points.size == 1) {
        if (!(taperStart != 0f || taperEnd != 0f) || isComplete) {
            val start = prj(
                firstPoint,
                uni(per(sub(firstPoint, lastPoint))),
                -(firstRadius ?: radius)
            )
            val dotPts: MutableList<Point> = mutableListOf()

            val step = 1f / 13f
            var t = step
            while (t <= 1) {
                dotPts.add(rotAround(start, firstPoint, FIXED_PI * 2 * t))
                t += step
            }
            return dotPts
        }
    } else {
        /*
        Draw a start cap

        Unless the line has a tapered start, or unless the line has a tapered end
        and the line is very short, draw a start cap around the first point. Use
        the distance between the second left and right point for the cap's radius.
        Finally remove the first left and right points. :psyduck:
      */

        if (taperStart != 0f || (taperEnd != 0f && points.size == 1)) {
            // The start point is tapered, noop
        } else if (capStart) {
            // Draw the round cap - add thirteen points rotating the right point around the start point to the left point
            val step = 1f / 13f
            var t = step
            while (t <= 1) {
                val pt = rotAround(rightPts[0], firstPoint, FIXED_PI * t)
                startCap.add(pt)
                t += step
            }
        } else {
            // Draw the flat cap - add a point to the left and right of the start point
            val cornersVector = sub(leftPts[0], rightPts[0])
            val offsetA = mul(cornersVector, 0.5f)
            val offsetB = mul(cornersVector, 0.51f)

            startCap.addAll(
                mutableListOf(
                    sub(firstPoint, offsetA),
                    sub(firstPoint, offsetB),
                    add(firstPoint, offsetB),
                    add(firstPoint, offsetA)
                )
            )
        }

        /*
        Draw an end cap

        If the line does not have a tapered end, and unless the line has a tapered
        start and the line is very short, draw a cap around the last point. Finally,
        remove the last left and right points. Otherwise, add the last point. Note
        that This cap is a full-turn-and-a-half: this prevents incorrect caps on
        sharp end turns.
      */

        val direction = per(neg(points[points.size - 1].vector))

        if (taperEnd != 0f || (taperStart != 0f && points.size == 1)) {
            // Tapered end - push the last point to the line
            endCap.add(lastPoint)
        } else if (capEnd) {
            // Draw the round end cap
            val start = prj(lastPoint, direction, radius)

            val step = 1f / 29f
            var t = step
            while (t < 1) {
                endCap.add(rotAround(start, lastPoint, FIXED_PI * 3 * t))
                t += step
            }
        } else {
            // Draw the flat end cap

            endCap.addAll(
                mutableListOf(
                    add(lastPoint, mul(direction, radius)),
                    add(lastPoint, mul(direction, radius * 0.99f)),
                    sub(lastPoint, mul(direction, radius * 0.99f)),
                    sub(lastPoint, mul(direction, radius))
                )
            )
        }
    }

    /*
      Return the points in the correct winding order: begin on the left side, then
      continue around the end cap, then come back along the right side, and finally
      complete the start cap.
    */

    return leftPts.plus(endCap).plus(rightPts.reversed()).plus(startCap)
}


object Easing {
    val linear: ((t: Float) -> Float) = { t ->
        t
    }

    val easeInQuad: ((t: Float) -> Float) = { t ->
        t * t
    }
    val easeOutQuad: ((t: Float) -> Float) = { t ->
        1 - (1 - t).pow(2)
    }
    val easeInOutQuad: ((t: Float) -> Float) = { t ->
        if (t < 0.5) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2
    }

    val easeInCubic: ((t: Float) -> Float) = { t ->
        t * t * t
    }
    val easeOutCubic: ((t: Float) -> Float) = { t ->
        1 - (1 - t).pow(3)
    }
    val easeInOutCubic: ((t: Float) -> Float) = { t ->
        if (t < 0.5) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
    }

    val easeInQuart: ((t: Float) -> Float) = { t ->
        t * t * t * t
    }
    val easeOutQuart: ((t: Float) -> Float) = { t ->
        1 - (1 - t).pow(4)
    }
    val easeInOutQuart: ((t: Float) -> Float) = { t ->
        if (t < 0.5) 8 * t * t * t * t else 1 - (-2 * t + 2).pow(4) / 2
    }

    val easeInQuint: ((t: Float) -> Float) = { t ->
        t * t * t * t * t
    }
    val easeOutQuint: ((t: Float) -> Float) = { t ->
        1 - (1 - t).pow(5)
    }
    val easeInOutQuint: ((t: Float) -> Float) = { t ->
        if (t < 0.5) 16 * t * t * t * t * t else 1 - (-2 * t + 2).pow(5) / 2
    }

    val easeInSine: ((t: Float) -> Float) = { t ->
        1 - cos(t * PI.toFloat() / 2)
    }
    val easeOutSine: ((t: Float) -> Float) = { t ->
        sin(t * PI.toFloat() / 2)
    }
    val easeInOutSine: ((t: Float) -> Float) = { t ->
        -(cos(PI.toFloat() * t) - 1) / 2
    }

    val easeInExpo: ((t: Float) -> Float) = { t ->
        if (t <= 0) 0f else 2f.pow(10 * t - 10)
    }
    val easeOutExpo: ((t: Float) -> Float) = { t ->
        if (t >= 1) 1f else 1 - 2f.pow(-10 * t)
    }
    val easeInOutExpo: ((t: Float) -> Float) = { t ->
        if (t <= 0) 0f else if (t >= 1) 1f else if (t < 0.5f) 2f.pow(20 * t - 10) / 2 else (2 - 2f.pow(
            -20 * t + 10
        )) / 2
    }
}