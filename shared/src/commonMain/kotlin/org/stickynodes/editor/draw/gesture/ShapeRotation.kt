
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isShiftPressed
import org.stickynodes.editor.draw.geometry.combinedVisualBounds
import org.stickynodes.editor.draw.geometry.rotatedTogether
import org.stickynodes.editor.draw.geometry.rotationAngle
import org.stickynodes.editor.draw.geometry.snapRotation
import org.stickynodes.editor.draw.model.EditorShape

internal suspend fun AwaitPointerEventScope.handleRotationGesture(
    down: PointerInputChange,
    shapes: List<EditorShape>,
    screenToWorld: (Offset) -> Offset,
    onPreview: (List<EditorShape>) -> Unit,
    onCommit: (List<EditorShape>) -> Unit,
) {
    if (shapes.isEmpty()) return

    val singleShape = shapes.singleOrNull()

    val center = singleShape?.bounds?.center
        ?: shapes.combinedVisualBounds()?.center
        ?: return

    var previousPointer = screenToWorld(down.position)
    var accumulatedAngle = 0f
    var preview = shapes

    fun updateRotation(
        position: Offset,
        snap: Boolean,
    ) {
        val currentPointer = screenToWorld(position)

        val delta = rotationAngle(
            center = center,
            start = previousPointer,
            current = currentPointer,
        )

        accumulatedAngle += delta
        previousPointer = currentPointer

        preview = if (singleShape != null) {
            val angle = singleShape.rotation + accumulatedAngle

            listOf(
                singleShape.withRotation(
                    if (snap) snapRotation(angle) else angle
                )
            )
        } else {
            val angle = if (snap) {
                snapRotation(accumulatedAngle)
            } else {
                accumulatedAngle
            }

            shapes.rotatedTogether(
                angle = angle,
                center = center,
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
        updateRotation(
            position = dragStart.position,
            snap = currentEvent.keyboardModifiers.isShiftPressed,
        )

        val completed = drag(dragStart.id) { change ->
            updateRotation(
                position = change.position,
                snap = currentEvent.keyboardModifiers.isShiftPressed,
            )
            change.consume()
        }

        if (completed) {
            onCommit(preview)
        }
    }

    onPreview(emptyList())
}
