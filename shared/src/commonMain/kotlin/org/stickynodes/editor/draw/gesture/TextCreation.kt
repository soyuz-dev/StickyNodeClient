package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

suspend fun PointerInputScope.detectTextCreation(
    screenToWorld: (Offset) -> Offset,
    onCreate: (Offset) -> Unit
) {
    detectTapGestures { position ->
        onCreate(screenToWorld(position))
    }
}