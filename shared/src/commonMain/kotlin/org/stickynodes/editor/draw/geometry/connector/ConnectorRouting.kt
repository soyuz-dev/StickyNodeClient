package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isFinite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

private const val ANGLE_STEP = PI / 4.0
private const val SNAP_TOLERANCE = 0.03
private const val PARALLEL_EPSILON = 1e-10

data class ConnectorRoute(
    val points: List<Offset>
) {
    init {
        require(points.size in 2..3) {
            "A connector route must contain two or three points"
        }
        require(points.all { it.isFinite }) {
            "Connector route points must be finite"
        }
    }
}

fun routeConnector(
    start: Offset,
    end: Offset,
): ConnectorRoute {
    require(start.isFinite && end.isFinite) {
        "Start and End positions must be finite"
    }

    val dx = (end.x - start.x).toDouble()
    val dy = (end.y - start.y).toDouble()

    if (dx == 0.0 && dy == 0.0) {
        return ConnectorRoute(listOf(start, end))
    }

    val angle = atan2(dy, dx)
    val snapped = round(angle / ANGLE_STEP) * ANGLE_STEP

    if (abs(angle - snapped) < SNAP_TOLERANCE) {
        return ConnectorRoute(listOf(start, end))
    }

    val startAngle = floor(angle / ANGLE_STEP) * ANGLE_STEP
    val endAngle = ceil((angle + PI) / ANGLE_STEP) * ANGLE_STEP

    val ux = cos(startAngle)
    val uy = sin(startAngle)
    val vx = cos(endAngle)
    val vy = sin(endAngle)

    val determinant = ux * vy - uy * vx

    if (abs(determinant) < PARALLEL_EPSILON) {
        return ConnectorRoute(listOf(start, end))
    }

    val t = (dx * vy - dy * vx) / determinant

    val corner = Offset(
        x = (start.x.toDouble() + ux * t).toFloat(),
        y = (start.y.toDouble() + uy * t).toFloat(),
    )

    if (!corner.x.isFinite() || !corner.y.isFinite()) {
        return ConnectorRoute(listOf(start, end))
    }

    return ConnectorRoute(listOf(start, corner, end))
}