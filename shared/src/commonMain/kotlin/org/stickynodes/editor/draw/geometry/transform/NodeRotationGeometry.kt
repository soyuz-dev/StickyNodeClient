package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText

fun List<EditorNode>.rotatedNodesTogether(
    angle: Float,
    center: Offset,
    textBounds: (EditorText) -> Rect,
): List<EditorNode> {
    require(angle.isFinite()) {
        "Rotation angle must be finite"
    }
    require(center.x.isFinite() && center.y.isFinite()) {
        "Rotation centre must be finite"
    }

    if (angle == 0f) return this

    return map { node ->
        when (node) {
            is EditorShape -> {
                listOf(node)
                    .rotatedTogether(angle, center)
                    .single()
            }

            is EditorText -> {
                val originalCenter = textBounds(node).center
                val rotatedCenter = originalCenter.rotatedAround(
                    center = center,
                    angle = angle,
                )
                val displacement = rotatedCenter - originalCenter

                node.copy(
                    x = node.x + displacement.x,
                    y = node.y + displacement.y,
                    rotation = node.rotation + angle,
                )
            }

            is EditorPath -> {
                val originalOrigin = Offset(node.x, node.y)
                val rotatedOrigin = originalOrigin.rotatedAround(
                    center = center,
                    angle = angle,
                )

                node.copy(
                    x = rotatedOrigin.x,
                    y = rotatedOrigin.y,
                    points = node.points.map { point ->
                        point.rotatedAround(
                            center = Offset.Zero,
                            angle = angle,
                        )
                    },
                )
            }
        }
    }
}
