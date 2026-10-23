package org.stickynodes.editor.draw

import kotlin.uuid.Uuid

data class EditorRectangle(
    override val id: Uuid = Uuid.random(),
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) : EditorNode