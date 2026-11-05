package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

data class OrientedGroupResizeTransform(
    val source: Rect,
    val target: Rect,
    val rotation: Float,
) {
    init {
        require(source.width > 0f && source.height > 0f) {
            "Source bounds must have positive dimensions"
        }
        require(target.width > 0f && target.height > 0f) {
            "Target bounds must have positive dimensions"
        }
        require(rotation.isFinite()) {
            "Rotation must be finite"
        }
    }

    val scaleX: Float
        get() = target.width / source.width

    val scaleY: Float
        get() = target.height / source.height

    fun transform(point: Offset): Offset {
        val local = point.rotatedAround(
            center = Offset.Zero,
            angle = -rotation,
        )

        val resized = Offset(
            x = target.left +
                    (local.x - source.left) * scaleX,
            y = target.top +
                    (local.y - source.top) * scaleY,
        )

        return resized.rotatedAround(
            center = Offset.Zero,
            angle = rotation,
        )
    }
}

fun orientedGroupResizeTransform(
    source: Rect,
    rotation: Float,
    handle: ResizeHandle,
    worldDelta: Offset,
    proportional: Boolean = false,
    minimumSize: Float = 1f,
): OrientedGroupResizeTransform {
    require(rotation.isFinite()) {
        "Rotation must be finite"
    }
    require(worldDelta.x.isFinite() && worldDelta.y.isFinite()) {
        "Drag delta must be finite"
    }
    require(minimumSize.isFinite() && minimumSize > 0f) {
        "Minimum size must be finite and positive"
    }
    require(source.width > 0f && source.height > 0f) {
        "Source bounds must have positive dimensions"
    }

    val localDelta = worldDelta.rotatedAround(
        center = Offset.Zero,
        angle = -rotation,
    )

    val target = if (proportional) {
        resizeBoundsProportionally(
            original = source,
            handle = handle,
            delta = localDelta,
            minimumSize = minimumSize,
        )
    } else {
        resizeBounds(
            original = source,
            handle = handle,
            delta = localDelta,
            minimumSize = minimumSize,
        )
    }

    return OrientedGroupResizeTransform(
        source = source,
        target = target,
        rotation = rotation,
    )
}
