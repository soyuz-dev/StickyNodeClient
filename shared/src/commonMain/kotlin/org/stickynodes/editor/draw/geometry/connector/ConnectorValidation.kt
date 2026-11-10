package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.TextEditSession
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorConnector
import kotlin.uuid.Uuid

fun EditorConnector.isValidConnection(
    nodePosition: (Uuid, ConnectorAnchor?) -> Offset?
): Boolean {
    val startAttachment = start as? ConnectorEndpoint.Attached
    val endAttachment = end as? ConnectorEndpoint.Attached

    if (
        startAttachment != null &&
        endAttachment != null &&
        startAttachment.nodeId == endAttachment.nodeId
    ) return false

    val startPosition = start.resolve(nodePosition)
    val endPosition = end.resolve(nodePosition)

    return startPosition != endPosition
}