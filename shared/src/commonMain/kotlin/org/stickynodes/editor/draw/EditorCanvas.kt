package org.stickynodes.editor.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.contains
import org.stickynodes.editor.draw.geometry.groupRotationHandlePosition
import org.stickynodes.editor.draw.geometry.isRotationHandleHit
import org.stickynodes.editor.draw.geometry.layoutStyle
import org.stickynodes.editor.draw.geometry.measuredBounds
import org.stickynodes.editor.draw.geometry.measuredSelectionBounds
import org.stickynodes.editor.draw.geometry.resizeHandleAt
import org.stickynodes.editor.draw.geometry.rotationHandlePosition
import org.stickynodes.editor.draw.gesture.detectCanvasPan
import org.stickynodes.editor.draw.gesture.detectCanvasZoom
import org.stickynodes.editor.draw.gesture.detectNodeInteraction
import org.stickynodes.editor.draw.gesture.detectPathCreation
import org.stickynodes.editor.draw.gesture.detectShapeCreation
import org.stickynodes.editor.draw.gesture.detectTextCreation
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorGroup
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorShape
import org.stickynodes.editor.draw.model.EditorText
import org.stickynodes.editor.draw.render.drawNode
import org.stickynodes.editor.draw.render.drawSelection
import org.stickynodes.editor.draw.render.drawShapeSelection
import org.stickynodes.editor.draw.render.drawTextSelection
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.tools.EditorTool
import kotlin.uuid.Uuid

