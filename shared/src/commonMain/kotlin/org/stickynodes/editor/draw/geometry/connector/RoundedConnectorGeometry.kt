package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.min

data class RoundedConnectorCorner(
    val entry: Offset,
    val control: Offset,
    val exit: Offset,
)

fun ConnectorRoute.roundedCorner(
    radius: Float
): RoundedConnectorCorner? {
    require(radius.isFinite() && radius >= 0f) {
        "Corner radius must be finite and non-negative"
    }

    if (points.size != 3 || radius == 0f) return null

    val start = points[0]
    val corner = points[1]
    val end = points[2]

    val incoming = corner - start
    val outgoing = end - corner

    val incomingLength = incoming.getDistance()
    val outgoingLength = outgoing.getDistance()


    if (
        incomingLength <= 0f ||
        outgoingLength <= 0f ||
        !incomingLength.isFinite() ||
        !outgoingLength.isFinite()
    ) return null

    val effectiveRadius = min(
        radius,
        min(
            incomingLength,
            outgoingLength
        )
    )

    val entry = corner -
            incoming / incomingLength * effectiveRadius

    val exit = corner +
            outgoing / outgoingLength * effectiveRadius

    return RoundedConnectorCorner(
        entry = entry,
        control = corner,
        exit = exit,
    )
}

fun ConnectorRoute.toPath(
    cornerRadius: Float = 1f,
): Path {
    val path = Path()
    val start = points.first()

    path.moveTo(start.x, start.y)

    val rounded = roundedCorner(cornerRadius)

    if (rounded == null) {
        for (point in points.drop(1)) {
            path.lineTo(point.x, point.y)
        }
    } else {
        path.lineTo(rounded.entry.x, rounded.entry.y)
        path.quadraticTo(
            rounded.control.x,
            rounded.control.y,
            rounded.exit.x,
            rounded.exit.y
        )

        val end = points.last()
        path.lineTo(end.x, end.y)
    }

    return path
}