package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.geometry.selection.commonRotationOrNull
import org.stickynodes.editor.draw.model.EditorShape
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun List<EditorShape>.resizedWith(
    transform: OrientedGroupResizeTransform,
    minimumSize: Float = 1f,
): List<EditorShape> {
    require(minimumSize.isFinite() && minimumSize > 0f) {
        "Minimum size must be finite and positive"
    }

    if (isEmpty()) return this

    val commonRotation = commonRotationOrNull()
        ?: error("Shapes must share an orientation")

    val difference = commonRotation - transform.rotation
    val angularDistance = abs(
        atan2(sin(difference), cos(difference))
    )

    require(angularDistance <= 0.0001f) {
        "Transform rotation must match the shapes' orientation"
    }

    val scaleX = transform.scaleX
    val scaleY = transform.scaleY

    require(scaleX.isFinite() && scaleY.isFinite()) {
        "Scale factors must be finite"
    }

    check(all { shape ->
        shape.width * scaleX >= minimumSize &&
                shape.height * scaleY >= minimumSize
    }) {
        "Resizing would violate a shape's minimum dimensions"
    }

    return map { shape ->
        val originalCenter = shape.bounds.center
        val newCenter = transform.transform(originalCenter)

        val width = shape.width * scaleX
        val height = shape.height * scaleY

        shape.withBounds(
            Rect(
                left = newCenter.x - width / 2f,
                top = newCenter.y - height / 2f,
                right = newCenter.x + width / 2f,
                bottom = newCenter.y + height / 2f,
            )
        )
    }
}
