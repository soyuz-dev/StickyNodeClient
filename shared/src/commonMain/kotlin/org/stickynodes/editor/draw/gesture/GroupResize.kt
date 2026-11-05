
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isShiftPressed
import org.stickynodes.editor.draw.geometry.ResizeHandle
import org.stickynodes.editor.draw.geometry.constrainedOrientedResizeTransform
import org.stickynodes.editor.draw.geometry.resizedWith
import org.stickynodes.editor.draw.model.EditorShape

internal suspend fun AwaitPointerEventScope.handleGroupResizeGesture(
    down: PointerInputChange,
    handle: ResizeHandle,
    shapes: List<EditorShape>,
    screenDeltaToWorld: (Offset) -> Offset,
    onPreview: (List<EditorShape>) -> Unit,
    onCommit: (List<EditorShape>) -> Unit,
) {
    if (shapes.isEmpty()) return

    var preview = shapes

    fun updateResize(
        position: Offset,
        proportional: Boolean,
    ) {
        val worldDelta = screenDeltaToWorld(
            position - down.position
        )

        val transform = shapes.constrainedOrientedResizeTransform(
            handle = handle,
            worldDelta = worldDelta,
            proportional = proportional,
        ) ?: return

        preview = shapes.resizedWith(transform)
        onPreview(preview)
    }

    try {
        val dragStart = awaitTouchSlopOrCancellation(
            pointerId = down.id,
        ) { change, _ ->
            change.consume()
        }

        if (dragStart != null) {
            updateResize(
                position = dragStart.position,
                proportional =
                    currentEvent.keyboardModifiers.isShiftPressed,
            )

            val completed = drag(dragStart.id) { change ->
                updateResize(
                    position = change.position,
                    proportional =
                        currentEvent.keyboardModifiers.isShiftPressed,
                )
                change.consume()
            }

            if (completed && preview != shapes) {
                onCommit(preview)
            }
        }
    } finally {
        onPreview(emptyList())
    }
}