package org.stickynodes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PointDto(
    val x: Float,
    val y: Float,
)
