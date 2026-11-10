package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorConnector
import kotlin.uuid.Uuid

fun ConnectorEndpoint.detachIfRemoved(
    removedNodeIds: Set<Uuid>,
    resolvePosition: (ConnectorEndpoint.Attached) -> Offset?,
): ConnectorEndpoint {
    if (this !is ConnectorEndpoint.Attached) return this
    if (nodeId !in removedNodeIds) return this

    return ConnectorEndpoint.Free(
        position = resolvePosition(this) ?: fallbackPosition
    )
}

fun List<EditorConnector>.detachRemovedNodes(
    removedNodeIds: Set<Uuid>,
    resolvePosition: (ConnectorEndpoint.Attached) -> Offset?
): List<EditorConnector> {
    if (removedNodeIds.isEmpty()) return this

    return map { connector ->
        connector.copy(
            start = connector.start.detachIfRemoved(
                removedNodeIds,
                resolvePosition,
            ),
            end = connector.end.detachIfRemoved(
                removedNodeIds,
                resolvePosition,
            ),
        )
    }
}