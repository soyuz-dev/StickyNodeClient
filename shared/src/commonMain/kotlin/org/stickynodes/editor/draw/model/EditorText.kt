package org.stickynodes.editor.draw.model

import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

data class EditorText(
    override val id: Uuid,
    override val x: Float,
    override val y: Float,
    val text: String,
    val fontSize: Float = 16f,
    override val style: NodeStyle = NodeStyle(
        fill = FillStyle.Solid(0xFF0D0D0D.toInt()),
    ),
) : EditorNode {
    init {
        require(fontSize > 0f) {
            "Font size must be positive"
        }
    }
}