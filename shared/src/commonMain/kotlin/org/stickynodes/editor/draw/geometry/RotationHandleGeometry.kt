
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.model.EditorShape

fun EditorShape.rotationHandlePosition(
    camera: EditorCamera,
    offset: Float,
): Offset {
    val center = bounds.center

    val topCenter = Offset(
        x = center.x,
        y = bounds.top,
    ).rotatedAround(
        center = center,
        angle = rotation,
    )

    val screenCenter = camera.worldToScreen(center)
    val screenTop = camera.worldToScreen(topCenter)

    val direction = screenTop - screenCenter
    val distance = direction.getDistance()

    if (distance <= 0f) return screenTop

    return screenTop + direction / distance * offset
}
