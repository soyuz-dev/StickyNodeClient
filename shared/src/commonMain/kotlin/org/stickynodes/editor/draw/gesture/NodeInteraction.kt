
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
import org.stickynodes.editor.draw.geometry.SnapResult
import org.stickynodes.editor.draw.geometry.snapTranslation
import org.stickynodes.editor.draw.geometry.enclosingBounds
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
    groupRotationHandleAt: (Offset) -> Boolean = { false },
    groupResizeHandleAt: (Offset) -> ResizeHandle? = { null },
    onSnapChanged: (SnapResult?) -> Unit = {},
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

            // Group resize takes priority over rotation and node interactions.
            val groupResizeHandle = groupResizeHandleAt(down.position)

            if (groupResizeHandle != null) {
                val selection = selectedNodeIds()

                val selectedNodes = nodes().filter { node ->
                    node.id in selection
                }

                val shapes = selectedNodes.filterIsInstance<EditorShape>()

                if (
                    selectedNodes.isNotEmpty() &&
                    shapes.size == selectedNodes.size
                ) {
                    handleGroupResizeGesture(
                        down = down,
                        handle = groupResizeHandle,
                        shapes = shapes,
                        screenDeltaToWorld = screenDeltaToWorld,
                        onPreview = onPreview,
                        onCommit = onCommit,
                    )

                    resetClickTracking()
                    return@awaitEachGesture
                }
            }

            if (groupRotationHandleAt(down.position)) {
                val selection = selectedNodeIds()

                val selectedNodes = nodes().filter { node ->
                    node.id in selection
                }

                if (selectedNodes.isNotEmpty()) {
                    handleNodeRotationGesture(
                        down = down,
                        nodes = selectedNodes,
                        screenToWorld = screenToWorld,
                        selectionBounds = selectionBounds,
                        textBounds = { text ->
                            selectionBounds(text)
                        },
                        onPreview = onPreview,
                        onCommit = onCommit,
                    )

                    return@awaitEachGesture
                }
            }

            val rotationId = rotationHandleAt(down.position)

            if (rotationId != null) {
                val selection = selectedNodeIds()

                val selectedNodes = nodes().filter { node ->
                    node.id in selection
                }

                if (rotationId in selection) {
                    val shapes =
                        selectedNodes.filterIsInstance<EditorShape>()

                    when {
                        selectedNodes.size == 1 &&
                                selectedNodes.single() is EditorText -> {
                            handleNodeRotationGesture(
                                down = down,
                                nodes = selectedNodes,
                                screenToWorld = screenToWorld,
                                selectionBounds = selectionBounds,
                                textBounds = { text ->
                                    selectionBounds(text)
                                },
                                onPreview = onPreview,
                                onCommit = onCommit,
                            )

                            return@awaitEachGesture
                        }

                        shapes.isNotEmpty() &&
                                shapes.size == selectedNodes.size -> {
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

// Capture the geometry at the beginning of the drag,
// rather than recalculating it on every pointer event.
            val movingBounds = originalNodes
                .map(selectionBounds)
                .enclosingBounds()

            val targetBounds = nodes()
                .filterNot { node ->
                    node.id in startingSelection
                }
                .map(selectionBounds)

// Keep snapping sensitivity approximately constant
// regardless of the canvas zoom level.
            val snapThreshold = 8f / zoom()

            val dragStarted = handleNodeDrag(
                down = down,
                nodes = originalNodes,
                screenDeltaToWorld = screenDeltaToWorld,
                onPreview = onPreview,
                onCommit = onCommit,
                adjustDelta = { proposedDelta ->
                    if(
                        movingBounds == null ||
                        targetBounds.isEmpty()
                    ) SnapResult(delta = proposedDelta)
                    else {
                        snapTranslation(
                            movingBounds = movingBounds,
                            targetBounds = targetBounds,
                            proposedDelta = proposedDelta,
                            threshold = snapThreshold,
                        )
                    }
                },
                onSnapChanged = onSnapChanged
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
        onSnapChanged(null)
        onPreview(emptyList())
        onMarquee(null)
    }
}
