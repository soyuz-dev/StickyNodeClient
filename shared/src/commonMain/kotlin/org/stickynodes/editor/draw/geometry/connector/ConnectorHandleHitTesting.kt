
package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isFinite

enum class ConnectorHandle {
    START,
    END,
}

fun ConnectorRoute.handleAt(
    position: Offset,
    radius: Float,
): ConnectorHandle? {
    require(position.isFinite) {
        "Pointer position must be finite"
    }
    require(radius.isFinite() && radius >= 0f) {
        "Handle radius must be finite and non-negative"
    }

    val radiusSquared = radius * radius

    val startDistanceSquared =
        (position - points.first()).getDistanceSquared()

    val endDistanceSquared =
        (position - points.last()).getDistanceSquared()

    val startHit = startDistanceSquared <= radiusSquared
    val endHit = endDistanceSquared <= radiusSquared

    return when {
        startHit && endHit ->
            if (startDistanceSquared <= endDistanceSquared) {
                ConnectorHandle.START
            } else {
                ConnectorHandle.END
            }

        startHit -> ConnectorHandle.START
        endHit -> ConnectorHandle.END
        else -> null
    }
}
