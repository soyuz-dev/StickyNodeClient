package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle

fun EditorNode.contains(point: Offset): Boolean =
    when (this) {
        is EditorRectangle -> bounds.contains(point)
    }

val EditorNode.bounds: Rect
    get() = when (this) {
        is EditorRectangle -> Rect(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height,
        )
    }