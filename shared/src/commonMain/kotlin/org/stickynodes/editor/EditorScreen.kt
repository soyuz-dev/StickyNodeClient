
package org.stickynodes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.EditorCanvas
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.withStyle
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.history.EditorHistory
import org.stickynodes.editor.tools.EditorTool
import org.stickynodes.editor.tools.EditorToolbar
import org.stickynodes.editor.tools.StylingToolbar
import org.stickynodes.editor.tools.ToolbarOrientation
import kotlin.uuid.Uuid

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
) {
    var camera by remember {
        mutableStateOf(EditorCamera())
    }

    var selectedTool by remember {
        mutableStateOf(EditorTool.SELECT)
    }

    var history by remember {
        mutableStateOf(EditorHistory())
    }

    val nodes = history.document.nodes

    var selectedNodeId by remember {
        mutableStateOf<Uuid?>(null)
    }

    val selectedNode = nodes.firstOrNull {
        it.id == selectedNodeId
    }

    var rectangleStyle by remember {
        mutableStateOf(NodeStyle())
    }

    var ellipseStyle by remember {
        mutableStateOf(NodeStyle())
    }

    var penStyle by remember {
        mutableStateOf(
            NodeStyle(
                fill = FillStyle.None,
                stroke = StrokeStyle(
                    color = 0xFF264B72.toInt(),
                    width = 3f,
                ),
            )
        )
    }

    val focusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun updateSelection() {
        selectedNodeId = selectedNodeId?.takeIf { id ->
            history.document.nodes.any { it.id == id }
        }
    }

    fun undo() {
        history = history.undo()
        updateSelection()
    }

    fun redo() {
        history = history.redo()
        updateSelection()
    }

    fun commitNodes(updatedNodes: List<EditorNode>) {
        history = history.commit(
            history.document.copy(
                nodes = updatedNodes,
            )
        )
    }

    val activeStyle = selectedNode?.style ?: when (selectedTool) {
        EditorTool.RECTANGLE -> rectangleStyle
        EditorTool.ELLIPSE -> ellipseStyle
        EditorTool.PEN -> penStyle
        else -> null
    }

    val onStyleChanged: (NodeStyle) -> Unit = { updated ->
        if (selectedNode != null) {
            commitNodes(
                nodes.map { node ->
                    if (node.id == selectedNode.id) {
                        node.withStyle(updated)
                    } else {
                        node
                    }
                }
            )
        } else {
            when (selectedTool) {
                EditorTool.RECTANGLE -> rectangleStyle = updated
                EditorTool.ELLIPSE -> ellipseStyle = updated
                EditorTool.PEN -> penStyle = updated
                else -> Unit
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (
                    event.type !=
                    androidx.compose.ui.input.key.KeyEventType.KeyDown ||
                    !event.isCtrlPressed
                ) {
                    false
                } else {
                    when {
                        event.key == Key.Z && !event.isShiftPressed -> {
                            if (history.canUndo) {
                                undo()
                                true
                            } else {
                                false
                            }
                        }

                        event.key == Key.Y ||
                                (event.key == Key.Z && event.isShiftPressed) -> {
                            if (history.canRedo) {
                                redo()
                                true
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
                }
            }
            .focusable(),
    ) {
        EditorCanvas(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            selectedTool = selectedTool,
            camera = camera,
            rectangleStyle = rectangleStyle,
            ellipseStyle = ellipseStyle,
            penStyle = penStyle,
            onCameraChanged = {
                camera = it
            },
            onNodeCreated = { node ->
                commitNodes(nodes + node)
            },
            onNodeUpdated = { updated ->
                commitNodes(
                    nodes.map { node ->
                        if (node.id == updated.id) {
                            updated
                        } else {
                            node
                        }
                    }
                )
            },
            selectedNodeId = selectedNodeId,
            onNodeSelected = {
                selectedNodeId = it
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorToolbar(
                selectedTool = selectedTool,
                onToolSelected = { tool ->
                    selectedTool = tool

                    if (tool != EditorTool.SELECT) {
                        selectedNodeId = null
                    }

                    focusRequester.requestFocus()
                },
                orientation = ToolbarOrientation.VERTICAL,
            )

            if (activeStyle != null) {
                StylingToolbar(
                    style = activeStyle,
                    onStyleChanged = onStyleChanged,
                    orientation = ToolbarOrientation.VERTICAL,
                )
            }
        }
    }
}

@Composable
@Preview(widthDp = 800, heightDp = 800)
fun EditorScreenPreview() = EditorScreen()
