package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode

fun EditorGroupMember.translatedNodesBy(
    delta: Offset,
): List<EditorNode> {
    val nodes = flattenedNodes()

    if (delta == Offset.Zero) return nodes

    return nodes.map { node ->
        node.translatedBy(delta)
    }
}