package org.stickynodes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("group")
data class GroupDto(
    override val id: String,
    val members: List<MemberDto>
): MemberDto
