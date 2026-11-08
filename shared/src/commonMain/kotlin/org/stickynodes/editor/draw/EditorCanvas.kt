package org.stickynodes.editor.draw

import androidx.compose.animation.core.snap
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
import androidx.compose.ui.geometry.Size
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
import org.stickynodes.editor.draw.geometry.commonRotationOrNull
import org.stickynodes.editor.draw.geometry.contains
import org.stickynodes.editor.draw.geometry.groupRotationHandlePosition
import org.stickynodes.editor.draw.geometry.isRotationHandleHit
import org.stickynodes.editor.draw.geometry.layoutStyle
import org.stickynodes.editor.draw.geometry.measuredBounds
import org.stickynodes.editor.draw.geometry.measuredSelectionBounds
import org.stickynodes.editor.draw.geometry.orientedGroupBoundsOrNull
import org.stickynodes.editor.draw.geometry.orientedResizeHandleScreenPositions
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
import androidx.compose.ui.graphics.Path
import org.stickynodes.editor.draw.geometry.ResizeHandle
import org.stickynodes.editor.draw.geometry.SnapResult
import org.stickynodes.editor.draw.geometry.SpacingPlacement
import org.stickynodes.editor.draw.geometry.orientedResizeHandleAt
import org.stickynodes.editor.draw.geometry.orientedShapeSelectionOrNull

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

    var snapResult by remember {
        mutableStateOf<SnapResult?>(null)
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
                            groupResizeHandleAt = { screenPoint ->
                                if (currentSelectedMembers.none { it is EditorGroup }) {
                                    null
                                } else {
                                    val selectedNodes = currentNodes.filter { node ->
                                        node.id in currentSelectedNodeIds
                                    }

                                    val shapes = selectedNodes.filterIsInstance<EditorShape>()

                                    if (
                                        selectedNodes.isEmpty() ||
                                        shapes.size != selectedNodes.size
                                    ) {
                                        null
                                    } else {
                                        val rotation = shapes.commonRotationOrNull()

                                        val bounds = if (rotation != null) {
                                            shapes.orientedGroupBoundsOrNull {
                                                (it as EditorShape).bounds
                                            }
                                        } else {
                                            null
                                        }

                                        if (rotation == null || bounds == null) {
                                            null
                                        } else {
                                            bounds.orientedResizeHandleAt(
                                                pointer = screenPoint,
                                                rotation = rotation,
                                                camera = currentCamera,
                                                padding = with(density) { 8.dp.toPx() },
                                                radius = with(density) { 10.dp.toPx() },
                                            )
                                        }
                                    }
                                }
                            },
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
                                        useOrientedBounds =
                                            currentSelectedMembers.any { it is EditorGroup },
                                    )

                                    val radius = with(density) { 12.dp.toPx() }

                                    position != null &&
                                            (screenPoint - position).getDistanceSquared() <=
                                            radius * radius
                                }
                            },
                            onSnapChanged = { snapResult = it }
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

                if (selectedTool == EditorTool.SELECT) {
                    snapResult?.let { snap ->
                        val guideColor = Color(0xFFDD4FAF)
                        val guideWidth = 1.25.dp.toPx() / camera.zoom

                        snap.verticalGuide?.let { x ->
                            snap.verticalSpan?.let { span ->
                                drawLine(
                                    color = guideColor,
                                    start = Offset(x, span.start),
                                    end = Offset(x, span.end),
                                    strokeWidth = guideWidth,
                                )
                            }
                        }

                        snap.horizontalGuide?.let { y ->
                            snap.horizontalSpan?.let { span ->
                                drawLine(
                                    color = guideColor,
                                    start = Offset(span.start, y),
                                    end = Offset(span.end, y),
                                    strokeWidth = guideWidth,
                                )
                            }
                        }


// Equal-spacing indicators.
                        snap.snappedBounds?.let { moving ->

                            fun gapY(a: Rect, b: Rect): Float {
                                val top = maxOf(a.top, b.top)
                                val bottom = minOf(a.bottom, b.bottom)

                                return if (top <= bottom) {
                                    (top + bottom) / 2f
                                } else {
                                    (a.center.y + b.center.y) / 2f
                                }
                            }

                            fun gapX(a: Rect, b: Rect): Float {
                                val left = maxOf(a.left, b.left)
                                val right = minOf(a.right, b.right)

                                return if (left <= right) {
                                    (left + right) / 2f
                                } else {
                                    (a.center.x + b.center.x) / 2f
                                }
                            }

                            snap.horizontalSpacing?.let { spacing ->
                                val first = spacing.firstTarget
                                val second = spacing.secondTarget

                                val gaps = when (spacing.placement) {
                                    SpacingPlacement.BEFORE -> listOf(
                                        Triple(moving.right, first.left, moving to first),
                                        Triple(first.right, second.left, first to second),
                                    )

                                    SpacingPlacement.BETWEEN -> listOf(
                                        Triple(first.right, moving.left, first to moving),
                                        Triple(moving.right, second.left, moving to second),
                                    )

                                    SpacingPlacement.AFTER -> listOf(
                                        Triple(first.right, second.left, first to second),
                                        Triple(second.right, moving.left, second to moving),
                                    )
                                }

                                for ((start, end, pair) in gaps) {
                                    val y = gapY(pair.first, pair.second)

                                    drawLine(
                                        color = guideColor,
                                        start = Offset(start, y),
                                        end = Offset(end, y),
                                        strokeWidth = guideWidth,
                                    )
                                }
                            }

                            snap.verticalSpacing?.let { spacing ->
                                val first = spacing.firstTarget
                                val second = spacing.secondTarget

                                val gaps = when (spacing.placement) {
                                    SpacingPlacement.BEFORE -> listOf(
                                        Triple(moving.bottom, first.top, moving to first),
                                        Triple(first.bottom, second.top, first to second),
                                    )

                                    SpacingPlacement.BETWEEN -> listOf(
                                        Triple(first.bottom, moving.top, first to moving),
                                        Triple(moving.bottom, second.top, moving to second),
                                    )

                                    SpacingPlacement.AFTER -> listOf(
                                        Triple(first.bottom, second.top, first to second),
                                        Triple(second.bottom, moving.top, second to moving),
                                    )
                                }

                                for ((start, end, pair) in gaps) {
                                    val x = gapX(pair.first, pair.second)

                                    drawLine(
                                        color = guideColor,
                                        start = Offset(x, start),
                                        end = Offset(x, end),
                                        strokeWidth = guideWidth,
                                    )
                                }
                            }
                        }
                    }
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


                val isAncestor = group in ancestorGroups

                val screenBounds = Rect(
                    camera.worldToScreen(bounds.topLeft),
                    camera.worldToScreen(bounds.bottomRight),
                ).inflate(8.dp.toPx())

                if (
                    !isAncestor &&
                    selectedGroups.size == 1 &&
                    selectedMembers.size == 1
                ) {
                    val displayedGroupNodes = group.flattenedNodes().map { node ->
                        previewById[node.id] ?: node
                    }

                    val selection = displayedGroupNodes
                        .orientedShapeSelectionOrNull()

                    if (selection != null) {
                        val positions = selection.bounds
                            .orientedResizeHandleScreenPositions(
                                rotation = selection.rotation,
                                camera = camera,
                                padding = 8.dp.toPx(),
                            )

                        val nw = positions.getValue(ResizeHandle.NW)
                        val ne = positions.getValue(ResizeHandle.NE)
                        val se = positions.getValue(ResizeHandle.SE)
                        val sw = positions.getValue(ResizeHandle.SW)

                        val outline = Path().apply {
                            moveTo(nw.x, nw.y)
                            lineTo(ne.x, ne.y)
                            lineTo(se.x, se.y)
                            lineTo(sw.x, sw.y)
                            close()
                        }

                        drawPath(
                            path = outline,
                            color = selectionColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        9.dp.toPx(),
                                        5.dp.toPx(),
                                    ),
                                ),
                            ),
                        )

                        continue
                    }
                }

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


                val selectedNodes = displayedNodes.filter { node ->
                    node.id in selectedNodeIds
                }


                val selection = selectedNodes.orientedShapeSelectionOrNull()

                if (selection != null) {
                    val handleSize = 10.dp.toPx()
                    val outlineWidth = 1.5.dp.toPx()

                    val positions = selection.bounds
                        .orientedResizeHandleScreenPositions(
                            rotation = selection.rotation,
                            camera = camera,
                            padding = 8.dp.toPx(),
                        )

                    if (selectedMembers.size > 1) {
                        val nw = positions.getValue(ResizeHandle.NW)
                        val ne = positions.getValue(ResizeHandle.NE)
                        val se = positions.getValue(ResizeHandle.SE)
                        val sw = positions.getValue(ResizeHandle.SW)

                        val outline = Path().apply {
                            moveTo(nw.x, nw.y)
                            lineTo(ne.x, ne.y)
                            lineTo(se.x, se.y)
                            lineTo(sw.x, sw.y)
                            close()
                        }

                        drawPath(
                            path = outline,
                            color = selectionColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        9.dp.toPx(),
                                        5.dp.toPx(),
                                    ),
                                ),
                            ),
                        )
                    }

                    for (position in positions.values) {
                        val topLeft = position - Offset(
                            handleSize / 2f,
                            handleSize / 2f,
                        )

                        drawRoundRect(
                            color = Color.White,
                            topLeft = topLeft,
                            size = Size(handleSize, handleSize),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                        )

                        drawRoundRect(
                            color = selectionColor,
                            topLeft = topLeft,
                            size = Size(handleSize, handleSize),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            style = Stroke(width = outlineWidth),
                        )
                    }
                }

                val useOrientedBounds = selectedGroups.isNotEmpty()

                val padding = 8.dp.toPx()
                val handleOffset = 28.dp.toPx()

                fun handlePosition(offset: Float) =
                    displayedNodes.groupRotationHandlePosition(
                        selectedNodeIds = selectedNodeIds,
                        camera = camera,
                        textMeasurer = textMeasurer,
                        textStyle = { text -> text.layoutStyle(density) },
                        padding = padding,
                        offset = offset,
                        useOrientedBounds = useOrientedBounds,
                    )

                val stemStart = handlePosition(0f)
                val position = handlePosition(handleOffset)

                if (stemStart != null && position != null) {
                    val radius = 7.dp.toPx()

                    drawLine(
                        color = selectionColor,
                        start = stemStart,
                        end = position,
                        strokeWidth = 1.5.dp.toPx(),
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