package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Rect


sealed interface EditorShape : EditorNode {
    val width: Float
    val height: Float

    val bounds: Rect
        get() = Rect(x, y, x + width, y + height)

    fun withBounds(bounds: Rect): EditorShape
}