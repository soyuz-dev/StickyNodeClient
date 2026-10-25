package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("path")
data class PathDto(
    override val id: String,
    override val x: Float,
    override val y: Float,
    override val style: StyleDto,
    val points: List<PointDto>
): NodeDto
