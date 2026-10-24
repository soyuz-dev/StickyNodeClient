package org.stickynodes.editor.draw.model

import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

data class EditorRectangle(
    override val id: Uuid = Uuid.random(),
    override val x: Float,
    override val y: Float,
    override val style: NodeStyle = NodeStyle(),
    val width: Float,
    val height: Float,
) : EditorNode