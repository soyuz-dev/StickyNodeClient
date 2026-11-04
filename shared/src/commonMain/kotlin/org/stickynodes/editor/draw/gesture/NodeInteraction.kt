
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
    selectionBounds: (EditorNode) -> Rect,
    onHitSelection: ((Uuid, Boolean) -> Set<Uuid>)? = null,
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

            val shiftPressed =
                currentEvent.keyboardModifiers.isShiftPressed

            val worldPoint = screenToWorld(down.position)

            val hitNode = nodes()
                .asReversed()
                .firstOrNull { node ->
                    hitTest(node, worldPoint, zoom())
                }

            val initialSelection = selectedNodeIds()
            val wasSelected = hitNode?.id in initialSelection

            onPreview(emptyList())
            onMarquee(null)

            if (shiftPressed && hitNode != null) {
                if (onHitSelection != null) {
                    onHitSelection(hitNode.id, true)
                } else {
                    onToggleSelection(hitNode.id)
                }

                waitForUpOrCancellation()
                resetClickTracking()
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
                    selectionBounds = selectionBounds,
                    onMarquee = onMarquee,
                )

                resetClickTracking()
                return@awaitEachGesture
            }

            val startingSelection = if (wasSelected) {
                initialSelection
            } else if (onHitSelection != null) {
                onHitSelection(hitNode.id, false)
            } else {
                onSelect(hitNode.id)
                setOf(hitNode.id)
            }

            val originalNodes = nodes().filter { node ->
                node.id in startingSelection
            }

            val dragStarted = handleNodeDrag(
                down = down,
                nodes = originalNodes,
                screenDeltaToWorld = screenDeltaToWorld,
                onPreview = onPreview,
                onCommit = onCommit,
            )

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

                val finalSelection = if (wasSelected) {
                    if (onHitSelection != null) {
                        onHitSelection(hitNode.id, false)
                    } else {
                        onSelect(hitNode.id)
                        setOf(hitNode.id)
                    }
                } else {
                    startingSelection
                }

                if (
                    isDoubleClick &&
                    hitNode.id in finalSelection &&
                    finalSelection.size == 1
                ) {
                    onEditText(hitNode)
                    resetClickTracking()
                } else {
                    lastClickNodeId = hitNode.id
                    lastClickPosition = down.position
                    lastClickUpTime =
                        currentEvent.changes.firstOrNull()?.uptimeMillis
                            ?: down.uptimeMillis
                }
            }
        }
    } finally {
        onPreview(emptyList())
        onMarquee(null)
    }
}
