package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isShiftPressed
import org.stickynodes.editor.draw.geometry.ResizeHandle
import org.stickynodes.editor.draw.geometry.resizedRotatedBy
import org.stickynodes.editor.draw.geometry.resizedTogether
import org.stickynodes.editor.draw.model.EditorShape

internal suspend fun AwaitPointerEventScope.handleResizeGesture(
    down: PointerInputChange,
    handle: ResizeHandle,
    shapes: List<EditorShape>,
    screenDeltaToWorld: (Offset) -> Offset,
    onPreview: (List<EditorShape>) -> Unit,
    onCommit: (List<EditorShape>) -> Unit,
) {
    if (shapes.isEmpty()) return

    var preview: List<EditorShape> = shapes

    fun updateResize(
        position: Offset,
        preserveAspectRatio: Boolean,
    ) {
        val delta = screenDeltaToWorld(
            position - down.position
        )

        preview = if (shapes.size == 1) {
            listOf(
                shapes.single().resizedRotatedBy(
                    handle = handle,
                    delta = delta,
                    proportional = preserveAspectRatio,
                )
            )
        } else {
            shapes.resizedTogether(
                handle = handle,
                delta = delta,
                proportional = preserveAspectRatio,
            )
        }

        onPreview(preview)
    }

    val dragStart = awaitTouchSlopOrCancellation(
        pointerId = down.id,
    ) { change, _ ->
        change.consume()
    }

    if (dragStart != null) {
        updateResize(
            position = dragStart.position,
            preserveAspectRatio = currentEvent.keyboardModifiers.isShiftPressed,
        )

        val completed = drag(dragStart.id) { change ->
            updateResize(
                position = change.position,
                preserveAspectRatio = currentEvent.keyboardModifiers.isShiftPressed,
            )
            change.consume()
        }

        if (completed) {
            onCommit(preview)
        }
    }

    onPreview(emptyList())
}