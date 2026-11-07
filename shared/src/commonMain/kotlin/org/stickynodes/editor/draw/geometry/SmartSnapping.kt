
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

data class SnapResult(
    val delta: Offset,
    val verticalGuide: Float? = null,
    val horizontalGuide: Float? = null,
)

/**
 * Adjusts a proposed drag displacement so the moving bounds
 * align with nearby target bounds.
 *
 * All coordinates and thresholds are in world space.
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

                if (distance <= bestXDistance) {
                    if (
                        verticalGuide == null ||
                        distance < bestXDistance
                    ) {
                        bestXDistance = distance
                        correctionX = difference
                        verticalGuide = candidate
                    }
                }
            }
        }

        for (moving in movingY) {
            for (candidate in targetY) {
                val difference = candidate - moving
                val distance = abs(difference)

                if (distance <= bestYDistance) {
                    if (
                        horizontalGuide == null ||
                        distance < bestYDistance
                    ) {
                        bestYDistance = distance
                        correctionY = difference
                        horizontalGuide = candidate
                    }
                }
            }
        }
    }

    return SnapResult(
        delta = proposedDelta + Offset(
            correctionX,
            correctionY,
        ),
        verticalGuide = verticalGuide,
        horizontalGuide = horizontalGuide,
    )
}
