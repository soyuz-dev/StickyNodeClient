package org.stickynodes.editor.draw.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import org.stickynodes.editor.draw.geometry.layoutStyle
import org.stickynodes.editor.draw.geometry.measuredBounds
import org.stickynodes.editor.draw.model.EditorNode

fun DrawScope.drawSelection(
    node: EditorNode,
    color: Color,
    textMeasurer: TextMeasurer,
) {
    val outline = node.measuredBounds(
        textMeasurer = textMeasurer,
        textStyle = { text -> text.layoutStyle(this) },
    ).inflate(3f)

    drawRect(
        color = color,
        topLeft = outline.topLeft,
        size = outline.size,
        style = Stroke(width = 2f),
    )
}
