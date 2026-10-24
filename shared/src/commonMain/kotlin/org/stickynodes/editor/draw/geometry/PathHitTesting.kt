
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.EditorPath
import kotlin.math.ceil

private fun distanceSquared(
    point: Offset,
    start: Offset,
    end: Offset,
): Float {
    val delta = end - start
    val lengthSquared = delta.x * delta.x + delta.y * delta.y

    if (lengthSquared == 0f) {
        val difference = point - start
        return difference.x * difference.x +
                difference.y * difference.y
    }

    val relative = point - start

    val t = (
            (relative.x * delta.x + relative.y * delta.y) /
                    lengthSquared
            ).coerceIn(0f, 1f)

    val closest = start + delta * t
    val difference = point - closest

    return difference.x * difference.x +
            difference.y * difference.y
}

fun EditorPath.containsStroke(
    point: Offset,
    zoom: Float = 1f,
    tolerancePx: Float = 6f,
): Boolean {
    if (zoom <= 0f || !zoom.isFinite()) return false

    val stroke = style.stroke ?: return false
    val radius = stroke.width / 2f + tolerancePx / zoom
    val radiusSquared = radius * radius

    // Convert the world-space pointer into local coordinates.
    val localPoint = point - Offset(x, y)

    if (points.isEmpty()) return false

    // Handle single-point strokes.
    if (points.size == 1) {
        val difference = localPoint - points.first()

        return difference.x * difference.x +
                difference.y * difference.y <= radiusSquared
    }

    for (segment in bezierSegments(points)) {
        // Approximate each Bezier segment using a polyline.
        // More subdivisions are used at higher zoom levels.
        val polygonLength =
            (segment.control1 - segment.start).getDistance() +
                    (segment.control2 - segment.control1).getDistance() +
                    (segment.end - segment.control2).getDistance()

        val steps = ceil(polygonLength * zoom / 4f)
            .toInt()
            .coerceIn(1, 256)

        var previous = segment.start

        for (i in 1..steps) {
            val current = segment.pointAt(i.toFloat() / steps)

            if (
                distanceSquared(
                    localPoint,
                    previous,
                    current,
                ) <= radiusSquared
            ) {
                return true
            }

            previous = current
        }
    }

    return false
}
