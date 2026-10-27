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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import org.stickynodes.editor.draw.geometry.layoutStyle
import org.stickynodes.editor.draw.gesture.detectCanvasPan
import org.stickynodes.editor.draw.gesture.detectCanvasZoom
import org.stickynodes.editor.draw.gesture.detectNodeInteraction
import org.stickynodes.editor.draw.gesture.detectPathCreation
import org.stickynodes.editor.draw.gesture.detectShapeCreation
import org.stickynodes.editor.draw.gesture.detectTextCreation
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorText
import org.stickynodes.editor.draw.render.drawNode
import org.stickynodes.editor.draw.render.drawSelection
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
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var previewNode by remember { mutableStateOf<EditorNode?>(null) }

    var interactionPreview by remember { mutableStateOf<List<EditorNode>>(listOf()) }

    val selectionColor = Color(0xFF4492FF)

    // Keep callbacks and state current inside long-running pointerInput handlers.
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
    val currentOnNodeSelectionToggled by rememberUpdatedState(onNodeSelectionToggled)
    val currentOnNodeSelected by rememberUpdatedState(onNodeSelected)
    val currentOnNodesSelected by rememberUpdatedState(onNodesSelected)
    val currentOnTextPlacement by rememberUpdatedState(onTextPlacement)

    var marquee by remember { mutableStateOf<Rect?>(null) }

    Canvas(
        modifier = modifier
            .pointerInput(selectedTool, textMeasurer, density) {
                when (selectedTool) {
                    EditorTool.RECTANGLE, EditorTool.ELLIPSE -> {
                        detectShapeCreation(
                            screenToWorld = { currentCamera.screenToWorld(it) },
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
                                    else -> error("Unsupported creation tool")
                                }
                            },
                            onPreview = { previewNode = it },
                            onCommit = { currentOnNodeCreated(it) },
                        )
                    }

                    EditorTool.TEXT -> {
                        detectTextCreation(
                            screenToWorld = {camera.screenToWorld(it)},
                            onCreate = { currentOnTextPlacement(it) }
                        )
                    }

                    EditorTool.PEN -> {
                        detectPathCreation(
                            screenToWorld = { currentCamera.screenToWorld(it) },
                            style = { currentPenStyle },
                            onPreview = { previewNode = it },
                            onCommit = { currentOnNodeCreated(it) },
                        )
                    }

                    EditorTool.SELECT -> {
                        detectNodeInteraction(
                            nodes = { currentNodes },
                            screenToWorld = { currentCamera.screenToWorld(it) },
                            screenDeltaToWorld = { it / currentCamera.zoom },
                            zoom = { currentCamera.zoom },
                            hitTest = { node, point, zoom ->
                                node.contains(
                                    point = point,
                                    textMeasurer = textMeasurer,
                                    textStyle = { text -> text.layoutStyle(density) },
                                    zoom = zoom,
                                )
                            },
                            selectedNodeIds = { currentSelectedNodeIds },
                            onToggleSelection = { currentOnNodeSelectionToggled(it) },
                            onSelectNodes = { currentOnNodesSelected(it) },
                            onMarquee = { marquee = it },
                            onSelect = {currentOnNodeSelected(it)},
                            onPreview = { interactionPreview = it },
                            onCommit = { currentOnNodesUpdated(it) },
                        )
                    }

                    EditorTool.PAN -> {
                        detectCanvasPan(
                            camera = { currentCamera },
                            onCameraChanged = currentOnCameraChanged,
                        )
                    }

                    else -> Unit
                }
            }
            .pointerInput(Unit) {
                detectCanvasZoom(
                    camera = { currentCamera },
                    onCameraChanged = { currentOnCameraChanged(it) },
                )
            },
    ) {
        scale(scale = camera.zoom, pivot = Offset.Zero) {
            translate(
                left = -camera.position.x,
                top = -camera.position.y,
            ) {
                val previewById = interactionPreview.associateBy { it.id }

                for (node in nodes) {
                    val displayedNode = previewById[node.id]
                        ?: previewNode?.takeIf { it.id == node.id }
                        ?: node

                    if (displayedNode.id == previewNode?.id) continue

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

                for (node in nodes) {
                    if (node.id !in selectedNodeIds) continue

                    drawSelection(
                        node = previewById[node.id] ?: node,
                        color = selectionColor,
                        textMeasurer = textMeasurer,
                    )
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
                        style = Stroke(width = 1.5.dp.toPx() / camera.zoom),
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun EditorCanvasPreview() {
    var nodes by remember { mutableStateOf<List<EditorNode>>(listOf()) }
    var selectedNodeId by remember { mutableStateOf<Uuid?>(null) }
    var camera by remember { mutableStateOf(EditorCamera()) }

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
        onCameraChanged = { camera = it },
        onNodeCreated = { nodes += it },
        onNodeUpdated = { updated ->
            nodes = nodes.map { node ->
                if (node.id == updated.id) updated else node
            }
        },
        onNodesUpdated = { updated ->
            val replacements = updated.associateBy { it.id }

            nodes = nodes.map { node ->
                replacements[node.id] ?: node
            }
        },
        selectedNodeIds = selectedNodeId?.let(::setOf) ?: emptySet(),
        onNodeSelectionToggled = { id -> selectedNodeId = if (selectedNodeId == id) null else id },
        onNodeSelected = { selectedNodeId = it },
        onNodesSelected = { ids -> selectedNodeId = ids.singleOrNull() },
        onTextPlacement = {},
    )
}
