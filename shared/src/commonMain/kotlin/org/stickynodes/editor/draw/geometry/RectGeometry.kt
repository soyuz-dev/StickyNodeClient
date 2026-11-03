package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect

fun Iterable<Rect>.enclosingBounds(): Rect? {
    val iterator = iterator()

    if (!iterator.hasNext()) return null

    var result = iterator.next()

    while (iterator.hasNext()) {
        val next = iterator.next()

        result = Rect(
            left = minOf(result.left, next.left),
            top = minOf(result.top, next.top),
            right = maxOf(result.right, next.right),
            bottom = maxOf(result.bottom, next.bottom),
        )
    }

    return result
}