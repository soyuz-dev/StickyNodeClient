package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("text")
data class TextDto(
    override val id: String,
    override val x: Float,
    override val y: Float,
    override val style: StyleDto,
    val text: String,
    val fontSize: Float
) : NodeDto