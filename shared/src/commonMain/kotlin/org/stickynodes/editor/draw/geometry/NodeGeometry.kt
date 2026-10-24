package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.style.NodeStyle

val EditorNode.bounds: Rect
    get() = when (this) {
        is EditorRectangle -> Rect(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height,
        )
        is EditorEllipse -> Rect(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height,
        )
        is EditorPath -> {
            val left = points.minOfOrNull { it.x } ?: 0f
            val top = points.minOfOrNull { it.y } ?: 0f
            val right = points.maxOfOrNull { it.x } ?: 0f
            val bottom = points.maxOfOrNull { it.y } ?: 0f

            Rect(
                x + left,
                y + top,
                x + right,
                y + bottom,
            )
        }
    }

fun EditorNode.contains(point: Offset): Boolean =
    when (this) {
        is EditorRectangle -> bounds.contains(point)
        is EditorEllipse -> {
            if (width <= 0f || height <= 0f) {
                false
            } else {
                val centreX = x + width / 2f
                val centreY = y + height / 2f

                val radiusX = width / 2f
                val radiusY = height / 2f

                val dx = (point.x - centreX) / radiusX
                val dy = (point.y - centreY) / radiusY

                dx * dx + dy * dy <= 1f
            }
        }
        is EditorPath -> false
    }

fun EditorNode.translatedBy(delta: Offset): EditorNode =
    when (this) {
        is EditorRectangle -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )
        is EditorEllipse -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )
        is EditorPath -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )
    }

fun EditorNode.withStyle(style: NodeStyle): EditorNode =
    when (this) {
        is EditorRectangle -> copy(style = style)
        is EditorEllipse -> copy(style = style)
        is EditorPath -> copy(style = style)
    }