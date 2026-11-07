
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import org.stickynodes.editor.draw.geometry.SnapResult
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode

internal suspend fun AwaitPointerEventScope.handleNodeDrag(
    down: PointerInputChange,
    nodes: List<EditorNode>,
    screenDeltaToWorld: (Offset) -> Offset,
    onPreview: (List<EditorNode>) -> Unit,
    onCommit: (List<EditorNode>) -> Unit,
    adjustDelta: (Offset) -> SnapResult = {
        SnapResult(delta = it)
    },
    onSnapChanged: (SnapResult?) -> Unit = {},
): Boolean {
    if (nodes.isEmpty()) return false

    var preview = nodes

    fun updatePreview(position: Offset) {
        val proposedDelta = screenDeltaToWorld(
            position - down.position
        )

        val snap = adjustDelta(proposedDelta)

        preview = nodes.map { node ->
            node.translatedBy(snap.delta)
        }

        onPreview(preview)
        onSnapChanged(snap)
    }

    try {
        val dragStart = awaitTouchSlopOrCancellation(
            pointerId = down.id,
        ) { change, _ ->
            change.consume()
        }

        if (dragStart == null) {
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

        return true
    } finally {
        onSnapChanged(null)
        onPreview(emptyList())
    }
}
