package org.stickynodes.editor.draw.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.combinedVisualBounds
import org.stickynodes.editor.draw.geometry.layoutStyle
import org.stickynodes.editor.draw.geometry.measuredBounds
import org.stickynodes.editor.draw.geometry.resizeHandleScreenPositions
import org.stickynodes.editor.draw.geometry.rotationHandleScreenPosition
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import kotlin.math.PI

fun DrawScope.drawSelection(
    node: EditorNode,
    color: Color,
    textMeasurer: TextMeasurer,
) {
    val outline = node.measuredBounds(
        textMeasurer = textMeasurer,
        textStyle = { text -> text.layoutStyle(this) },
    ).inflate(3f)

    drawRect(
        color = color,
        topLeft = outline.topLeft,
        size = outline.size,
        style = Stroke(width = 2f),
    )
}


fun DrawScope.drawShapeSelection(
    shapes: List<EditorShape>,
    camera: EditorCamera,
    color: Color,
) {
    if (shapes.isEmpty()) return

    val singleShape = shapes.singleOrNull()
    val bounds = singleShape?.bounds
        ?: shapes.combinedVisualBounds()
        ?: return

    val screenBounds = Rect(
        camera.worldToScreen(bounds.topLeft),
        camera.worldToScreen(
            Offset(bounds.right, bounds.bottom),
        ),
    ).inflate(8.dp.toPx())

    val outlineWidth = 1.5.dp.toPx()
    val cornerRadius = CornerRadius(6.dp.toPx())

    fun drawOutline() {
        drawRoundRect(
            color = color,
            topLeft = screenBounds.topLeft,
            size = screenBounds.size,
            cornerRadius = cornerRadius,
            style = Stroke(width = outlineWidth),
        )
    }

    if (singleShape != null && singleShape.rotation != 0f) {
        rotate(
            degrees = singleShape.rotation * 180f / PI.toFloat(),
            pivot = camera.worldToScreen(singleShape.bounds.center),
        ) {
            drawOutline()
        }
    } else {
        drawOutline()
    }

    val canResize = singleShape != null ||
            shapes.all { it.rotation == 0f }

    if (canResize) {
        val handleSize = 10.dp.toPx()
        val handleCornerRadius = CornerRadius(3.dp.toPx())

        for (position in shapes.resizeHandleScreenPositions(
            camera = camera,
            padding = 8.dp.toPx(),
        ).values) {
            val topLeft = position - Offset(
                handleSize / 2f,
                handleSize / 2f,
            )

            drawRoundRect(
                color = Color.White,
                topLeft = topLeft,
                size = Size(handleSize, handleSize),
                cornerRadius = handleCornerRadius,
            )

            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(handleSize, handleSize),
                cornerRadius = handleCornerRadius,
                style = Stroke(width = outlineWidth),
            )
        }
    }

    shapes.rotationHandleScreenPosition(
        camera = camera,
        offset = 28.dp.toPx(),
    )?.let { position ->
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = position,
        )

        drawCircle(
            color = color,
            radius = 7.dp.toPx(),
            center = position,
            style = Stroke(width = outlineWidth),
        )
    }
}

