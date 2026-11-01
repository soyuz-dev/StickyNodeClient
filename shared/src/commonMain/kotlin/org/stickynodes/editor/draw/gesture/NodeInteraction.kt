
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isShiftPressed
import org.stickynodes.editor.draw.geometry.ResizeHandle
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText
import kotlin.uuid.Uuid

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
    onEditText: (EditorText) -> Unit,
) {


    var lastClickNodeId: Uuid? = null
    var lastClickPosition = Offset.Unspecified
    var lastClickUpTime = 0L

    fun resetClickTracking() {
        lastClickNodeId = null
        lastClickPosition = Offset.Unspecified
        lastClickUpTime = 0L
    }


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

            if (hitNode == null) {
                handleMarqueeSelection(
                    down = down,
                    nodes = nodes,
                    initialSelection = initialSelection,
                    screenToWorld = screenToWorld,
                    additive = shiftPressed,
                    onSelect = onSelect,
                    onSelectNodes = onSelectNodes,
                    onMarquee = onMarquee
                )

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

            if (dragStarted) {
                resetClickTracking()
            } else {
                val interval = down.uptimeMillis - lastClickUpTime

                val isDoubleClick =
                    hitNode is EditorText &&
                            lastClickNodeId == hitNode.id &&
                            lastClickPosition.isSpecified &&
                            interval >= viewConfiguration.doubleTapMinTimeMillis &&
                            interval <= viewConfiguration.doubleTapTimeoutMillis &&
                            (down.position - lastClickPosition).getDistance() <=
                            viewConfiguration.touchSlop

                if (isDoubleClick) {
                    onSelect(hitNode.id)
                    onEditText(hitNode)
                    resetClickTracking()
                } else {
                    if (wasSelected) {
                        onSelect(hitNode.id)
                    }

                    lastClickNodeId = hitNode.id
                    lastClickPosition = down.position

                    // handleNodeDrag has already awaited the end
                    // of this interaction.
                    lastClickUpTime =
                        currentEvent.changes.firstOrNull()?.uptimeMillis
                            ?: down.uptimeMillis
                }
            }

        }
    } finally {
        // Always clean up temporary interaction state,
        // including when the pointer-input coroutine is cancelled.
        onPreview(emptyList())
        onMarquee(null)
    }
}
