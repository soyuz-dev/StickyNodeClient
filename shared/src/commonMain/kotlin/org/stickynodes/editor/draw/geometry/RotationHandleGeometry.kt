
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText
import kotlin.uuid.Uuid


private fun Rect.rotationHandlePosition(
    rotation: Float,
    camera: EditorCamera,
    offset: Float,
): Offset {
    val center = camera.worldToScreen(center)

    val topCenter = camera.worldToScreen(
        Offset(this.center.x, top)
    ).rotatedAround(
        center = center,
        angle = rotation,
    )

    val direction = topCenter - center
    val distance = direction.getDistance()

    if (distance <= 0f) return topCenter

    return topCenter + direction / distance * offset
}

fun EditorShape.rotationHandlePosition(
    camera: EditorCamera,
    offset: Float,
): Offset = bounds.rotationHandlePosition(
    rotation = rotation,
    camera = camera,
    offset = offset,
)

fun EditorText.rotationHandlePosition(
    bounds: Rect,
    camera: EditorCamera,
    padding: Float,
    offset: Float,
): Offset = bounds.rotationHandlePosition(
    rotation = rotation,
    camera = camera,
    offset = padding + offset,
)


fun List<EditorNode>.groupRotationHandlePosition(
    selectedNodeIds: Set<Uuid>,
    camera: EditorCamera,
    textMeasurer: TextMeasurer,
    textStyle: (EditorText) -> TextStyle,
    padding: Float,
    offset: Float,
): Offset? {
    val bounds = asSequence()
        .filter { it.id in selectedNodeIds }
        .map {
            it.measuredSelectionBounds(
                textMeasurer = textMeasurer,
                textStyle = textStyle,
            )
        }
        .toList()
        .enclosingBounds()
        ?: return null

    val screenTop = camera.worldToScreen(
        Offset(
            x = bounds.center.x,
            y = bounds.top,
        )
    )

    return screenTop - Offset(
        x = 0f,
        y = padding + offset,
    )
}