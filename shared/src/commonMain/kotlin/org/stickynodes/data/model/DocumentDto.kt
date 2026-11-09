package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentDto(
    val version: Int = 1,
    @SerialName("nodes")
    val members: List<MemberDto> = listOf(),
    val connectors: List<ConnectorDto> = listOf()
)