@Composable
fun EditorCanvas(
    modifier: Modifier = Modifier,
    nodes: List<EditorNode>,
    selectedTool: EditorTool,
    camera: EditorCamera,
    rectangleStyle: NodeStyle,
    ellipseStyle: NodeStyle,
    penStyle: NodeStyle,
    onCameraChanged: (EditorCamera) -> Unit,
    onNodeCreated: (EditorNode) -> Unit,
    onNodeUpdated: (EditorNode) -> Unit,
    onNodesUpdated: (List<EditorNode>) -> Unit,
    onTextPlacement: (Offset) -> Unit,
    selectedNodeIds: Set<Uuid>,
    onNodeSelected: (Uuid?) -> Unit,
    onNodesSelected: (Set<Uuid>) -> Unit,
    onNodeSelectionToggled: (Uuid) -> Unit,
    onTextEdit: (EditorText) -> Unit,
    onHitSelection: ((Uuid, Boolean) -> Set<Uuid>)? = null,
    selectedMembers: List<EditorGroupMember> = listOf(),
    ancestorGroups: List<EditorGroup> = listOf(),
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var previewNode by remember {
        mutableStateOf<EditorNode?>(null)
    }

    var interactionPreview by remember {
        mutableStateOf<List<EditorNode>>(emptyList())
    }

    var marquee by remember {
        mutableStateOf<Rect?>(null)
    }

    val selectionColor = Color(0xFF4492FF)
    val selectedGroups = selectedMembers.filterIsInstance<EditorGroup>()
    val selectedLeafIds = selectedMembers
        .filterIsInstance<EditorNode>()
        .mapTo(mutableSetOf()) { it.id }

    val currentCamera by rememberUpdatedState(camera)
    val currentNodes by rememberUpdatedState(nodes)
    val currentRectangleStyle by rememberUpdatedState(rectangleStyle)
    val currentEllipseStyle by rememberUpdatedState(ellipseStyle)
    val currentPenStyle by rememberUpdatedState(penStyle)
    val currentOnCameraChanged by rememberUpdatedState(onCameraChanged)
    val currentOnNodeCreated by rememberUpdatedState(onNodeCreated)
    val currentOnNodeUpdated by rememberUpdatedState(onNodeUpdated)
    val currentOnNodesUpdated by rememberUpdatedState(onNodesUpdated)
    val currentSelectedNodeIds by rememberUpdatedState(selectedNodeIds)
    val currentSelectedMembers by rememberUpdatedState(selectedMembers)
    val currentOnNodeSelectionToggled by rememberUpdatedState(onNodeSelectionToggled)
    val currentOnNodeSelected by rememberUpdatedState(onNodeSelected)
    val currentOnNodesSelected by rememberUpdatedState(onNodesSelected)
    val currentOnTextPlacement by rememberUpdatedState(onTextPlacement)
    val currentOnTextEdit by rememberUpdatedState(onTextEdit)
    val currentOnHitSelection by rememberUpdatedState(onHitSelection)

    Canvas(
        modifier = modifier
            .pointerInput(selectedTool, textMeasurer, density) {
                when (selectedTool) {
                    EditorTool.RECTANGLE,
                    EditorTool.ELLIPSE -> {
                        detectShapeCreation(
                            screenToWorld = {
                                currentCamera.screenToWorld(it)
                            },
                            createNode = { id, rect ->
                                when (selectedTool) {
                                    EditorTool.RECTANGLE -> EditorRectangle(
                                        id = id,
                                        x = rect.left,
                                        y = rect.top,
                                        width = rect.width,
                                        height = rect.height,
                                        style = currentRectangleStyle,
                                    )

                                    EditorTool.ELLIPSE -> EditorEllipse(
                                        id = id,
                                        x = rect.left,
                                        y = rect.top,
                                        width = rect.width,
                                        height = rect.height,
                                        style = currentEllipseStyle,
                                    )
                                }
                            },
                            onPreview = {
                                previewNode = it
                            },
                            onCommit = {
                                currentOnNodeCreated(it)
                            },
                        )
                    }

                    EditorTool.TEXT -> {
                        detectTextCreation(
                            screenToWorld = {
                                currentCamera.screenToWorld(it)
                            },
                            onCreate = {
                                currentOnTextPlacement(it)
                            },
                        )
                    }

                    EditorTool.PEN -> {
                        detectPathCreation(
                            screenToWorld = {
                                currentCamera.screenToWorld(it)
                            },
                            style = {
                                currentPenStyle
                            },
                            onPreview = {
                                previewNode = it
                            },
                            onCommit = {
                                currentOnNodeCreated(it)
                            },
                        )
                    }

                    EditorTool.SELECT -> {
                        detectNodeInteraction(
                            nodes = {
                                currentNodes
                            },
                            selectedNodeIds = {
                                currentSelectedNodeIds
                            },
                            screenToWorld = {
                                currentCamera.screenToWorld(it)
                            },
                            screenDeltaToWorld = {
                                it / currentCamera.zoom
                            },
                            zoom = {
                                currentCamera.zoom
                            },
                            hitTest = { node, point, zoom ->
                                node.contains(
                                    point = point,
                                    textMeasurer = textMeasurer,
                                    textStyle = { text ->
                                        text.layoutStyle(density)
                                    },
                                    zoom = zoom,
                                )
                            },
                            onToggleSelection = {
                                currentOnNodeSelectionToggled(it)
                            },
                            onSelectNodes = {
                                currentOnNodesSelected(it)
                            },
                            onMarquee = {
                                marquee = it
                            },
                            onSelect = {
                                currentOnNodeSelected(it)
                            },
                            onPreview = {
                                interactionPreview = it
                            },
                            onCommit = {
                                currentOnNodesUpdated(it)
                            },
                            resizeHandleAt = { screenPoint ->
                                if (currentSelectedMembers.any { it is EditorGroup }) {
                                    null
                                } else {
                                    val selected = currentNodes.filter {
                                        it.id in currentSelectedNodeIds
                                    }
                                    val shapes = selected.filterIsInstance<EditorShape>()

                                    if (
                                        selected.isEmpty() ||
                                        shapes.size != selected.size ||
                                        (shapes.size > 1 &&
                                                shapes.any { it.rotation != 0f })
                                    ) {
                                        null
                                    } else {
                                        shapes.resizeHandleAt(
                                            pointer = screenPoint,
                                            camera = currentCamera,
                                            padding = with(density) { 8.dp.toPx() },
                                            radius = with(density) { 10.dp.toPx() },
                                        )
                                    }
                                }
                            },
                            rotationHandleAt = { screenPoint ->
                                if (currentSelectedMembers.any { it is EditorGroup }) {
                                    null
                                } else {
                                    val selected = currentNodes.filter {
                                        it.id in currentSelectedNodeIds
                                    }

                                    val text = selected.singleOrNull() as? EditorText

                                    when {
                                        text != null -> {
                                            val bounds = text.measuredBounds(
                                                textMeasurer = textMeasurer,
                                                textStyle = { node ->
                                                    node.layoutStyle(density)
                                                },
                                            )

                                            val position = text.rotationHandlePosition(
                                                bounds = bounds,
                                                camera = currentCamera,
                                                padding = with(density) { 8.dp.toPx() },
                                                offset = with(density) { 28.dp.toPx() },
                                            )

                                            val radius = with(density) { 12.dp.toPx() }

                                            if (
                                                (screenPoint - position).getDistanceSquared() <=
                                                radius * radius
                                            ) {
                                                text.id
                                            } else {
                                                null
                                            }
                                        }

                                        selected.isNotEmpty() &&
                                                selected.all { it is EditorShape } -> {
                                            val shapes = selected.filterIsInstance<EditorShape>()

                                            if (
                                                shapes.isRotationHandleHit(
                                                    pointer = screenPoint,
                                                    camera = currentCamera,
                                                    offset = with(density) { 28.dp.toPx() },
                                                    radius = with(density) { 12.dp.toPx() },
                                                )
                                            ) {
                                                shapes.first().id
                                            } else {
                                                null
                                            }
                                        }

                                        else -> null
                                    }
                                }
                            },
                            onEditText = { node ->
                                currentOnTextEdit(node)
                            },
                            selectionBounds = { node ->
                                node.measuredSelectionBounds(
                                    textMeasurer = textMeasurer,
                                    textStyle = { text ->
                                        text.layoutStyle(density)
                                    },
                                )
                            },
                            onHitSelection = currentOnHitSelection,
                            groupRotationHandleAt = { screenPoint ->
                                if (currentSelectedMembers.none { it is EditorGroup }) false
                                else {
                                    val position = currentNodes.groupRotationHandlePosition(
                                        selectedNodeIds = currentSelectedNodeIds,
                                        camera = currentCamera,
                                        textMeasurer = textMeasurer,
                                        textStyle = { text -> text.layoutStyle(density)},
                                        padding = with(density) { 8.dp.toPx() },
                                        offset = with(density) { 28.dp.toPx() },
                                    )

                                    val radius = with(density) { 12.dp.toPx() }

                                    position != null &&
                                            (screenPoint - position).getDistanceSquared() <=
                                            radius * radius
                                }
                            },
                        )
                    }

                    EditorTool.PAN -> {
                        detectCanvasPan(
                            camera = {
                                currentCamera
                            },
                            onCameraChanged = {
                                currentOnCameraChanged(it)
                            },
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                detectCanvasZoom(
                    camera = {
                        currentCamera
                    },
                    onCameraChanged = {
                        currentOnCameraChanged(it)
                    },
                )
            },
    ) {
        scale(
            scale = camera.zoom,
            pivot = Offset.Zero,
        ) {
            translate(
                left = -camera.position.x,
                top = -camera.position.y,
            ) {
                val previewById = interactionPreview.associateBy {
                    it.id
                }

                for (node in nodes) {
                    val displayedNode = previewById[node.id]
                        ?: previewNode?.takeIf {
                            it.id == node.id
                        }
                        ?: node

                    if (displayedNode.id == previewNode?.id) {
                        continue
                    }

                    drawNode(
                        node = displayedNode,
                        textMeasurer = textMeasurer,
                    )
                }

                previewNode?.let {
                    drawNode(
                        node = it,
                        textMeasurer = textMeasurer,
                        alpha = 0.6f,
                    )
                }

                val visibleSelectionIds = if (selectedGroups.isEmpty()) {
                    selectedNodeIds
                } else {
                    selectedLeafIds
                }

                val selected = nodes.filter {
                    it.id in visibleSelectionIds
                }

                val allSelectedAreShapes =
                    selected.isNotEmpty() &&
                            selected.size == selectedNodeIds.size &&
                            selected.all { it is EditorShape }

                if (!allSelectedAreShapes) {
                    val singleTextSelected =
                        selectedGroups.isEmpty() &&
                                selected.size == 1 &&
                                selected.single() is EditorText

                    if (!singleTextSelected) {
                        for (node in selected) {
                            drawSelection(
                                node = previewById[node.id] ?: node,
                                color = selectionColor,
                                textMeasurer = textMeasurer,
                            )
                        }
                    }
                }

                marquee?.let { rect ->
                    drawRect(
                        color = selectionColor.copy(alpha = 0.12f),
                        topLeft = rect.topLeft,
                        size = rect.size,
                    )

                    drawRect(
                        color = selectionColor,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(
                            width = 1.5.dp.toPx() / camera.zoom,
                        ),
                    )
                }
            }
        }

        if (selectedTool == EditorTool.SELECT) {
            val selected = nodes.filter {
                it.id in selectedNodeIds
            }

            if (selectedGroups.isEmpty()) {
                val text = selected.singleOrNull() as? EditorText

                if (text != null) {
                    val displayedText = interactionPreview
                        .firstOrNull { it.id == text.id } as? EditorText
                        ?: text

                    drawTextSelection(
                        text = displayedText,
                        camera = camera,
                        color = selectionColor,
                        textMeasurer = textMeasurer,
                    )
                }
            }

            if (
                selectedGroups.isEmpty() &&
                selected.isNotEmpty() &&
                selected.all { it is EditorShape }
            ) {
                val previewById = interactionPreview.associateBy {
                    it.id
                }

                val displayedShapes = selected.map { node ->
                    (previewById[node.id] as? EditorShape)
                        ?: (node as EditorShape)
                }

                drawShapeSelection(
                    shapes = displayedShapes,
                    camera = camera,
                    color = selectionColor,
                )
            }

            val previewById = interactionPreview.associateBy { it.id }

            for (group in ancestorGroups + selectedGroups) {
                val bounds = group.flattenedNodes()
                    .map { node ->
                        val displayedNode = previewById[node.id] ?: node
                        displayedNode.measuredSelectionBounds(
                            textMeasurer = textMeasurer,
                            textStyle = { text -> text.layoutStyle(density) },
                        )
                    }
                    .reduceOrNull { a, b ->
                        Rect(
                            left = minOf(a.left, b.left),
                            top = minOf(a.top, b.top),
                            right = maxOf(a.right, b.right),
                            bottom = maxOf(a.bottom, b.bottom),
                        )
                    } ?: continue

                val screenBounds = Rect(
                    camera.worldToScreen(bounds.topLeft),
                    camera.worldToScreen(bounds.bottomRight),
                ).inflate(8.dp.toPx())

                val isAncestor = group in ancestorGroups

                drawRoundRect(
                    color = selectionColor.copy(
                        alpha = if (isAncestor) 0.5f else 1f
                    ),
                    topLeft = screenBounds.topLeft,
                    size = screenBounds.size,
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(
                        width = if (isAncestor) 1.dp.toPx() else 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            if (isAncestor) {
                                floatArrayOf(5.dp.toPx(), 5.dp.toPx())
                            } else {
                                floatArrayOf(9.dp.toPx(), 5.dp.toPx())
                            },
                        ),
                    ),
                )
            }

            if (selectedGroups.isNotEmpty()) {
                val previewById = interactionPreview.associateBy { it.id }

                val displayedNodes = nodes.map { node ->
                    previewById[node.id] ?: node
                }

                val position = displayedNodes.groupRotationHandlePosition(
                    selectedNodeIds = selectedNodeIds,
                    camera = camera,
                    textMeasurer = textMeasurer,
                    textStyle = { text -> text.layoutStyle(density) },
                    padding = 8.dp.toPx(),
                    offset = 28.dp.toPx(),
                )

                if (position != null) {
                    val radius = 7.dp.toPx()

                    drawLine(
                        color = selectionColor,
                        start = position + Offset(0f, 28.dp.toPx()),
                        end = position,
                        strokeWidth = 1.5.dp.toPx()
                    )

                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = position,
                    )

                    drawCircle(
                        color = selectionColor,
                        radius = radius,
                        center = position,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun EditorCanvasPreview() {
    var nodes by remember {
        mutableStateOf<List<EditorNode>>(listOf())
    }

    var selectedNodeId by remember {
        mutableStateOf<Uuid?>(null)
    }

    var camera by remember {
        mutableStateOf(EditorCamera())
    }

    EditorCanvas(
        modifier = Modifier.size(400.dp),
        nodes = nodes,
        selectedTool = EditorTool.RECTANGLE,
        camera = camera,
        rectangleStyle = NodeStyle(),
        ellipseStyle = NodeStyle(),
        penStyle = NodeStyle(
            fill = FillStyle.None,
            stroke = StrokeStyle(
                color = 0xFF264B72.toInt(),
                width = 3f,
            ),
        ),
        onCameraChanged = {
            camera = it
        },
        onNodeCreated = {
            nodes += it
        },
        onNodeUpdated = { updated ->
            nodes = nodes.map { node ->
                if (node.id == updated.id) updated else node
            }
        },
        onNodesUpdated = { updated ->
            val replacements = updated.associateBy {
                it.id
            }

            nodes = nodes.map { node ->
                replacements[node.id] ?: node
            }
        },
        selectedNodeIds = selectedNodeId?.let(::setOf) ?: emptySet(),
        onNodeSelectionToggled = { id ->
            selectedNodeId =
                if (selectedNodeId == id) null else id
        },
        onNodeSelected = {
            selectedNodeId = it
        },
        onNodesSelected = { ids ->
            selectedNodeId = ids.singleOrNull()
        },
        onTextPlacement = {},
        onTextEdit = {},
    )
}