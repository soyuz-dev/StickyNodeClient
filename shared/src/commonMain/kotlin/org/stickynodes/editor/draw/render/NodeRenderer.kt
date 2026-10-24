package org.stickynodes.editor.draw.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.style.FillStyle

fun DrawScope.drawNode(
    node: EditorNode,
    alpha: Float = 1f
) {
    val topLeft = Offset(node.x, node.y)

    val size = when(node) {
        is EditorRectangle -> Size(node.width, node.height)
        is EditorEllipse -> Size(node.width, node.height)
    }

    fun drawShape(
        color: Color,
        style: DrawStyle
    ) {
        when(node) {
            is EditorRectangle -> drawRect(
                color = color,
                topLeft = topLeft,
                size = size,
                style = style,
                alpha = alpha
            )
            is EditorEllipse -> drawOval(
                color = color,
                topLeft = topLeft,
                size = size,
                style = style,
                alpha = alpha
            )
        }
    }

    when (val fill = node.style.fill) {
        FillStyle.None -> Unit

        is FillStyle.Solid -> drawShape(
            color = fill.color.toComposeColor(),
            style = Fill,
        )
    }
    node.style.stroke?.let { stroke ->
        drawShape(
            color = stroke.color.toComposeColor(),
            style = stroke.toDrawStyle(),
        )
    }
}