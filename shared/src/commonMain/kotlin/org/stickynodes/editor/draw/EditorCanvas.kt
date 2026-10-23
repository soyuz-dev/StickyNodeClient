package org.stickynodes.editor.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.EditorTool
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.gesture.detectCanvasPan
import org.stickynodes.editor.draw.gesture.detectCanvasZoom
import org.stickynodes.editor.draw.gesture.detectNodeMovement
import org.stickynodes.editor.draw.gesture.detectNodeSelection
import org.stickynodes.editor.draw.gesture.detectShapeCreation
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.render.drawNode
import org.stickynodes.editor.draw.render.drawSelection
import kotlin.uuid.Uuid

@Composable
fun EditorCanvas(
    modifier: Modifier = Modifier,
    nodes: List<EditorNode>,
    selectedTool: EditorTool,
    camera: EditorCamera,
    onCameraChanged: (EditorCamera) -> Unit,
    onNodeCreated: (EditorNode) -> Unit,
    onNodeUpdated: (EditorNode) -> Unit,
    selectedNodeId: Uuid?,
    onNodeSelected: (Uuid?) -> Unit,
) {
    var previewNode by remember {
        mutableStateOf<EditorNode?>(null)
    }

    val nodeColor = MaterialTheme.colorScheme.secondaryContainer
    val selectionColor = Color(0xFF4492FF)

    val currentCamera by rememberUpdatedState(camera)
    val currentNodes by rememberUpdatedState(nodes)

    val currentOnCameraChanged by rememberUpdatedState(onCameraChanged)
    val currentOnNodeCreated by rememberUpdatedState(onNodeCreated)
    val currentOnNodeUpdated by rememberUpdatedState(onNodeUpdated)
    val currentOnNodeSelected by rememberUpdatedState(onNodeSelected)

    Canvas(
        modifier = modifier
            .pointerInput(selectedTool) {
                when (selectedTool) {
                    EditorTool.RECTANGLE, EditorTool.ELLIPSE -> {
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
                                    )

                                    EditorTool.ELLIPSE -> EditorEllipse(
                                        id = id,
                                        x = rect.left,
                                        y = rect.top,
                                        width = rect.width,
                                        height = rect.height,
                                    )

                                    else -> error("Unsupported creation tool")
                                }
                            },
                            onPreview = { previewNode = it },
                            onCommit = { currentOnNodeCreated(it) },
                        )
                    }

                    EditorTool.SELECT -> {
                        detectNodeSelection(
                            nodes = { currentNodes },
                            screenToWorld = {
                                currentCamera.screenToWorld(it)
                            },
                            onSelect = { currentOnNodeSelected(it) },
                        )
                    }

                    EditorTool.PAN -> {
                        detectCanvasPan(
                            camera = { currentCamera },
                            onCameraChanged = { currentOnCameraChanged(it) },
                        )
                    }

                    else -> Unit
                }
            }
            .pointerInput(selectedTool) {
                if (selectedTool == EditorTool.SELECT) {
                    detectNodeMovement(
                        nodes = { currentNodes },
                        screenToWorld = {
                            currentCamera.screenToWorld(it)
                        },
                        screenDeltaToWorld = {
                            it / currentCamera.zoom
                        },
                        onSelect = {
                            currentOnNodeSelected(it.id)
                        },
                        onPreview = { previewNode = it },
                        onCommit = { currentOnNodeUpdated(it) },
                    )
                }
            }.pointerInput(Unit) {
                detectCanvasZoom(
                    camera = { currentCamera },
                    onCameraChanged = { currentOnCameraChanged(it) },
                )
            }
    ) {
        scale(
            scale = camera.zoom,
            pivot = Offset.Zero,
        ) {
            translate(
                left = -camera.position.x,
                top = -camera.position.y,
            ) {
                for (node in nodes) {
                    if (node.id != previewNode?.id) {
                        drawNode(node, nodeColor)
                    }
                }

                previewNode?.let {
                    drawNode(
                        node = it,
                        color = nodeColor.copy(alpha = 0.6f),
                    )
                }

                val selectedNode = previewNode
                    ?.takeIf { it.id == selectedNodeId }
                    ?: nodes.firstOrNull { it.id == selectedNodeId }

                selectedNode?.let {
                    drawSelection(
                        node = it,
                        color = selectionColor,
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
        mutableStateOf<List<EditorNode>>(emptyList())
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
        onCameraChanged = { camera = it },
        onNodeCreated = { nodes += it },
        onNodeUpdated = { updated ->
            nodes = nodes.map { node ->
                if (node.id == updated.id) updated else node
            }
        },
        selectedNodeId = selectedNodeId,
        onNodeSelected = { selectedNodeId = it },
    )
}