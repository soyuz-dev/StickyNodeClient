package org.stickynodes.editor.draw.model

import kotlin.uuid.Uuid

sealed interface EditorGroupMember {
    val id: Uuid

    fun flattenedNodes(): List<EditorNode> =
        when(this) {
            is EditorNode -> listOf(this)
            is EditorGroup -> members.flatMap { it.flattenedNodes() }
        }

    fun mapNodes(
        transform: (EditorNode) -> EditorNode
    ): EditorGroupMember = when(this) {
        is EditorNode -> transform(this)
        is EditorGroup -> {
            val updatedMembers = members.map { member ->
                member.mapNodes(transform)
            }

            if (updatedMembers == members) this
            else copy(members = updatedMembers)
        }
    }
}

fun List<EditorGroupMember>.mapNodes(
    transform: (EditorNode) -> EditorNode
): List<EditorGroupMember> = map { member ->
    member.mapNodes(transform)
}