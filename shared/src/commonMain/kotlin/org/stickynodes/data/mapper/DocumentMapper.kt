
package org.stickynodes.data.mapper

import androidx.compose.ui.geometry.Offset
import org.stickynodes.data.model.ConnectorDto
import org.stickynodes.data.model.ConnectorEndpointDto
import org.stickynodes.data.model.DocumentDto
import org.stickynodes.data.model.EllipseDto
import org.stickynodes.data.model.GroupDto
import org.stickynodes.data.model.MemberDto
import org.stickynodes.data.model.NodeDto
import org.stickynodes.data.model.PathDto
import org.stickynodes.data.model.PointDto
import org.stickynodes.data.model.RectangleDto
import org.stickynodes.data.model.StrokeDto
import org.stickynodes.data.model.StyleDto
import org.stickynodes.data.model.TextDto
import org.stickynodes.editor.draw.model.ConnectorEndpoint
import org.stickynodes.editor.draw.model.EditorConnector
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorGroup
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorText
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeCap
import org.stickynodes.editor.draw.style.StrokeJoin
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.history.EditorDocument
import kotlin.uuid.Uuid

fun EditorDocument.toDto(): DocumentDto = DocumentDto(
    version = 3,
    members = members.map { it.toDto() },
    connectors = connectors.map { it.toDto() }
)

fun DocumentDto.toDocument(): EditorDocument {
    require(version in 1..3) {
        "Unsupported document version: $version"
    }

    val document = EditorDocument(
        members = members.map { it.toMember() },
        connectors = connectors.map { it.toConnector() }
    )

    document.validateMemberIds()

    return document
}


private fun EditorDocument.validateMemberIds() {
    val encountered = mutableSetOf<Uuid>()

    fun validate(member: EditorGroupMember) {
        require(encountered.add(member.id)) {
            "Duplicate member ID: ${member.id}"
        }

        if (member is EditorGroup) {
            member.members.forEach(::validate)
        }
    }

    members.forEach(::validate)

    connectors.forEach { connector ->
        require(encountered.add(connector.id)) {
            "Duplicate connector ID: ${connector.id}"
        }
    }
}

private fun EditorGroupMember.toDto(): MemberDto = when (this) {
    is EditorNode -> toDto()

    is EditorGroup -> GroupDto(
        id = id.toString(),
        members = members.map { it.toDto() },
    )
}

private fun MemberDto.toMember(): EditorGroupMember = when (this) {
    is NodeDto -> toNode()

    is GroupDto -> EditorGroup(
        id = Uuid.parse(id),
        members = members.map { it.toMember() },
    )
}

private fun EditorNode.toDto(): NodeDto = when (this) {
    is EditorRectangle -> RectangleDto(
        id = id.toString(),
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
        style = style.toDto(),
    )

    is EditorEllipse -> EllipseDto(
        id = id.toString(),
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
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

    is EditorText -> TextDto(
        id = id.toString(),
        x = x,
        y = y,
        style = style.toDto(),
        text = text,
        fontSize = fontSize,
        rotation = rotation,
    )
}

private fun NodeDto.toNode(): EditorNode = when (this) {
    is RectangleDto -> EditorRectangle(
        id = Uuid.parse(id),
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
        style = style.toStyle(),
    )

    is EllipseDto -> EditorEllipse(
        id = Uuid.parse(id),
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
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

    is TextDto -> EditorText(
        id = Uuid.parse(id),
        x = x,
        y = y,
        style = style.toStyle(),
        text = text,
        fontSize = fontSize,
        rotation = rotation,
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

private fun EditorConnector.toDto(): ConnectorDto =
    ConnectorDto(
        id = id.toString(),
        start = start.toDto(),
        end = end.toDto(),
    )

private fun ConnectorDto.toConnector(): EditorConnector =
    EditorConnector(
        id = Uuid.parse(id),
        start = start.toEndpoint(),
        end = end.toEndpoint(),
    )

private fun ConnectorEndpoint.toDto(): ConnectorEndpointDto =
    when (this) {
        is ConnectorEndpoint.Free ->
            ConnectorEndpointDto.Free(
                position = PointDto(
                    position.x,
                    position.y
                )
            )

        is ConnectorEndpoint.Attached ->
            ConnectorEndpointDto.Attached(
                nodeId = nodeId.toString(),
                fallbackPosition = PointDto(
                    fallbackPosition.x,
                    fallbackPosition.y
                )
            )
    }

private fun ConnectorEndpointDto.toEndpoint(): ConnectorEndpoint =
    when (this) {
        is ConnectorEndpointDto.Free ->
            ConnectorEndpoint.Free(
                position = Offset(
                    position.x,
                    position.y
                )
            )

        is ConnectorEndpointDto.Attached ->
            ConnectorEndpoint.Attached(
                nodeId = Uuid.parse(nodeId),
                fallbackPosition = Offset(
                    fallbackPosition.x,
                    fallbackPosition.y
                )
            )
    }