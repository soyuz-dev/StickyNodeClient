package org.stickynodes.data.model

sealed interface ShapeDto : NodeDto {
    val width: Float
    val height: Float
    val rotation: Float
}