package org.stickynodes.editor.draw.model

import org.stickynodes.editor.draw.style.StrokeStyle
import kotlin.uuid.Uuid

val DefaultConnectorStroke = StrokeStyle(
    color = 0xFF26A69A.toInt(),
    width = 2.5f,
)

data class EditorConnector(
    val id: Uuid = Uuid.random(),
    val start: ConnectorEndpoint,
    val end: ConnectorEndpoint,
    val stroke: StrokeStyle = DefaultConnectorStroke
)