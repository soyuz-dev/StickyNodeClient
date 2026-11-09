package org.stickynodes.editor.draw.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.geometry.connector.route
import org.stickynodes.editor.draw.geometry.connector.toPath
import org.stickynodes.editor.draw.model.EditorConnector
import kotlin.uuid.Uuid

fun DrawScope.drawConnector(
    connector: EditorConnector,
    nodePosition: (Uuid) -> Offset?,
    zoom: Float
) {
    require(zoom.isFinite() && zoom > 0f)

    val route = connector.route(nodePosition)

    drawPath(
        route.toPath(
            cornerRadius = 12.dp.toPx() / zoom
        ),
        color = Color(0xFF26A69A),
        style = Stroke(
            width = 2.5.dp.toPx() / zoom,
        )
    )
}