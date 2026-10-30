
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isShiftPressed
import org.stickynodes.editor.draw.geometry.ResizeHandle
import org.stickynodes.editor.draw.geometry.bounds
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import kotlin.uuid.Uuid

private fun rectangleBetween(
    a: Offset,
    b: Offset,
): Rect = Rect(
    left = minOf(a.x, b.x),
    top = minOf(a.y, b.y),
    right = maxOf(a.x, b.x),
    bottom = maxOf(a.y, b.y),
)

private fun Rect.fullyContains(other: Rect): Boolean =
    other.left >= left &&
            other.top >= top &&
            other.right <= right &&
            other.bottom <= bottom

suspend fun PointerInputScope.detectNodeInteraction(
    nodes: () -> List<EditorNode>,
    selectedNodeIds: () -> Set<Uuid>,
    screenToWorld: (Offset) -> Offset,
    screenDeltaToWorld: (Offset) -> Offset,
    zoom: () -> Float,
    hitTest: (EditorNode, Offset, Float) -> Boolean,
    onSelect: (Uuid?) -> Unit,
    onSelectNodes: (Set<Uuid>) -> Unit,
    onToggleSelection: (Uuid) -> Unit,
    onPreview: (List<EditorNode>) -> Unit,
    onMarquee: (Rect?) -> Unit,
    onCommit: (List<EditorNode>) -> Unit,
    resizeHandleAt: (Offset) -> ResizeHandle?,
    rotationHandleAt: (Offset) -> Uuid?,
) {
    try {
        awaitEachGesture {
            val down = awaitFirstDown()

            // Rotation takes priority over other interactions.
            val rotationId = rotationHandleAt(down.position)

            if (rotationId != null) {
                val selection = selectedNodeIds()

                val shapes = nodes()
                    .filter { it.id in selection }
                    .filterIsInstance<EditorShape>()

                if (
                    rotationId in selection &&
                    shapes.isNotEmpty() &&
                    shapes.size == selection.size
                ) {
                    handleRotationGesture(
                        down = down,
                        shapes = shapes,
                        screenToWorld = screenToWorld,
                        onPreview = onPreview,
                        onCommit = onCommit,
                    )

                    return@awaitEachGesture
                }
            }

            // Resize handles take priority over node movement.
            val resizeHandle = resizeHandleAt(down.position)

            if (resizeHandle != null) {
                val selection = selectedNodeIds()

                val shapes = nodes()
                    .filter { it.id in selection }
                    .filterIsInstance<EditorShape>()

                if (
                    shapes.isNotEmpty() &&
                    shapes.size == selection.size
                ) {
                    handleResizeGesture(
                        down = down,
                        handle = resizeHandle,
                        shapes = shapes,
                        screenDeltaToWorld = screenDeltaToWorld,
                        onPreview = onPreview,
                        onCommit = onCommit,
                    )

                    return@awaitEachGesture
                }
            }

            // Regular selection, marquee and movement.
            val shiftPressed =
                currentEvent.keyboardModifiers.isShiftPressed

            val worldPoint = screenToWorld(down.position)

            // Reverse order ensures the topmost node is hit first.
            val hitNode = nodes()
                .asReversed()
                .firstOrNull { node ->
                    hitTest(node, worldPoint, zoom())
                }

            val initialSelection = selectedNodeIds()
            val wasSelected = hitNode?.id in initialSelection

            onPreview(emptyList())
            onMarquee(null)

            // Shift-click toggles a node's selection.
            if (shiftPressed && hitNode != null) {
                onToggleSelection(hitNode.id)
                waitForUpOrCancellation()
                return@awaitEachGesture
            }

            // Pressing empty canvas may start marquee selection.
            if (hitNode == null) {
                val dragStart = awaitTouchSlopOrCancellation(
                    pointerId = down.id,
                ) { change, _ ->
                    change.consume()
                }

                if (dragStart == null) {
                    if (!shiftPressed) {
                        onSelect(null)
                    }

                    return@awaitEachGesture
                }

                val initialNodes = nodes()

                fun updateMarquee(position: Offset) {
                    val marquee = rectangleBetween(
                        worldPoint,
                        screenToWorld(position),
                    )

                    onMarquee(marquee)

                    val containedIds = initialNodes
                        .asSequence()
                        .filter { marquee.fullyContains(it.bounds) }
                        .map { it.id }
                        .toSet()

                    onSelectNodes(
                        if (shiftPressed) {
                            initialSelection + containedIds
                        } else {
                            containedIds
                        }
                    )
                }

                updateMarquee(dragStart.position)

                val completed = drag(dragStart.id) { change ->
                    updateMarquee(change.position)
                    change.consume()
                }

                if (!completed) {
                    onSelectNodes(initialSelection)
                }

                onMarquee(null)
                return@awaitEachGesture
            }

            // Select the node before initiating movement.
            if (!wasSelected) {
                onSelect(hitNode.id)
            }

            val startingSelection = if (wasSelected) {
                initialSelection
            } else {
                setOf(hitNode.id)
            }

            val originalNodes = nodes().filter { node ->
                node.id in startingSelection
            }

            // Movement is handled by NodeDrag.kt.
            // Boolean indicates whether drag slop was crossed.
            val dragStarted = handleNodeDrag(
                down = down,
                nodes = originalNodes,
                screenDeltaToWorld = screenDeltaToWorld,
                onPreview = onPreview,
                onCommit = onCommit,
            )

            // Clicking an already-selected node without dragging
            // collapses a multi-selection to that node.
            if (!dragStarted && wasSelected) {
                onSelect(hitNode.id)
            }
        }
    } finally {
        // Always clean up temporary interaction state,
        // including when the pointer-input coroutine is cancelled.
        onPreview(emptyList())
        onMarquee(null)
    }
}
