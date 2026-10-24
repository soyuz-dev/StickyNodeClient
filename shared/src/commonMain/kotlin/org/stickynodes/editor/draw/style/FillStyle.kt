package org.stickynodes.editor.draw.style

sealed interface FillStyle {
    data object None : FillStyle

    data class Solid(
        val color: Int
    ) : FillStyle
}