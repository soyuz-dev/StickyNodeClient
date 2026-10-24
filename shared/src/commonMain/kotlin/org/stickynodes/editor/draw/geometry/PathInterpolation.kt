
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

data class BezierSegment(
    val start: Offset,
    val control1: Offset,
    val control2: Offset,
    val end: Offset,
) {
    fun pointAt(t: Float): Offset {
        val u = 1f - t

        return start * (u * u * u) +
                control1 * (3f * u * u * t) +
                control2 * (3f * u * t * t) +
                end * (t * t * t)
    }
}

fun bezierSegments(points: List<Offset>): List<BezierSegment> {
    val samples = points.filterIndexed { index, point ->
        index == 0 || point != points[index - 1]
    }

    if (samples.size < 2) return emptyList()

    if (samples.size == 2) {
        val start = samples[0]
        val end = samples[1]
        val delta = end - start

        return listOf(
            BezierSegment(
                start = start,
                control1 = start + delta / 3f,
                control2 = start + delta * (2f / 3f),
                end = end,
            )
        )
    }

    return (0 until samples.lastIndex).map { i ->
        val p1 = samples[i]
        val p2 = samples[i + 1]

        val p0 = if (i == 0) {
            p1 * 2f - p2
        } else {
            samples[i - 1]
        }

        val p3 = if (i + 2 >= samples.size) {
            p2 * 2f - p1
        } else {
            samples[i + 2]
        }

        val dt0 = sqrt((p1 - p0).getDistance())
        val dt1 = sqrt((p2 - p1).getDistance())
        val dt2 = sqrt((p3 - p2).getDistance())

        val m1 =
            (p1 - p0) / dt0 -
                    (p2 - p0) / (dt0 + dt1) +
                    (p2 - p1) / dt1

        val m2 =
            (p2 - p1) / dt1 -
                    (p3 - p1) / (dt1 + dt2) +
                    (p3 - p2) / dt2

        BezierSegment(
            start = p1,
            control1 = p1 + m1 * (dt1 / 3f),
            control2 = p2 - m2 * (dt1 / 3f),
            end = p2,
        )
    }
}

fun smoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply

    val segments = bezierSegments(points)

    if (segments.isEmpty()) {
        moveTo(points.first().x, points.first().y)
        return@apply
    }

    moveTo(segments.first().start.x, segments.first().start.y)

    for ((_, control1, control2, end) in segments) {
        cubicTo(
            control1.x,
            control1.y,
            control2.x,
            control2.y,
            end.x,
            end.y,
        )
    }
}
