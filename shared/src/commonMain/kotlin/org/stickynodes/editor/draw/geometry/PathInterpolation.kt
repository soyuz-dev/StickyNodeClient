
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

fun smoothPath(points: List<Offset>): Path {
    val samples = points.filterIndexed { index, point ->
        index == 0 || point != points[index - 1]
    }

    return Path().apply {
        if (samples.isEmpty()) return@apply

        moveTo(samples.first().x, samples.first().y)

        if (samples.size == 1) return@apply

        if (samples.size == 2) {
            lineTo(samples.last().x, samples.last().y)
            return@apply
        }

        for (i in 0 until samples.lastIndex) {
            val p1 = samples[i]
            val p2 = samples[i + 1]

            // Extrapolate the endpoints for the first
            // and last segments.
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

            // Centripetal Catmull-Rom parameterisation:
            // deltaT = |deltaP| ^ 0.5
            val dt0 = sqrt((p1 - p0).getDistance())
            val dt1 = sqrt((p2 - p1).getDistance())
            val dt2 = sqrt((p3 - p2).getDistance())

            // Derivatives with respect to the
            // non-uniform Catmull-Rom parameter.
            val m1 =
                (p1 - p0) / dt0 -
                        (p2 - p0) / (dt0 + dt1) +
                        (p2 - p1) / dt1

            val m2 =
                (p2 - p1) / dt1 -
                        (p3 - p1) / (dt1 + dt2) +
                        (p3 - p2) / dt2

            // Convert endpoint derivatives to
            // cubic Bezier control points.
            val c1 = p1 + m1 * (dt1 / 3f)
            val c2 = p2 - m2 * (dt1 / 3f)

            cubicTo(
                c1.x, c1.y,
                c2.x, c2.y,
                p2.x, p2.y,
            )
        }
    }
}
