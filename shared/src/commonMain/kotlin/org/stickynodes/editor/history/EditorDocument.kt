package org.stickynodes.editor.history

import org.stickynodes.editor.draw.model.EditorNode

data class EditorDocument(
    val nodes: List<EditorNode> = listOf()
)