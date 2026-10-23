package org.stickynodes.editor.draw.gesture

import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.camera.EditorCamera
import kotlin.math.exp

suspend fun PointerInputScope.detectCanvasZoom(
    camera: () -> EditorCamera,
    onCameraChanged: (EditorCamera) -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()

            if (event.type != PointerEventType.Scroll) continue

            val change = event.changes.firstOrNull() ?: continue
            val delta = change.scrollDelta.y

            if (delta == 0f || change.isConsumed) continue

            val current = camera()

            // Negative wheel delta zooms in.
            val factor = exp((-delta * 0.12f).toDouble()).toFloat()
            val newZoom = current.zoom * factor

            onCameraChanged(
                current.zoomAt(
                    screenPoint = change.position,
                    newZoom = newZoom,
                )
            )

            change.consume()
        }
    }
}