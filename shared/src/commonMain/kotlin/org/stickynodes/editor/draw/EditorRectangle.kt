package org.stickynodes.editor.draw

import kotlin.uuid.Uuid

data class EditorRectangle(
    val id: Uuid = Uuid.random(),
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    init {
        require(width.isFinite() && width >= 0f)
        require(height.isFinite() && height >= 0f)
    }
}