package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.rectangleBetween
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectShapeCreation(
    screenToWorld: (Offset) -> Offset,
    createNode: (Uuid, Rect) -> EditorNode,
    onPreview: (EditorNode?) -> Unit,
    onCommit: (EditorNode) -> Unit,
) {
    var start: Offset? = null
    var end: Offset? = null
    var id: Uuid? = null

    try {
        detectDragGestures(
            onDragStart = { position ->
                val worldPosition = screenToWorld(position)
                val newId = Uuid.random()

                start = worldPosition
                end = worldPosition
                id = newId

                onPreview(
                    createNode(
                        newId,
                        rectangleBetween(worldPosition, worldPosition),
                    )
                )
            },
            onDrag = { change, _ ->
                val origin = start
                val nodeId = id

                if (origin != null && nodeId != null) {
                    val next = screenToWorld(change.position)
                    end = next

                    onPreview(
                        createNode(
                            nodeId,
                            rectangleBetween(origin, next),
                        )
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