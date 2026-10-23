package org.stickynodes.editor.draw.model

import kotlin.uuid.Uuid

data class EditorEllipse(
    override val id: Uuid = Uuid.random(),
    override val x: Float,
    override val y: Float,
    val width: Float,
    val height: Float,
) : EditorNode