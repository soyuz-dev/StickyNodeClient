package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import kotlin.uuid.Uuid

data class EditorPath(
    override val id: Uuid = Uuid.random(),
    override val x: Float,
    override val y: Float,
    override val style: NodeStyle = NodeStyle(
        fill = FillStyle.None,
        stroke = StrokeStyle(
            color = 0xff0d0d0d.toInt(),
            width = 3f
        )
    ),
    val points: List<Offset>,
) : EditorNode
