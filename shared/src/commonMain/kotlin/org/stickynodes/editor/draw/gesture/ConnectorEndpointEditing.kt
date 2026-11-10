
package org.stickynodes.editor.draw.gesture

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
    var moved = false
    var cancelled = false

    try {
        while (true) {
            val event = awaitPointerEvent()

            val change = event.changes.firstOrNull {
                it.id == down.id
            }

            if (change == null) {
                cancelled = true
                break
            }

            if (change.isConsumed) {
                cancelled = true
                break
            }

            if (!change.pressed) {
                break
            }

            if (change.position != down.position) {
                moved = true
            }

            if (!moved) {
                continue
            }

            preview = connector.withEndpoint(
                handle = handle,
                endpoint = endpointAt(change.position),
            )

            onPreview(preview)
            change.consume()
        }

        if (
            !cancelled &&
            moved &&
            preview != connector &&
            preview.isValidConnection(nodePosition)
        ) {
            onCommit(preview)
        }
    } finally {
        onPreview(null)
    }
}
