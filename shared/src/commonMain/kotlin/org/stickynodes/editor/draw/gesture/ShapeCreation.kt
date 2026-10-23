package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.rectangleBetween
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectShapeCreation(
    createNode: (Uuid, Rect) -> EditorNode,
    onPreview: (EditorNode?) -> Unit,
    onCommit: (EditorNode) -> Unit,
) {
    var start: Offset? = null
    var end: Offset? = null
    var id: Uuid? = null

    try {
        detectDragGestures(
            onDragStart = { offset ->
                start = offset
                end = offset
                val newId = Uuid.random()
                id = newId
                onPreview(createNode(newId, rectangleBetween(offset, offset)))
            },
            onDrag = { change, dragAmount ->
                val origin = start
                val current = end
                val nodeId = id

                if (origin != null && current != null && nodeId != null) {
                    val next = current + dragAmount
                    end = next

                    onPreview(
                        createNode(nodeId, rectangleBetween(origin, next))
                    )
                }

                change.consume()
            },
            onDragEnd = {
                val origin = start
                val finish = end
                val nodeId = id

                if (origin != null && finish != null && nodeId != null) {
                    val rect = rectangleBetween(origin, finish)

                    if (rect.width > 0f && rect.height > 0f) {
                        onCommit(createNode(nodeId, rect))
                    }
                }

                start = null
                end = null
                id = null
                onPreview(null)
            },
            onDragCancel = {
                start = null
                end = null
                id = null
                onPreview(null)
            },
        )
    } finally {
        onPreview(null)
    }
}