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
import org.stickynodes.editor.draw.geometry.resizedRotatedBy
import org.stickynodes.editor.draw.geometry.resizedTogether
import org.stickynodes.editor.draw.geometry.rotationAngle
import org.stickynodes.editor.draw.geometry.snapRotation
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import kotlin.uuid.Uuid

private fun rectangleBetween(a: Offset, b: Offset) = Rect(
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


            val rotationId = rotationHandleAt(down.position)

            if (rotationId != null) {
                val originalShape = nodes()
                    .firstOrNull { it.id == rotationId } as? EditorShape

                // Only a single selected shape can be rotated for now.
                if (
                    originalShape != null &&
                    selectedNodeIds() == setOf(rotationId)
                ) {
                    val center = originalShape.bounds.center

                    // Keep an accumulated angle so dragging through 180 degrees
                    // doesn't jump back to the opposite direction.
                    var previousPointer = screenToWorld(down.position)
                    var accumulatedAngle = 0f

                    var preview: EditorShape? = null

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

                        val angle = originalShape.rotation + accumulatedAngle

                        preview = originalShape.withRotation(
                            if (snap) snapRotation(angle) else angle
                        )

                        onPreview(listOf(preview))
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
                            preview?.let {
                                onCommit(listOf(it))
                            }
                        }
                    }

                    onPreview(emptyList())
                    return@awaitEachGesture
                }
            }


            val resizeHandle = resizeHandleAt(down.position)

            if(resizeHandle != null) {
                val selection = selectedNodeIds()

                val originalShapes = nodes()
                    .filter { it.id in selection }
                    .filterIsInstance<EditorShape>()

                if(
                    originalShapes.isNotEmpty() &&
                    originalShapes.size == selection.size
                    ) {
                    var preview = listOf<EditorShape>()


                    fun updateResize(
                        position: Offset,
                        preserveAspectRatio: Boolean,
                    ) {
                        val delta = screenDeltaToWorld(
                            position - down.position
                        )

                        preview = if (originalShapes.size == 1) {
                            listOf(
                                originalShapes.single().resizedRotatedBy(
                                    handle = resizeHandle,
                                    delta = delta,
                                    proportional = preserveAspectRatio,
                                )
                            )
                        } else {
                            originalShapes.resizedTogether(
                                handle = resizeHandle,
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
                        updateResize(dragStart.position, currentEvent.keyboardModifiers.isShiftPressed)

                        val completed = drag(dragStart.id) { change ->
                            updateResize(change.position, currentEvent.keyboardModifiers.isShiftPressed)
                            change.consume()
                        }

                        if (completed) {
                            onCommit(preview)
                        }
                    }

                    onPreview(listOf())
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
                onToggleSelection(hitNode.id)
                waitForUpOrCancellation()
                return@awaitEachGesture
            }

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
        onMarquee(null)
    }
}