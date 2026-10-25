package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectNodeInteraction(
    nodes: () -> List<EditorNode>,
    screenToWorld: (Offset) -> Offset,
    screenDeltaToWorld: (Offset) -> Offset,
    zoom: () -> Float,
    hitTest: (EditorNode, Offset, Float) -> Boolean,
    onSelect: (Uuid?) -> Unit,
    onPreview: (EditorNode?) -> Unit,
    onCommit: (EditorNode) -> Unit,
) {
    try {
        awaitEachGesture {
            val down = awaitFirstDown()
            val worldPoint = screenToWorld(down.position)

            val selectedNode = nodes()
                .asReversed()
                .firstOrNull { node ->
                    hitTest(node, worldPoint, zoom())
                }

            onPreview(null)
            onSelect(selectedNode?.id)

            val dragStart = awaitTouchSlopOrCancellation(
                pointerId = down.id,
            ) { change, _ ->
                if (selectedNode != null) {
                    change.consume()
                }
            }

            if (selectedNode != null && dragStart != null) {
                var preview: EditorNode? = null

                fun updatePreview(position: Offset) {
                    val delta = screenDeltaToWorld(
                        position - down.position
                    )

                    preview = selectedNode.translatedBy(delta)
                    onPreview(preview)
                }

                updatePreview(dragStart.position)

                val completed = drag(dragStart.id) { change ->
                    updatePreview(change.position)
                    change.consume()
                }

                if (completed) {
                    preview?.let(onCommit)
                }

                onPreview(null)
            }
        }
    } finally {
        onPreview(null)
    }
}
