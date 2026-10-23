package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.camera.EditorCamera

suspend fun PointerInputScope.detectCanvasPan(
    camera: () -> EditorCamera,
    onCameraChanged: (EditorCamera) -> Unit,
) {
    detectDragGestures(
        onDrag = { change, dragAmount ->
            val current = camera()

            onCameraChanged(
                current.copy(
                    position = current.position -
                            dragAmount / current.zoom,
                )
            )

            change.consume()
        },
    )
}