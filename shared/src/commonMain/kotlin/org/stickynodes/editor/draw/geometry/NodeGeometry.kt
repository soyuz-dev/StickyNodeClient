
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText
import org.stickynodes.editor.draw.style.NodeStyle

val EditorNode.bounds: Rect
    get() = when (this) {

        is EditorShape -> this.bounds

        is EditorPath -> {
            val segments = bezierSegments(points)

            val positions = if (segments.isEmpty()) {
                points
            } else {
                segments.flatMap { segment ->
                    listOf(
                        segment.start,
                        segment.control1,
                        segment.control2,
                        segment.end,
                    )
                }
            }

            if (positions.isEmpty()) {
                Rect(x, y, x, y)
            } else {
                val strokeRadius = (style.stroke?.width ?: 0f) / 2f

                Rect(
                    left = x + positions.minOf { it.x } - strokeRadius,
                    top = y + positions.minOf { it.y } - strokeRadius,
                    right = x + positions.maxOf { it.x } + strokeRadius,
                    bottom = y + positions.maxOf { it.y } + strokeRadius,
                )
            }
        }

        // Accurate text bounds require a TextMeasurer.
        // Use measuredBounds() for text-aware geometry.
        is EditorText -> Rect(x, y, x, y)
    }

fun EditorNode.measuredBounds(
    textMeasurer: TextMeasurer,
    textStyle: (EditorText) -> TextStyle,
): Rect = when (this) {
    is EditorText -> measureBounds(
        textMeasurer = textMeasurer,
        style = textStyle(this),
    )

    else -> bounds
}


fun EditorNode.contains(
    point: Offset,
    zoom: Float = 1f,
): Boolean {
    val localPoint = if (this is EditorShape) {
        point.rotatedAround(
            center = bounds.center,
            angle = -rotation,
        )
    } else {
        point
    }

    return when (this) {
        is EditorRectangle -> bounds.contains(localPoint)

        is EditorEllipse -> {
            if (width <= 0f || height <= 0f) {
                false
            } else {
                val center = bounds.center

                val radiusX = width / 2f
                val radiusY = height / 2f

                val dx = (localPoint.x - center.x) / radiusX
                val dy = (localPoint.y - center.y) / radiusY

                dx * dx + dy * dy <= 1f
            }
        }

        is EditorPath -> containsStroke(point, zoom)

        // Text requires its measured bounds.
        is EditorText -> false
    }
}


fun EditorNode.contains(
    point: Offset,
    textMeasurer: TextMeasurer,
    textStyle: (EditorText) -> TextStyle,
    zoom: Float = 1f,
): Boolean = when (this) {
    is EditorText -> measuredBounds(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
    ).contains(point)

    else -> contains(point, zoom)
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

        is EditorText -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )
    }

fun EditorNode.withStyle(style: NodeStyle): EditorNode =
    when (this) {
        is EditorRectangle -> copy(style = style)
        is EditorEllipse -> copy(style = style)
        is EditorPath -> copy(style = style)
        is EditorText -> copy(style = style)
    }
