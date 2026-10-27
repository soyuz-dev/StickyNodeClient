package org.stickynodes.editor.draw.gesture

import androidx.collection.longIntMapOf
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
import org.stickynodes.editor.draw.geometry.resizedBy
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode
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
) {
    try {
        awaitEachGesture {
            val down = awaitFirstDown()

            val resizeHandle = resizeHandleAt(down.position)

            if(resizeHandle != null) {
                val selectedId = selectedNodeIds().singleOrNull()

                val originalNode = nodes().firstOrNull { node ->
                    node.id == selectedId
                }

                if(originalNode != null) {
                    var preview: EditorNode? = null

                    fun updateResize(position: Offset) {
                        val delta = screenDeltaToWorld(position - down.position)

                        preview = originalNode.resizedBy(
                            handle = resizeHandle,
                            delta = delta,
                            minimumSize = 1f,
                        )

                        onPreview(listOfNotNull(preview))
                    }

                    val dragStart = awaitTouchSlopOrCancellation(
                        pointerId = down.id,
                    ) { change, _ ->
                        change.consume()
                    }

                    if (dragStart != null) {
                        updateResize(dragStart.position)

                        val completed = drag(dragStart.id) { change ->
                            updateResize(change.position)
                            change.consume()
                        }

                        if (completed) {
                            preview?.let { onCommit(listOf(it)) }
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