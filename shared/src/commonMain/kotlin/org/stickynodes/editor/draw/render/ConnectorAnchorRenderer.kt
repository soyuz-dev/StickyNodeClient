package org.stickynodes.editor.draw.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.geometry.connector.ConnectorAnchor
import org.stickynodes.editor.draw.geometry.connector.anchorPosition
import org.stickynodes.editor.draw.model.EditorShape

fun DrawScope.drawConnectorAnchors(
    shape: EditorShape,
    zoom: Float,
    selectedAnchors: Set<ConnectorAnchor> = setOf(),
) {
    require(zoom.isFinite() && zoom > 0f)

    val normalRadius = 4.dp.toPx() / zoom
    val selectedRadius = 7.dp.toPx() / zoom
    val outlineWidth = 1.5.dp.toPx() / zoom

    val normalColor = Color(0xFF4492FF)
    val selectedColor = Color(0xFF26A69A)

    for (anchor in ConnectorAnchor.entries) {
        val selected = anchor in selectedAnchors
        val position = shape.anchorPosition(anchor)

        drawCircle(
            color = if (selected) selectedColor else Color.White,
            radius = if (selected) selectedRadius else normalRadius,
            center = position,
        )

        drawCircle(
            color = if (selected) selectedColor else normalColor,
            radius = if (selected) selectedRadius else normalRadius,
            center = position,
            style = Stroke(width = outlineWidth),
        )
    }
}