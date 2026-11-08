package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import org.stickynodes.editor.draw.geometry.path.containsStroke
import org.stickynodes.editor.draw.geometry.transform.rotatedAround
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText

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

        is EditorText -> false
    }
}

fun EditorNode.contains(
    point: Offset,
    textMeasurer: TextMeasurer,
    textStyle: (EditorText) -> TextStyle,
    zoom: Float = 1f,
): Boolean = when (this) {
    is EditorText -> {
        val layoutBounds = measuredBounds(
            textMeasurer = textMeasurer,
            textStyle = textStyle,
        )

        val localPoint = point.rotatedAround(
            center = layoutBounds.center,
            angle = -rotation,
        )

        layoutBounds.contains(localPoint)
    }

    else -> contains(point, zoom)
}