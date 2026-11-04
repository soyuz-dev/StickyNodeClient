
package org.stickynodes.editor.draw.model

import kotlin.uuid.Uuid

fun List<EditorGroupMember>.flattenedMembers(): List<EditorGroupMember> =
    flatMap { member ->
        when (member) {
            is EditorNode -> listOf(member)
            is EditorGroup -> listOf(member) + member.members.flattenedMembers()
        }
    }

fun List<EditorGroupMember>.pathToMember(
    id: Uuid,
): List<EditorGroupMember>? {
    for (member in this) {
        if (member.id == id) {
            return listOf(member)
        }

        if (member is EditorGroup) {
            val childPath = member.members.pathToMember(id)

            if (childPath != null) {
                return listOf(member) + childPath
            }
        }
    }

    return null
}
