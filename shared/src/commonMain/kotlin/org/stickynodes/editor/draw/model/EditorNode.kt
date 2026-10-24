package org.stickynodes.editor.draw.model

import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

sealed interface EditorNode {
    val id: Uuid
    val x: Float
    val y: Float

    val style: NodeStyle
}