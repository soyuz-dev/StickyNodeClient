package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.transform.rotatedAround
import org.stickynodes.editor.draw.model.EditorShape

enum class ConnectorAnchor(
    val horizontalFraction: Float,
    val verticalFraction: Float,
) {
    NORTH_WEST(0f, 0f),
    NORTH(0.5f, 0f),
    NORTH_EAST(1f, 0f),
    EAST(1f, 0.5f),
    SOUTH_EAST(1f, 1f),
    SOUTH(0.5f, 1f),
    SOUTH_WEST(0f, 1f),
    WEST(0f, 0.5f),
}

fun EditorShape.anchorPosition(
    anchor: ConnectorAnchor,
): Offset {
    val unrotated = Offset(
        x = x + width * anchor.horizontalFraction,
        y = y + height * anchor.verticalFraction,
    )

    return unrotated.rotatedAround(
        center = bounds.center,
        angle = rotation,
    )
}