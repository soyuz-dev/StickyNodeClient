package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.stickynodes.editor.draw.geometry.connector.ConnectorAnchor

@Serializable
data class ConnectorDto(
    val id: String,
    val start: ConnectorEndpointDto,
    val end: ConnectorEndpointDto,
    val stroke: StrokeDto? = null,
)

@Serializable
sealed interface ConnectorEndpointDto{
    @Serializable
    @SerialName("free")
    data class Free(
        val position: PointDto,
    ): ConnectorEndpointDto

    @Serializable
    @SerialName("attached")
    data class Attached(
        val nodeId: String,
        val fallbackPosition: PointDto,
        val anchor: String? = null,
    ): ConnectorEndpointDto
}
