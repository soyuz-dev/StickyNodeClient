package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

fun Rect.resizeHandlePositions(): Map<ResizeHandle, Offset> {
    val centreX = (left + right)/2f
    val centreY = (top + bottom)/2f

    return mapOf(
        ResizeHandle.N to Offset(centreX, top),
        ResizeHandle.NW to Offset(left, top),
        ResizeHandle.W to Offset(left, centreY),
        ResizeHandle.SW to Offset(left, bottom),
        ResizeHandle.S to Offset(centreX, bottom),
        ResizeHandle.SE to Offset(right, bottom),
        ResizeHandle.E to Offset(right, centreY),
        ResizeHandle.NE to Offset(right, top),
    )
}

fun hitTestResizeHandle(
    bounds: Rect,
    pointer: Offset,
    radius: Float = 10f
): ResizeHandle? {
    val radiusSquared = radius * radius

    return bounds.resizeHandlePositions()
        .entries
        .firstOrNull { (_, position) ->
            val delta = pointer - position
            delta.getDistanceSquared() <= radiusSquared
        }
        ?.key
}