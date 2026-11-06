package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape

data class OrientedShapeSelection(
    val bounds: Rect,
    val rotation: Float,
)

fun List<EditorNode>.orientedShapeSelectionOrNull():
        OrientedShapeSelection? {

    if (isEmpty() || any { it !is EditorShape }) {
        return null
    }

    val rotation = commonRotationOrNull() ?: return null

    val bounds = orientedGroupBoundsOrNull { node ->
        (node as EditorShape).bounds
    } ?: return null

    return OrientedShapeSelection(
        bounds = bounds,
        rotation = rotation,
    )
}