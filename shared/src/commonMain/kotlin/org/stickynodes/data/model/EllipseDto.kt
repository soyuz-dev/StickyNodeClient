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
    override val width: Float,
    override val height: Float,
    override val rotation: Float = 0f,
): ShapeDto