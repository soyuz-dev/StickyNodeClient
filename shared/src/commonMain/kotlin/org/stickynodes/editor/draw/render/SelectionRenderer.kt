package org.stickynodes.editor.draw.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import org.stickynodes.editor.draw.geometry.bounds
import org.stickynodes.editor.draw.model.EditorNode

fun DrawScope.drawSelection(
    node: EditorNode,
    color: Color,
) {
    val outline = node.bounds.inflate(3f)

    drawRect(
        color = color,
        topLeft = outline.topLeft,
        size = outline.size,
        style = Stroke(width = 2f),
    )
}