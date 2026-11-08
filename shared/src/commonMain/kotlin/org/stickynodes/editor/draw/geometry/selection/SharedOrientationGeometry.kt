package org.stickynodes.editor.draw.geometry.selection

import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val DEFAULT_ANGLE_TOLERANCE = 0.0001f

fun List<EditorNode>.commonRotationOrNull(
    tolerance: Float = DEFAULT_ANGLE_TOLERANCE
): Float? {
    require(tolerance.isFinite() && tolerance >= 0f) {
        "Tolerance must be finite and non-negative"
    }

    if (isEmpty()) return null

    fun EditorNode.rotationOrNull(): Float? = when (this) {
        is EditorShape -> rotation
        is EditorText -> rotation
        is EditorPath -> null
    }

    val reference = first().rotationOrNull() ?: return null

    check(reference.isFinite()) {
        "Node rotation must be finite"
    }

    for (node in drop(1)) {
        val rotation = node.rotationOrNull() ?: return null

        check(rotation.isFinite()) {
            "Node rotation must be finite"
        }

        val difference = rotation - reference

        val angularDistance = abs(
            atan2(
                sin(difference),
                cos(difference),
            )
        )

        if (angularDistance > tolerance) return null
    }

    return reference
}