
package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.PathSampler
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectPathCreation(
    screenToWorld: (Offset) -> Offset,
    style: () -> NodeStyle,
    onPreview: (EditorPath?) -> Unit,
    onCommit: (EditorPath) -> Unit,
) {
    val sampler = PathSampler(spacing = 8f)

    var pathId: Uuid? = null
    var origin = Offset.Zero
    var pathStyle: NodeStyle? = null

    fun buildPath(screenPoints: List<Offset>): EditorPath? {
        val id = pathId ?: return null
        val currentStyle = pathStyle ?: return null

        val localPoints = screenPoints.map { point ->
            screenToWorld(point) - origin
        }

        return EditorPath(
            id = id,
            x = origin.x,
            y = origin.y,
            points = localPoints,
            style = currentStyle,
        )
    }

    fun reset() {
        pathId = null
        pathStyle = null
        onPreview(null)
    }

    try {
        detectDragGestures(
            orientationLock = null,
            shouldAwaitTouchSlop = { false },

            onDragStart = { down, _, _ ->
                val position = down.position

                sampler.start(position)
                origin = screenToWorld(position)

                pathId = Uuid.random()
                pathStyle = style()

                onPreview(buildPath(sampler.points))
            },

            onDrag = { change, _ ->
                val position = change.position

                sampler.moveTo(position)

                // Show the latest pointer position even if it
                // hasn't reached the next sampling threshold.
                val points = sampler.points

                val previewPoints = if (points.lastOrNull() == position) {
                    points
                } else {
                    points + position
                }

                onPreview(buildPath(previewPoints))
                change.consume()
            },

            onDragEnd = { change ->
                val points = sampler.finish(change.position)

                buildPath(points)?.let(onCommit)
                reset()
            },

            onDragCancel = {
                reset()
            },
        )
    } finally {
        reset()
    }
}
