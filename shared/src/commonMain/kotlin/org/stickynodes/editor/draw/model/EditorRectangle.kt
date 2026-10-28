package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

data class EditorRectangle(
    override val id: Uuid = Uuid.random(),
    override val x: Float,
    override val y: Float,
    override val style: NodeStyle = NodeStyle(),
    override val width: Float,
    override val height: Float,
    override val rotation: Float = 0f,
) : EditorShape {
    override fun withBounds(bounds: Rect): EditorRectangle =
        copy(
            x = bounds.left,
            y = bounds.top,
            width = bounds.width,
            height = bounds.height,
        )

    override fun withRotation(rotation: Float): EditorShape =
        copy(rotation = rotation)
}