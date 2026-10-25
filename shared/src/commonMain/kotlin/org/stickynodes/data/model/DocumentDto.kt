package org.stickynodes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DocumentDto(
    val version: Int = 1,
    val nodes: List<NodeDto> = listOf(),
)