
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorShape

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

fun resizeBoundsProportionally(
    original: Rect,
    handle: ResizeHandle,
    delta: Offset,
    minimumSize: Float = 1f
): Rect {
    require(minimumSize > 0f)

    if(
        !(handle.movesLeft || handle.movesRight) ||
        !(handle.movesTop || handle.movesBottom)
    ) {
        return resizeBounds(original, handle, delta, minimumSize)
    }

    val width = original.width
    val height = original.height

    if (width <= 0f || height <= 0f) return original

    val direction = Offset(
        x = if (handle.movesRight) width else -width,
        y = if (handle.movesBottom) height else -height,
    )

    val scale = (
            1f + (
                    delta.x * direction.x +
                            delta.y * direction.y
                    ) / direction.getDistanceSquared()
            ).coerceAtLeast(
            maxOf(minimumSize / width, minimumSize / height)
        )

    val newWidth = width * scale
    val newHeight = height * scale

    return Rect(
        left = if (handle.movesLeft)
            original.right - newWidth
        else
            original.left,

        top = if (handle.movesTop)
            original.bottom - newHeight
        else
            original.top,

        right = if (handle.movesRight)
            original.left + newWidth
        else
            original.right,

        bottom = if (handle.movesBottom)
            original.top + newHeight
        else
            original.bottom,
    )
}

fun EditorShape.resizedBy(
    handle: ResizeHandle,
    delta: Offset,
    minimumSize: Float = 1f,
): EditorShape =
    withBounds(
        resizeBounds(
            original = bounds,
            handle = handle,
            delta = delta,
            minimumSize = minimumSize,
        )
    )

fun EditorShape.resizedProportionallyBy(
    handle: ResizeHandle,
    delta: Offset,
    minimumSize: Float = 1f,
): EditorShape =
    withBounds(
        resizeBoundsProportionally(
            original = bounds,
            handle = handle,
            delta = delta,
            minimumSize = minimumSize,
        )
    )

fun EditorShape.resizedRotatedBy(
    handle: ResizeHandle,
    delta: Offset,
    proportional: Boolean = false,
    minimumSize: Float = 1f
): EditorShape {
    val originalBounds = bounds

    val localDelta = delta.rotatedAround(
        center = Offset.Zero,
        angle = -rotation
    )

    val resizedBounds = if (proportional) {
        resizeBoundsProportionally(
            original = originalBounds,
            handle = handle,
            delta = localDelta,
            minimumSize = minimumSize,
        )
    } else {
        resizeBounds(
            original = originalBounds,
            handle = handle,
            delta = localDelta,
            minimumSize = minimumSize,
        )
    }

    val centerDelta = (resizedBounds.center - originalBounds.center)
        .rotatedAround(
            center = Offset.Zero,
            angle = rotation
        )

    val newCenter = originalBounds.center + centerDelta

    val newBounds = Rect(
        left = newCenter.x - resizedBounds.width / 2f,
        top = newCenter.y - resizedBounds.height / 2f,
        right = newCenter.x + resizedBounds.width / 2f,
        bottom = newCenter.y + resizedBounds.height / 2f,
    )

    return withBounds(newBounds)
}