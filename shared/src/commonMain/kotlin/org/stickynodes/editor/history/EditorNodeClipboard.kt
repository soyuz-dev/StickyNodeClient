package org.stickynodes.editor.history

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.duplicatedBy
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

class EditorNodeClipboard {
    private var copiedNodes: List<EditorNode> = listOf()

    val hasContent: Boolean
        get() = copiedNodes.isNotEmpty()

    fun copy(
        nodes: List<EditorNode>,
        selectedIds: Set<Uuid>,
    ) {
        copiedNodes = nodes.filter { it.id in selectedIds }
    }

    fun paste(
        offset: Offset = Offset(20f, 20f),
    ): List<EditorNode> =
        copiedNodes.map { node ->
            node.duplicatedBy(offset)
        }
}