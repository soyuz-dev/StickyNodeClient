package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorShape

fun EditorShape.resizedTo(
    width: Float = bounds.width,
    height: Float = bounds.height,
): EditorShape {
    require(width.isFinite() && height.isFinite()) {
        "Shape dimensions must be finite"
    }
    require(width > 0f && height > 0f) {
        "Shape dimensions must be positive"
    }

    val original = bounds

    if (
        original.width == width &&
        original.height == height
    ) {
        return this
    }

    return withBounds(
        Rect(
            left = original.left,
            top = original.top,
            right = original.left + width,
            bottom = original.top + height
        )
    )
}