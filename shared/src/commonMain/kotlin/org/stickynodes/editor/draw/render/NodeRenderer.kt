package org.stickynodes.editor.draw.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorEllipse

fun DrawScope.drawNode(
    node: EditorNode,
    color: Color,
) {
    when (node) {
        is EditorRectangle -> drawRect(
            color = color,
            topLeft = Offset(node.x, node.y),
            size = Size(node.width, node.height),
        )
        is EditorEllipse -> drawOval(
            color = color,
            topLeft = Offset(node.x, node.y),
            size = Size(node.width, node.height),
        )
    }
}