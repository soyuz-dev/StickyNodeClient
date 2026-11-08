
package org.stickynodes.editor.history

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.transform.duplicatedBy
import org.stickynodes.editor.draw.model.EditorGroup
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

class EditorNodeClipboard {
    private var copiedMembers: List<EditorGroupMember> = emptyList()
    private var pasteCount = 0

    val hasContent: Boolean
        get() = copiedMembers.isNotEmpty()

    fun copy(
        nodes: List<EditorNode>,
        selectedIds: Set<Uuid>,
    ) {
        copyMembers(nodes, selectedIds)
    }

    fun copyMembers(
        members: List<EditorGroupMember>,
        selectedIds: Set<Uuid>,
    ) {
        copiedMembers = members.filter { it.id in selectedIds }
        pasteCount = 0
    }

    fun paste(
        offset: Offset = Offset(20f, 20f),
    ): List<EditorNode> =
        pasteMembers(offset).filterIsInstance<EditorNode>()

    fun pasteMembers(
        offset: Offset = Offset(20f, 20f),
    ): List<EditorGroupMember> {
        if (copiedMembers.isEmpty()) return emptyList()

        pasteCount++

        val displacement = Offset(
            offset.x * pasteCount,
            offset.y * pasteCount,
        )

        return copiedMembers.map { member ->
            member.duplicatedBy(displacement)
        }
    }
}

private fun EditorGroupMember.duplicatedBy(
    displacement: Offset,
): EditorGroupMember = when (this) {
    is EditorNode -> duplicatedBy(displacement)

    is EditorGroup -> EditorGroup(
        members = members.map { member ->
            member.duplicatedBy(displacement)
        }
    )
}
