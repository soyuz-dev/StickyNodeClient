package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.enclosingBounds
import org.stickynodes.editor.draw.geometry.measuredSelectionBounds
import org.stickynodes.editor.draw.geometry.selection.orientedShapeSelectionOrNull
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText
import kotlin.uuid.Uuid

private fun Rect.rotationHandlePosition(
    rotation: Float,
    camera: EditorCamera,
    offset: Float,
): Offset {
    val screenCenter = camera.worldToScreen(center)

    val screenTop = camera.worldToScreen(
        Offset(center.x, top),
    ).rotatedAround(
        center = screenCenter,
        angle = rotation,
    )

    val direction = screenTop - screenCenter
    val distance = direction.getDistance()

    if (distance <= 0f) return screenTop

    return screenTop + direction / distance * offset
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

/**
 * Calculates a rotation handle extending outward from the
 * north edge of an oriented selection rectangle.
 *
 * The rectangle is expressed in local coordinates.
 * Padding and offset are expressed in screen pixels.
 */
fun Rect.orientedRotationHandlePosition(
    rotation: Float,
    camera: EditorCamera,
    padding: Float,
    offset: Float,
): Offset {
    require(rotation.isFinite()) {
        "Rotation must be finite"
    }
    require(offset.isFinite() && offset >= 0f) {
        "Offset must be finite and non-negative"
    }

    val north = orientedResizeHandleScreenPositions(
        rotation = rotation,
        camera = camera,
        padding = padding,
    ).getValue(ResizeHandle.N)

    val outward = Offset(0f, -1f).rotatedAround(
        center = Offset.Zero,
        angle = rotation,
    )

    return north + outward * offset
}

/**
 * Returns the screen-space rotation handle for the selected nodes.
 *
 * Aligned shapes use an oriented bounding rectangle.
 * Other selections fall back to axis-aligned visual bounds.
 */
fun List<EditorNode>.groupRotationHandlePosition(
    selectedNodeIds: Set<Uuid>,
    camera: EditorCamera,
    textMeasurer: TextMeasurer,
    textStyle: (EditorText) -> TextStyle,
    padding: Float,
    offset: Float,
    useOrientedBounds: Boolean = true,
): Offset? {
    val selected = filter { it.id in selectedNodeIds }
    if (selected.isEmpty()) return null

    if (useOrientedBounds) {
        val oriented = selected.orientedShapeSelectionOrNull()

        if (oriented != null) {
            return oriented.bounds.orientedRotationHandlePosition(
                rotation = oriented.rotation,
                camera = camera,
                padding = padding,
                offset = offset,
            )
        }
    }

    val bounds = selected.map { node ->
        node.measuredSelectionBounds(
            textMeasurer = textMeasurer,
            textStyle = textStyle,
        )
    }.enclosingBounds() ?: return null

    val screenTop = camera.worldToScreen(
        Offset(bounds.center.x, bounds.top),
    )

    return screenTop - Offset(
        x = 0f,
        y = padding + offset,
    )
}
