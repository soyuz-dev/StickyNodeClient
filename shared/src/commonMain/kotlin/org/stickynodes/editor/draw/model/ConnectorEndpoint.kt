package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Offset
import kotlin.uuid.Uuid

sealed interface ConnectorEndpoint {
    data class Free(
        val position: Offset
    ): ConnectorEndpoint

    data class Attached(
        val nodeId: Uuid,
        val fallbackPosition: Offset
    ): ConnectorEndpoint

    fun resolve(
        nodePostion: (Uuid) -> Offset?,
    ): Offset = when (this) {
        is Free -> position
        is Attached ->
            nodePostion(nodeId) ?: fallbackPosition
    }
}