
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isFinite
import kotlin.math.abs

enum class SpacingAxis {
    HORIZONTAL,
    VERTICAL,
}

enum class SpacingPlacement {
    BEFORE,
    BETWEEN,
    AFTER,
}

data class EqualSpacingSnap(
    val correction: Float,
    val gap: Float,
    val firstTarget: Rect,
    val secondTarget: Rect,
    val placement: SpacingPlacement = SpacingPlacement.BETWEEN,
)

fun findEqualSpacingSnap(
    movingBounds: Rect,
    targetBounds: List<Rect>,
    proposedDelta: Offset,
    threshold: Float,
    axis: SpacingAxis,
): EqualSpacingSnap? {
    require(threshold.isFinite() && threshold >= 0f)
    require(proposedDelta.isFinite)

    if (targetBounds.size < 2) return null

    val moved = movingBounds.translate(proposedDelta)

    fun Rect.start(): Float = when (axis) {
        SpacingAxis.HORIZONTAL -> left
        SpacingAxis.VERTICAL -> top
    }

    fun Rect.end(): Float = when (axis) {
        SpacingAxis.HORIZONTAL -> right
        SpacingAxis.VERTICAL -> bottom
    }

    val movingStart = moved.start()
    val movingSize = moved.end() - movingStart

    val targets = targetBounds.sortedBy { it.start() }

    var best: EqualSpacingSnap? = null
    var bestDistance = threshold

    for (i in 0 until targets.lastIndex) {
        val first = targets[i]

        if (!moved.overlapsOnPerpendicularAxis(first, axis)) {
            continue
        }

        for (j in i + 1 until targets.size) {
            val second = targets[j]

            if (!moved.overlapsOnPerpendicularAxis(second, axis)) {
                continue
            }

            val existingGap = second.start() - first.end()

            // The stationary targets must not overlap
            // along the spacing axis.
            if (existingGap < 0f) continue

            fun consider(
                placement: SpacingPlacement,
                desiredStart: Float,
                gap: Float,
            ) {
                val correction = desiredStart - movingStart
                val distance = abs(correction)

                if (
                    distance <= bestDistance &&
                    (best == null || distance < bestDistance)
                ) {
                    bestDistance = distance

                    best = EqualSpacingSnap(
                        correction = correction,
                        gap = gap,
                        firstTarget = first,
                        secondTarget = second,
                        placement = placement,
                    )
                }
            }

            // Moving | First | Second
            consider(
                placement = SpacingPlacement.BEFORE,
                desiredStart =
                    first.start() - existingGap - movingSize,
                gap = existingGap,
            )

            // First | Moving | Second
            val remainingSpace = existingGap - movingSize

            if (remainingSpace >= 0f) {
                val gap = remainingSpace / 2f

                consider(
                    placement = SpacingPlacement.BETWEEN,
                    desiredStart = first.end() + gap,
                    gap = gap,
                )
            }

            // First | Second | Moving
            consider(
                placement = SpacingPlacement.AFTER,
                desiredStart = second.end() + existingGap,
                gap = existingGap,
            )
        }
    }

    return best
}

private fun Rect.overlapsOnPerpendicularAxis(
    other: Rect,
    axis: SpacingAxis,
): Boolean = when (axis) {
    SpacingAxis.HORIZONTAL ->
        top < other.bottom && bottom > other.top

    SpacingAxis.VERTICAL ->
        left < other.right && right > other.left
}
