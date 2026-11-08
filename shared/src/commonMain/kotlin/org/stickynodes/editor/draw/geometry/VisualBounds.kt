
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.geometry.transform.rotatedAround
import org.stickynodes.editor.draw.model.EditorShape

fun Rect.rotatedVisualBounds(rotation: Float): Rect {
    require(rotation.isFinite()) {
        "Rotation must be finite"
    }

    if (rotation == 0f) return this

    val corners = listOf(
        topLeft,
        Offset(right, top),
        bottomRight,
        Offset(left, bottom),
    ).map { point ->
        point.rotatedAround(
            center = center,
            angle = rotation,
        )
    }

    return Rect(
        left = corners.minOf { it.x },
        top = corners.minOf { it.y },
        right = corners.maxOf { it.x },
        bottom = corners.maxOf { it.y },
    )
}

val EditorShape.visualBounds: Rect
    get() = bounds.rotatedVisualBounds(rotation)

fun List<EditorShape>.combinedVisualBounds(): Rect? {
    if (isEmpty()) return null

    val bounds = map { it.visualBounds }

    return Rect(
        left = bounds.minOf { it.left },
        top = bounds.minOf { it.top },
        right = bounds.maxOf { it.right },
        bottom = bounds.maxOf { it.bottom },
    )
}
