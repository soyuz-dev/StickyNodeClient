package org.stickynodes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StyleDto(
    val fillColor: Int? = null,
    val stroke: StrokeDto? = null,
)