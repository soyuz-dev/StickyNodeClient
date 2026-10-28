package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

data class EditorEllipse(
    override val id: Uuid = Uuid.random(),
    override val x: Float,
    override val y: Float,
    override val style: NodeStyle = NodeStyle(),
    override val width: Float,
    override val height: Float,
) : EditorShape {
    override fun withBounds(bounds: Rect): EditorEllipse =
        copy(
            x = bounds.left,
            y = bounds.top,
            width = bounds.width,
            height = bounds.height,
        )
}