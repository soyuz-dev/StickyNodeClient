
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle

fun resizeBounds(
    original: Rect,
    handle: ResizeHandle,
    delta: Offset,
    minimumSize: Float = 1f,
): Rect {
    require(minimumSize > 0f)

    val left = if (handle.movesLeft) {
        (original.left + delta.x)
            .coerceAtMost(original.right - minimumSize)
    } else {
        original.left
    }

    val top = if (handle.movesTop) {
        (original.top + delta.y)
            .coerceAtMost(original.bottom - minimumSize)
    } else {
        original.top
    }

    val right = if (handle.movesRight) {
        (original.right + delta.x)
            .coerceAtLeast(original.left + minimumSize)
    } else {
        original.right
    }

    val bottom = if (handle.movesBottom) {
        (original.bottom + delta.y)
            .coerceAtLeast(original.top + minimumSize)
    } else {
        original.bottom
    }

    return Rect(left, top, right, bottom)
}

fun EditorNode.withBounds(bounds: Rect): EditorNode =
    when (this) {
        is EditorRectangle -> copy(
            x = bounds.left,
            y = bounds.top,
            width = bounds.width,
            height = bounds.height,
        )

        is EditorEllipse -> copy(
            x = bounds.left,
            y = bounds.top,
            width = bounds.width,
            height = bounds.height,
        )

        else -> this
    }

fun EditorNode.resizedBy(
    handle: ResizeHandle,
    delta: Offset,
    minimumSize: Float = 1f,
): EditorNode =
    withBounds(
        resizeBounds(
            original = bounds,
            handle = handle,
            delta = delta,
            minimumSize = minimumSize,
        )
    )
