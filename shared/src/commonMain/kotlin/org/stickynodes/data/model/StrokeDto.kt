package org.stickynodes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StrokeDto(
    val color: Int,
    val width: Float,
    val cap: String,
    val join: String,
    val dashPattern: List<Float>? = null,
)
