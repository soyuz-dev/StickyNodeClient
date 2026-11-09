package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorNode

fun connectorEndpointAt(
    position: Offset,
    nodes: List<EditorNode>,
    hitTest: (EditorNode, Offset) -> Boolean,
    bounds: (EditorNode) -> Rect
): ConnectorEndpoint {
    val target = nodes.asReversed().firstOrNull { node ->
        hitTest(node, position)
    } ?: return ConnectorEndpoint.Free(position)

    return ConnectorEndpoint.Attached(
        nodeId = target.id,
        fallbackPosition = bounds(target).center
    )
}