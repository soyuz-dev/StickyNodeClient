package org.stickynodes.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface NodeDto : MemberDto {
    val x: Float
    val y: Float
    val style: StyleDto
}