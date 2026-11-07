
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

data class SnapGuideSpan(
    val start: Float,
    val end: Float,
)

data class SnapResult(
    val delta: Offset,
    val verticalGuide: Float? = null,
    val horizontalGuide: Float? = null,
    val verticalSpan: SnapGuideSpan? = null,
    val horizontalSpan: SnapGuideSpan? = null,
)

/**
 * Adjusts a proposed translation to align the moving
 * bounds with nearby target bounds.
 *
 * Coordinates and threshold are in world space.
 */
fun snapTranslation(
    movingBounds: Rect,
    targetBounds: List<Rect>,
    proposedDelta: Offset,
    threshold: Float,
): SnapResult {
    require(threshold.isFinite() && threshold >= 0f)
    require(proposedDelta.x.isFinite())
    require(proposedDelta.y.isFinite())

    val moved = movingBounds.translate(proposedDelta)

    val movingX = listOf(
        moved.left,
        moved.center.x,
        moved.right,
    )

    val movingY = listOf(
        moved.top,
        moved.center.y,
        moved.bottom,
    )

    var bestXDistance = threshold
    var bestYDistance = threshold

    var correctionX = 0f
    var correctionY = 0f

    var verticalGuide: Float? = null
    var horizontalGuide: Float? = null

    var verticalTarget: Rect? = null
    var horizontalTarget: Rect? = null

    for (target in targetBounds) {
        val targetX = listOf(
            target.left,
            target.center.x,
            target.right,
        )

        val targetY = listOf(
            target.top,
            target.center.y,
            target.bottom,
        )

        for (moving in movingX) {
            for (candidate in targetX) {
                val difference = candidate - moving
                val distance = abs(difference)

                if (
                    distance <= bestXDistance &&
                    (
                            verticalGuide == null ||
                                    distance < bestXDistance
                            )
                ) {
                    bestXDistance = distance
                    correctionX = difference
                    verticalGuide = candidate
                    verticalTarget = target
                }
            }
        }

        for (moving in movingY) {
            for (candidate in targetY) {
                val difference = candidate - moving
                val distance = abs(difference)

                if (
                    distance <= bestYDistance &&
                    (
                            horizontalGuide == null ||
                                    distance < bestYDistance
                            )
                ) {
                    bestYDistance = distance
                    correctionY = difference
                    horizontalGuide = candidate
                    horizontalTarget = target
                }
            }
        }
    }

    val snapped = moved.translate(
        Offset(correctionX, correctionY)
    )

    val verticalSpan = verticalTarget?.let { target ->
        SnapGuideSpan(
            start = minOf(snapped.top, target.top),
            end = maxOf(snapped.bottom, target.bottom),
        )
    }

    val horizontalSpan = horizontalTarget?.let { target ->
        SnapGuideSpan(
            start = minOf(snapped.left, target.left),
            end = maxOf(snapped.right, target.right),
        )
    }

    return SnapResult(
        delta = proposedDelta + Offset(
            correctionX,
            correctionY,
        ),
        verticalGuide = verticalGuide,
        horizontalGuide = horizontalGuide,
        verticalSpan = verticalSpan,
        horizontalSpan = horizontalSpan,
    )
}