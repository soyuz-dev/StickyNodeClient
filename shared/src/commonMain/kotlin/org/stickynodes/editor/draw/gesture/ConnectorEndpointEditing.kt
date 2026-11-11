
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.material3.FabPosition
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import org.stickynodes.editor.draw.geometry.connector.ConnectorAnchor
import org.stickynodes.editor.draw.geometry.connector.ConnectorHandle
import org.stickynodes.editor.draw.geometry.connector.isValidConnection
import org.stickynodes.editor.draw.geometry.connector.withEndpoint
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorConnector
import kotlin.uuid.Uuid

suspend fun AwaitPointerEventScope.handleConnectorEndpointDrag(
    down: PointerInputChange,
    connector: EditorConnector,
    handle: ConnectorHandle,
    endpointAt: (Offset) -> ConnectorEndpoint,
    onPreview: (EditorConnector?) -> Unit,
    onCommit: (EditorConnector) -> Unit,
    nodePosition: (Uuid, ConnectorAnchor?) -> Offset?,
) {
    var preview = connector

    fun updatePreview(position: Offset) {
        preview = connector.withEndpoint(
            handle = handle,
            endpoint = endpointAt(position)
        )

        onPreview(preview)
    }

    try {
        val dragStart = awaitTouchSlopOrCancellation(
            pointerId = down.id,
        ) { change, _ ->
            change.consume()
        } ?: return

        updatePreview(dragStart.position)

        val completed = drag(dragStart.id) { change ->
            updatePreview(change.position)
            change.consume()
        }

        if (
            completed &&
            preview != connector &&
            preview.isValidConnection(nodePosition)
        ) onCommit(preview)

    } finally {
        onPreview(null)
    }
}
