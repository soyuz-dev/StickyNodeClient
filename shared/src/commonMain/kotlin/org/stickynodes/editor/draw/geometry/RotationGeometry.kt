
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.EditorShape
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

private const val fullTurn = (2.0 * PI).toFloat()
private const val snapStep = (PI / 12.0).toFloat()

fun Offset.rotatedAround(
    center: Offset,
    angle: Float,
): Offset {
    val dx = x - center.x
    val dy = y - center.y

    val cosA = cos(angle)
    val sinA = sin(angle)

    return Offset(
        x = center.x + dx * cosA - dy * sinA,
        y = center.y + dx * sinA + dy * cosA,
    )
}

fun rotationAngle(
    center: Offset,
    start: Offset,
    current: Offset,
): Float {
    val startAngle = atan2(
        start.y - center.y,
        start.x - center.x,
    )

    val currentAngle = atan2(
        current.y - center.y,
        current.x - center.x,
    )

    var delta = currentAngle - startAngle

    if (delta > PI) delta -= fullTurn
    if (delta < -PI) delta += fullTurn

    return delta
}

fun snapRotation(
    angle: Float,
    step: Float = snapStep,
): Float = round(angle / step) * step

fun EditorShape.rotatedBy(
    delta: Float,
    snap: Boolean = false,
): EditorShape {
    val angle = rotation + delta

    return withRotation(
        if (snap) snapRotation(angle) else angle
    )
}
