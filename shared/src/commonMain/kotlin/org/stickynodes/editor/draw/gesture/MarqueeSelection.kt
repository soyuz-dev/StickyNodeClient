
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import org.stickynodes.editor.draw.geometry.bounds
import org.stickynodes.editor.draw.geometry.selectionBounds
import org.stickynodes.editor.draw.model.EditorNode
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

internal suspend fun AwaitPointerEventScope.handleMarqueeSelection(
    down: PointerInputChange,
    nodes: () -> List<EditorNode>,
    initialSelection: Set<Uuid>,
    screenToWorld: (Offset) -> Offset,
    additive: Boolean,
    onSelect: (Uuid?) -> Unit,
    onSelectNodes: (Set<Uuid>) -> Unit,
    onMarquee: (Rect?) -> Unit,
) {
    val startPoint = screenToWorld(down.position)

    val dragStart = awaitTouchSlopOrCancellation(
        pointerId = down.id,
    ) { change, _ ->
        change.consume()
    }

    if (dragStart == null) {
        if (!additive) {
            onSelect(null)
        }
        return
    }

    // Snapshot the document at the start of the marquee drag.
    val initialNodes = nodes()

    fun updateMarquee(position: Offset) {
        val marquee = rectangleBetween(
            startPoint,
            screenToWorld(position),
        )

        onMarquee(marquee)

        val containedIds = initialNodes
            .asSequence()
            .filter { marquee.fullyContains(it.selectionBounds) }
            .map { it.id }
            .toSet()

        onSelectNodes(
            if (additive) {
                initialSelection + containedIds
            } else {
                containedIds
            }
        )
    }

    try {
        updateMarquee(dragStart.position)

        val completed = drag(dragStart.id) { change ->
            updateMarquee(change.position)
            change.consume()
        }

        if (!completed) {
            onSelectNodes(initialSelection)
        }
    } finally {
        onMarquee(null)
    }
}
