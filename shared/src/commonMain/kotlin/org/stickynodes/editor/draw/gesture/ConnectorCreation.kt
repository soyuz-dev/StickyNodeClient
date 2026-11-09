
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorConnector
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectConnectorCreation(
    endpointAt: (Offset) -> ConnectorEndpoint,
    onPreview: (EditorConnector?) -> Unit,
    onCommit: (EditorConnector) -> Unit,
) {
    var start: ConnectorEndpoint? = null
    var end: ConnectorEndpoint? = null
    var id: Uuid? = null

    fun buildConnector(): EditorConnector? {
        return EditorConnector(
            id = id ?: return null,
            start = start ?: return null,
            end = end ?: return null,
        )
    }

    fun reset() {
        start = null
        end = null
        id = null
        onPreview(null)
    }

    try {
        detectDragGestures(
            onDragStart = { position ->
                val endpoint = endpointAt(position)

                start = endpoint
                end = endpoint
                id = Uuid.random()

                onPreview(buildConnector())
            },

            onDrag = { change, _ ->
                end = endpointAt(change.position)
                onPreview(buildConnector())

                change.consume()
            },

            onDragEnd = {
                val connector = buildConnector()

                if (connector != null) {
                    val sameAttachment =
                        connector.start is ConnectorEndpoint.Attached &&
                                connector.end is ConnectorEndpoint.Attached &&
                                connector.start.nodeId == connector.end.nodeId

                    val samePosition =
                        connector.start.resolve { null } ==
                                connector.end.resolve { null }

                    if (!sameAttachment && !samePosition) {
                        onCommit(connector)
                    }
                }

                reset()
            },

            onDragCancel = ::reset,
        )
    } finally {
        reset()
    }
}
