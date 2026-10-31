package org.stickynodes.editor.draw.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.stickynodes.editor.draw.geometry.selectionBounds
import org.stickynodes.editor.draw.model.EditorNode

data class EditorCamera(
    val position: Offset = Offset.Zero,
    val zoom: Float = 1f,
) {
    fun worldToScreen(point: Offset): Offset =
        (point - position) * zoom

    fun screenToWorld(point: Offset): Offset =
        point / zoom + position

    fun zoomAt(
        screenPoint: Offset,
        newZoom: Float,
    ): EditorCamera {
        val zoom = newZoom.coerceIn(0.1f, 10f)
        val worldPoint = screenToWorld(screenPoint)
        return copy(
            position = worldPoint - screenPoint / zoom,
            zoom = zoom,
        )
    }


    fun fitToNodes(
        nodes: List<EditorNode>,
        viewport: IntSize,
        padding: Float = 64f,
    ): EditorCamera {
        if (nodes.isEmpty() || viewport.width <= 0 || viewport.height <= 0) {
            return this
        }

        val bounds = nodes.map { it.selectionBounds }

        val left = bounds.minOf { it.left }
        val top = bounds.minOf { it.top }
        val right = bounds.maxOf { it.right }
        val bottom = bounds.maxOf { it.bottom }

        val content = Rect(left, top, right, bottom)

        val availableWidth = (viewport.width - padding * 2).coerceAtLeast(1f)
        val availableHeight = (viewport.height - padding * 2).coerceAtLeast(1f)

        val fittedZoom = minOf(
            availableWidth / content.width.coerceAtLeast(1f),
            availableHeight / content.height.coerceAtLeast(1f),
        ).coerceIn(0.1f, 10f)

        val viewportCentre = Offset(
            viewport.width / 2f,
            viewport.height / 2f,
        )

        return copy(
            zoom = fittedZoom,
            position = content.center - viewportCentre / fittedZoom,
        )
    }

}