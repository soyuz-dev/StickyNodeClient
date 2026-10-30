package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorShape

val EditorShape.visualBounds: Rect
    get() {
        val center = bounds.center

        val corners = listOf(
            bounds.topLeft,
            Offset(bounds.right, bounds.top),
            bounds.bottomRight,
            Offset(bounds.left, bounds.bottom)
        ).map { point ->
            point.rotatedAround(
                center = center,
                angle = rotation
            )
        }

        return Rect(
            left = corners.minOf { it.x },
            top = corners.minOf { it.y },
            right = corners.maxOf { it.x },
            bottom = corners.maxOf { it.y },
        )
    }

fun List<EditorShape>.combinedVisualBounds(): Rect? {
    if(isEmpty()) return null

    val bounds = map { it.visualBounds }

    return Rect(
        left = bounds.minOf { it.left },
        top = bounds.minOf { it.top },
        right = bounds.maxOf { it.right },
        bottom = bounds.maxOf { it.bottom },
    )
}