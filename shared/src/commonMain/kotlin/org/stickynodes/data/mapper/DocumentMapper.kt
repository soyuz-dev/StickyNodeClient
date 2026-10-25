package org.stickynodes.data.mapper

import androidx.compose.ui.geometry.Offset
import org.stickynodes.data.model.DocumentDto
import org.stickynodes.data.model.EllipseDto
import org.stickynodes.data.model.NodeDto
import org.stickynodes.data.model.PathDto
import org.stickynodes.data.model.PointDto
import org.stickynodes.data.model.RectangleDto
import org.stickynodes.data.model.StrokeDto
import org.stickynodes.data.model.StyleDto
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeCap
import org.stickynodes.editor.draw.style.StrokeJoin
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.history.EditorDocument
import kotlin.uuid.Uuid

fun EditorDocument.toDto(): DocumentDto = DocumentDto(
    nodes = nodes.map { it.toDto() }
)

fun DocumentDto.toDocument(): EditorDocument {
    require(version == 1) {
        "Unsupported document version: $version"
    }

    return EditorDocument(
        nodes = nodes.map { it.toNode() },
    )
}

private fun EditorNode.toDto(): NodeDto = when (this) {
    is EditorRectangle -> RectangleDto(
        id = id.toString(),
        x = x,
        y = y,
        width = width,
        height = height,
        style = style.toDto(),
    )

    is EditorEllipse -> EllipseDto(
        id = id.toString(),
        x = x,
        y = y,
        width = width,
        height = height,
        style = style.toDto(),
    )

    is EditorPath -> PathDto(
        id = id.toString(),
        x = x,
        y = y,
        style = style.toDto(),
        points = points.map {
            PointDto(it.x, it.y)
        },
    )
}


private fun NodeDto.toNode(): EditorNode = when (this) {
    is RectangleDto -> EditorRectangle(
        id = Uuid.parse(id),
        x = x,
        y = y,
        width = width,
        height = height,
        style = style.toStyle(),
    )

    is EllipseDto -> EditorEllipse(
        id = Uuid.parse(id),
        x = x,
        y = y,
        width = width,
        height = height,
        style = style.toStyle(),
    )

    is PathDto -> EditorPath(
        id = Uuid.parse(id),
        x = x,
        y = y,
        style = style.toStyle(),
        points = points.map {
            Offset(it.x, it.y)
        },
    )
}

private fun NodeStyle.toDto(): StyleDto = StyleDto(
    fillColor = when (val fill = fill) {
        FillStyle.None -> null
        is FillStyle.Solid -> fill.color
    },
    stroke = stroke?.let {
        StrokeDto(
            color = it.color,
            width = it.width,
            cap = when (it.cap) {
                StrokeCap.Round -> "round"
                StrokeCap.Square -> "square"
            },
            join = when (it.join) {
                StrokeJoin.Round -> "round"
                StrokeJoin.Bevel -> "bevel"
            },
            dashPattern = it.dashPattern,
        )
    },
)

private fun StyleDto.toStyle(): NodeStyle = NodeStyle(
    fill = fillColor?.let {
        FillStyle.Solid(it)
    } ?: FillStyle.None,

    stroke = stroke?.let {
        StrokeStyle(
            color = it.color,
            width = it.width,
            cap = when (it.cap) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> error("Unknown stroke cap: ${it.cap}")
            },
            join = when (it.join) {
                "round" -> StrokeJoin.Round
                "bevel" -> StrokeJoin.Bevel
                else -> error("Unknown stroke join: ${it.join}")
            },
            dashPattern = it.dashPattern,
        )
    },
)