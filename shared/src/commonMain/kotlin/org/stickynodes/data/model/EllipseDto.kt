package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ellipse")
data class EllipseDto(
    override val id: String,
    override val x: Float,
    override val y: Float,
    override val style: StyleDto,
    val width: Float,
    val height: Float,
): NodeDto