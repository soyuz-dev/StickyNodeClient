package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText

fun List<EditorNode>.orientedGroupBoundsOrNull(
    nodeBounds: (EditorNode) -> Rect,
): Rect? {
    val rotation = commonRotationOrNull() ?: return null

    val corners = flatMap { node ->
        val bounds = nodeBounds(node)

        val nodeRotation = when (node) {
            is EditorShape -> node.rotation
            is EditorText -> node.rotation
            else -> return null
        }

        listOf(
            bounds.topLeft,
            Offset(bounds.right, bounds.top),
            bounds.bottomRight,
            Offset(bounds.left, bounds.bottom),
        ).map { corner ->
            corner
                .rotatedAround(
                    center = bounds.center,
                    angle = nodeRotation,
                )
                .rotatedAround(
                    center = Offset.Zero,
                    angle = -rotation,
                )
        }
    }

    return Rect(
        left = corners.minOf { it.x },
        top = corners.minOf { it.y },
        right = corners.maxOf { it.x },
        bottom = corners.maxOf { it.y },
    )
}