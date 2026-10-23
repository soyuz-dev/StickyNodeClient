package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.rectangleBetween
import org.stickynodes.editor.draw.model.EditorRectangle

suspend fun PointerInputScope.detectRectangleCreation(
    onPreview: (Rect?) -> Unit,
    onCommit: (EditorRectangle) -> Unit,
) {
    var start: Offset? = null
    var end: Offset? = null

    detectDragGestures(
        onDragStart = { offset ->
            start = offset
            end = offset
            onPreview(rectangleBetween(offset, offset))
        },
        onDrag = { change, dragAmount ->
            val origin = start
            val current = end

            if (origin != null && current != null) {
                val next = current + dragAmount
                end = next
                onPreview(rectangleBetween(origin, next))
            }

            change.consume()
        },
        onDragEnd = {
            val origin = start
            val finish = end

            if (origin != null && finish != null) {
                val rect = rectangleBetween(origin, finish)

                if (rect.width > 0f && rect.height > 0f) {
                    onCommit(
                        EditorRectangle(
                            x = rect.left,
                            y = rect.top,
                            width = rect.width,
                            height = rect.height,
                        )
                    )
                }
            }

            start = null
            end = null
            onPreview(null)
        },
        onDragCancel = {
            start = null
            end = null
            onPreview(null)
        },
    )
}