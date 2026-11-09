package org.stickynodes.editor.draw.geometry.connector

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.EditorConnector
import kotlin.uuid.Uuid

fun EditorConnector.route(
    nodePosition: (Uuid) -> Offset?
): ConnectorRoute = routeConnector(
    start = start.resolve(nodePosition),
    end = end.resolve(nodePosition)
)