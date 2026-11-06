package org.stickynodes.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface MemberDto {
    val id: String
}