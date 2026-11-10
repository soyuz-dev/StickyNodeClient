
package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape

fun connectorEndpointAt(
    position: Offset,
    nodes: List<EditorNode>,
    hitTest: (EditorNode, Offset) -> Boolean,
    bounds: (EditorNode) -> Rect,
    snapRadius: Float = 0f,
): ConnectorEndpoint {
    require(snapRadius.isFinite() && snapRadius >= 0f)

    // Prioritise the topmost object under the pointer.
    val target = nodes.asReversed().firstOrNull {
        hitTest(it, position)
    }

    if (target != null) {
        if (target is EditorShape) {
            val anchor = ConnectorAnchor.entries.minBy {
                (target.anchorPosition(it) - position)
                    .getDistanceSquared()
            }

            return ConnectorEndpoint.Attached(
                nodeId = target.id,
                fallbackPosition = target.anchorPosition(anchor),
                anchor = anchor,
            )
        }

        // Preserve centre attachments for non-shape nodes.
        return ConnectorEndpoint.Attached(
            nodeId = target.id,
            fallbackPosition = bounds(target).center,
        )
    }

    // Allow snapping just outside a shape as well.
    val radiusSquared = snapRadius * snapRadius

    val nearest = nodes
        .filterIsInstance<EditorShape>()
        .flatMap { shape ->
            ConnectorAnchor.entries.map { anchor ->
                val point = shape.anchorPosition(anchor)
                Triple(
                    shape,
                    anchor,
                    point,
                )
            }
        }
        .minByOrNull { (_, _, point) ->
            (point - position).getDistanceSquared()
        }

    if (nearest != null) {
        val (shape, anchor, point) = nearest

        if (
            (point - position).getDistanceSquared() <=
            radiusSquared
        ) {
            return ConnectorEndpoint.Attached(
                nodeId = shape.id,
                fallbackPosition = point,
                anchor = anchor,
            )
        }
    }

    return ConnectorEndpoint.Free(position)
}