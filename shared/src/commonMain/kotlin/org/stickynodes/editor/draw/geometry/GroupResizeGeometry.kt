package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorShape

fun List<EditorShape>.combinedBounds(): Rect? {
    if (isEmpty()) return null

    return Rect(
        left = minOf { it.bounds.left },
        top = minOf { it.bounds.top },
        right = maxOf { it.bounds.right },
        bottom = maxOf { it.bounds.bottom }
    )
}

fun EditorShape.scaledBetween(
    source: Rect,
    target: Rect,
): EditorShape {
    require(source.width > 0f && source.height > 0f)

    val scaleX = target.width / source.width
    val scaleY = target.height / source.height
    val original = bounds

    return withBounds(
        Rect(
            left = target.left + (original.left - source.left) * scaleX,
            top = target.top + (original.top - source.top) * scaleY,
            right = target.left + (original.right - source.left) * scaleX,
            bottom = target.top + (original.bottom - source.top) * scaleY,
        )
    )
}


fun List<EditorShape>.resizedTogether(
    handle: ResizeHandle,
    delta: Offset,
    proportional: Boolean = false,
    minimumSize: Float = 1f,
): List<EditorShape> {
    require(minimumSize > 0f)

    if (isEmpty()) return emptyList()

    val source = combinedBounds() ?: return this
    if (source.width <= 0f || source.height <= 0f) return this

    // All shapes must have positive dimensions.
    require(all { it.bounds.width > 0f && it.bounds.height > 0f })

    val minimumScaleX = maxOf {
        minimumSize / it.bounds.width
    }

    val minimumScaleY = maxOf {
        minimumSize / it.bounds.height
    }

    val minimumWidth = source.width * minimumScaleX
    val minimumHeight = source.height * minimumScaleY

    val target = if (proportional) {
        resizeBoundsProportionally(
            original = source,
            handle = handle,
            delta = delta,
            minimumSize = maxOf(minimumWidth, minimumHeight),
        )
    } else {
        resizeBounds(
            original = source,
            handle = handle,
            delta = delta,
            minimumSize = 0.0001f,
        ).let { resized ->
            val correctedWidth = maxOf(
                resized.width,
                minimumWidth,
            )

            val correctedHeight = maxOf(
                resized.height,
                minimumHeight,
            )

            Rect(
                left = if (handle.movesLeft)
                    resized.right - correctedWidth
                else resized.left,

                top = if (handle.movesTop)
                    resized.bottom - correctedHeight
                else resized.top,

                right = if (handle.movesRight)
                    resized.left + correctedWidth
                else resized.right,

                bottom = if (handle.movesBottom)
                    resized.top + correctedHeight
                else resized.bottom,
            )
        }
    }

    return map { it.scaledBetween(source, target) }
}
