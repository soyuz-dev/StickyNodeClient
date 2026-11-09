package org.stickynodes.editor.draw.model

import kotlin.uuid.Uuid

data class EditorConnector(
    val id: Uuid = Uuid.random(),
    val start: ConnectorEndpoint,
    val end: ConnectorEndpoint,
)