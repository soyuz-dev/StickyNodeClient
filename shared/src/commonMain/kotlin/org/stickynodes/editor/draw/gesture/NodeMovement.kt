package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.contains
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode

suspend fun PointerInputScope.detectNodeMovement(
    nodes: () -> List<EditorNode>,
    zoom: () -> Float,
    screenToWorld: (Offset) -> Offset,
    screenDeltaToWorld: (Offset) -> Offset,
    onSelect: (EditorNode) -> Unit,
    onPreview: (EditorNode?) -> Unit,
    onCommit: (EditorNode) -> Unit,
) {
    var original: EditorNode? = null
    var displacement = Offset.Zero

    try {
        detectDragGestures(
            onDragStart = { position ->
                val worldPosition = screenToWorld(position)

                original = nodes()
                    .asReversed()
                    .firstOrNull { it.contains(worldPosition, zoom()) }

                displacement = Offset.Zero
                original?.let(onSelect)
            },
            onDrag = { change, dragAmount ->
                original?.let { node ->
                    displacement += screenDeltaToWorld(dragAmount)

                    onPreview(
                        node.translatedBy(displacement)
                    )

                    change.consume()
                }
            },
            onDragEnd = {
                original?.let { node ->
                    if (displacement != Offset.Zero) {
                        onCommit(node.translatedBy(displacement))
                    }
                }

                original = null
                displacement = Offset.Zero
                onPreview(null)
            },
            onDragCancel = {
                original = null
                displacement = Offset.Zero
                onPreview(null)
            },
        )
    } finally {
        onPreview(null)
    }
}