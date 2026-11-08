package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import org.stickynodes.editor.draw.geometry.path.bezierSegments
import org.stickynodes.editor.draw.geometry.text.measureBounds
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText

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
                val strokeRadius =
                    (style.stroke?.width ?: 0f) / 2f

                Rect(
                    left = x + positions.minOf { it.x } - strokeRadius,
                    top = y + positions.minOf { it.y } - strokeRadius,
                    right = x + positions.maxOf { it.x } + strokeRadius,
                    bottom = y + positions.maxOf { it.y } + strokeRadius,
                )
            }
        }

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

fun EditorNode.measuredSelectionBounds(
    textMeasurer: TextMeasurer,
    textStyle: (EditorText) -> TextStyle,
): Rect = when (this) {
    is EditorShape -> visualBounds

    is EditorText -> measuredBounds(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
    ).rotatedVisualBounds(rotation)

    else -> bounds
}

val EditorNode.selectionBounds: Rect
    get() = when (this) {
        is EditorShape -> visualBounds
        else -> bounds
    }