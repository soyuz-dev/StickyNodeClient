package org.stickynodes.editor.draw.camera

import androidx.compose.ui.geometry.Offset

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
}