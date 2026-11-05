
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorShape

fun List<EditorShape>.constrainedOrientedResizeTransform(
    handle: ResizeHandle,
    worldDelta: Offset,
    proportional: Boolean = false,
    minimumSize: Float = 1f,
): OrientedGroupResizeTransform? {
    require(minimumSize.isFinite() && minimumSize > 0f) {
        "Minimum size must be finite and positive"
    }

    if (isEmpty()) return null

    val rotation = commonRotationOrNull() ?: return null

    val source = orientedGroupBoundsOrNull {
        (it as EditorShape).bounds
    } ?: return null

    if (source.width <= 0f || source.height <= 0f) {
        return null
    }

    check(all { it.width > 0f && it.height > 0f }) {
        "Shapes must have positive dimensions"
    }

    val minimumScaleX = maxOf {
        minimumSize / it.width
    }

    val minimumScaleY = maxOf {
        minimumSize / it.height
    }

    val initial = orientedGroupResizeTransform(
        source = source,
        rotation = rotation,
        handle = handle,
        worldDelta = worldDelta,
        proportional = proportional,
        minimumSize = minimumSize,
    )

    var scaleX = maxOf(initial.scaleX, minimumScaleX)
    var scaleY = maxOf(initial.scaleY, minimumScaleY)

    val cornerHandle =
        (handle.movesLeft || handle.movesRight) &&
                (handle.movesTop || handle.movesBottom)

    if (proportional && cornerHandle) {
        val scale = maxOf(scaleX, scaleY)
        scaleX = scale
        scaleY = scale
    }

    val width = source.width * scaleX
    val height = source.height * scaleY

    val target = Rect(
        left = if (handle.movesLeft) {
            source.right - width
        } else {
            source.left
        },
        top = if (handle.movesTop) {
            source.bottom - height
        } else {
            source.top
        },
        right = if (handle.movesRight) {
            source.left + width
        } else {
            source.right
        },
        bottom = if (handle.movesBottom) {
            source.top + height
        } else {
            source.bottom
        },
    )

    return OrientedGroupResizeTransform(
        source = source,
        target = target,
        rotation = rotation,
    )
}
