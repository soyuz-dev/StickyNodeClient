package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

fun EditorCamera.visibleWorldBounds(
    viewport: IntSize
): Rect {
    return Rect(
        left = position.x,
        top = position.y,
        right = position.x + viewport.width / zoom,
        bottom = position.y + viewport.height / zoom,
    )
}

fun List<EditorNode>.visibleNodeIds(
    viewportBounds: Rect,
    selectionBounds: (EditorNode) -> Rect,
): Set<Uuid> = asSequence()
    .filter { node ->
        val bounds = selectionBounds(node)

        bounds.left <= viewportBounds.right &&
                bounds.right >= viewportBounds.left &&
                bounds.top <= viewportBounds.bottom &&
                bounds.bottom >= viewportBounds.top
    }
    .map { it.id }
    .toSet()