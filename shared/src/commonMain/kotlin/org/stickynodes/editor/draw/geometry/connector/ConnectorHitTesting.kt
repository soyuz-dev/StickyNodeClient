package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset

fun distanceToSegment(
    point: Offset,
    start: Offset,
    end: Offset,
): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y

    val lengthSq = dx * dx + dy * dy

    if (lengthSq == 0f) {
        return (point - start).getDistance()
    }

    val projection = (
            (point.x - start.x) * dx +
            (point.y - start.y) * dy
    ) / lengthSq

    val t = projection.coerceIn(0f, 1f)

    val nearest = Offset(
        x = start.x + dx * t,
        y = start.y + dy * t,
    )

    return (point - nearest).getDistance()
}

fun ConnectorRoute.distanceTo(
    point: Offset,
): Float = points.zipWithNext()
    .minOf { (start, end) ->
        distanceToSegment(point, start, end)
    }

fun ConnectorRoute.contains(
    point: Offset,
    tolerance: Float,
): Boolean {
    require(tolerance.isFinite() && tolerance >= 0f) {
        "Tolerance must be finite and non-negative"
    }

    return distanceTo(point) <= tolerance
}