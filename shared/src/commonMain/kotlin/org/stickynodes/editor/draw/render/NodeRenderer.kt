
package org.stickynodes.editor.draw.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import org.stickynodes.editor.draw.geometry.smoothPath
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.style.FillStyle

fun DrawScope.drawNode(
    node: EditorNode,
    alpha: Float = 1f,
) {
    fun drawShape(
        color: Color,
        style: DrawStyle,
    ) {
        when (node) {
            is EditorRectangle -> drawRect(
                color = color,
                topLeft = Offset(node.x, node.y),
                size = Size(node.width, node.height),
                style = style,
                alpha = alpha,
            )

            is EditorEllipse -> drawOval(
                color = color,
                topLeft = Offset(node.x, node.y),
                size = Size(node.width, node.height),
                style = style,
                alpha = alpha,
            )

            is EditorPath -> {
                if(node.points.isEmpty()) return

                if(node.points.size == 1) {
                    val point = node.points[0]

                    if(style is Stroke) {
                        drawCircle(
                            color = color,
                            radius = style.width/2f,
                            center = Offset(
                                node.x + point.x,
                                node.y + point.y
                            ),
                            alpha
                        )
                    }

                    return
                }

                val worldPoints = node.points.map { point ->
                    Offset(
                        x = node.x + point.x,
                        y = node.y + point.y,
                    )
                }

                drawPath(
                    path = smoothPath(worldPoints),
                    color = color,
                    style = style,
                    alpha = alpha,
                )
            }
        }
    }

    // Filled areas are meaningful for closed shapes.
    if (node !is EditorPath) {
        when (val fill = node.style.fill) {
            FillStyle.None -> Unit

            is FillStyle.Solid -> drawShape(
                color = fill.color.toComposeColor(),
                style = Fill,
            )
        }
    }

    // Strokes apply to both shapes and freehand paths.
    node.style.stroke?.let { stroke ->
        drawShape(
            color = stroke.color.toComposeColor(),
            style = stroke.toDrawStyle(),
        )
    }
}
