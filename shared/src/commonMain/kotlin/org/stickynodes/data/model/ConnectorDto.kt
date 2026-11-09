package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectorDto(
    val id: String,
    val start: ConnectorEndpointDto,
    val end: ConnectorEndpointDto,
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
    ): ConnectorEndpointDto
}
