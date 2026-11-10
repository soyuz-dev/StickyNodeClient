package org.stickynodes.editor.draw.geometry.connector

import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorConnector

fun EditorConnector.withEndpoint(
    handle: ConnectorHandle,
    endpoint: ConnectorEndpoint,
): EditorConnector = when (handle) {
    ConnectorHandle.START -> copy(start = endpoint)
    ConnectorHandle.END -> copy(end = endpoint)
}