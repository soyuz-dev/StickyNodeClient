
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode

internal suspend fun AwaitPointerEventScope.handleNodeDrag(
    down: PointerInputChange,
    nodes: List<EditorNode>,
    screenDeltaToWorld: (Offset) -> Offset,
    onPreview: (List<EditorNode>) -> Unit,
    onCommit: (List<EditorNode>) -> Unit,
): Boolean {
    if (nodes.isEmpty()) return false

    var preview = nodes

    fun updatePreview(position: Offset) {
        val delta = screenDeltaToWorld(
            position - down.position
        )

        preview = nodes.map { node ->
            node.translatedBy(delta)
        }

        onPreview(preview)
    }

    val dragStart = awaitTouchSlopOrCancellation(
        pointerId = down.id,
    ) { change, _ ->
        change.consume()
    }

    if (dragStart == null) {
        onPreview(emptyList())
        return false
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
    return true
}
