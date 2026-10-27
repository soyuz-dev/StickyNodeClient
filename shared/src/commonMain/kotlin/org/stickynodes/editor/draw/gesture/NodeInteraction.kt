package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isShiftPressed
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectNodeInteraction(
    nodes: () -> List<EditorNode>,
    selectedNodeIds: () -> Set<Uuid>,
    screenToWorld: (Offset) -> Offset,
    screenDeltaToWorld: (Offset) -> Offset,
    zoom: () -> Float,
    hitTest: (EditorNode, Offset, Float) -> Boolean,
    onSelect: (Uuid?) -> Unit,
    onToggleSelection: (Uuid) -> Unit,
    onPreview: (List<EditorNode>) -> Unit,
    onCommit: (List<EditorNode>) -> Unit,
) {
    try {
        awaitEachGesture {
            val down = awaitFirstDown()
            val shiftPressed = currentEvent.keyboardModifiers.isShiftPressed

            val worldPoint = screenToWorld(down.position)

            val hitNode = nodes()
                .asReversed()
                .firstOrNull { node ->
                    hitTest(node, worldPoint, zoom())
                }

            val wasSelected = hitNode?.id in selectedNodeIds()

            onPreview(emptyList())

            if (shiftPressed) {
                if (hitNode != null) {
                    onToggleSelection(hitNode.id)
                }

                waitForUpOrCancellation()
                return@awaitEachGesture
            }

            if (hitNode == null) {
                onSelect(null)
                waitForUpOrCancellation()
                return@awaitEachGesture
            }

            if (!wasSelected) {
                onSelect(hitNode.id)
            }

            val startingSelection = if (wasSelected) {
                selectedNodeIds()
            } else {
                setOf(hitNode.id)
            }

            val originalNodes = nodes().filter { node ->
                node.id in startingSelection
            }

            val dragStart = awaitTouchSlopOrCancellation(
                pointerId = down.id,
            ) { change, _ ->
                change.consume()
            }

            if (dragStart == null) {
                if (wasSelected) {
                    onSelect(hitNode.id)
                }
                return@awaitEachGesture
            }

            var preview = emptyList<EditorNode>()

            fun updatePreview(position: Offset) {
                val delta = screenDeltaToWorld(
                    position - down.position
                )

                preview = originalNodes.map { node ->
                    node.translatedBy(delta)
                }

                onPreview(preview)
            }

            updatePreview(dragStart.position)

            val completed = drag(dragStart.id) { change ->
                updatePreview(change.position)
                change.consume()
            }

            if (completed) {
                onCommit(preview)
            }

            onPreview(emptyList())
        }
    } finally {
        onPreview(emptyList())
    }
}