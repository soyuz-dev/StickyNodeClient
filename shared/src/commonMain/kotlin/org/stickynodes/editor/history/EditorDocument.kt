package org.stickynodes.editor.history

import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode

data class EditorDocument(
    val members: List<EditorGroupMember> = listOf(),
) {
    val nodes: List<EditorNode>
        get() = members.flatMap { it.flattenedNodes() }
}