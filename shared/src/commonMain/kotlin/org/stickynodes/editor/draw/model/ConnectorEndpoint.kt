package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.connector.ConnectorAnchor
import kotlin.uuid.Uuid

sealed interface ConnectorEndpoint {
    data class Free(
        val position: Offset
    ): ConnectorEndpoint

    data class Attached(
        val nodeId: Uuid,
        val fallbackPosition: Offset,
        val anchor: ConnectorAnchor? = null
    ): ConnectorEndpoint

    fun resolve(
        nodePostion: (Uuid) -> Offset?,
    ): Offset = when (this) {
        is Free -> position
        is Attached ->
            nodePostion(nodeId) ?: fallbackPosition
    }

    fun resolve(
        nodePostion: (Uuid, ConnectorAnchor?) -> Offset?
    ): Offset = when (this) {
        is Free -> position
        is Attached -> nodePostion(nodeId, anchor) ?: fallbackPosition
    }
}