package org.stickynodes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.EditorCanvas
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.withStyle
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.tools.EditorTool
import org.stickynodes.editor.tools.EditorToolbar
import org.stickynodes.editor.tools.StylingToolbar
import org.stickynodes.editor.tools.ToolbarOrientation
import kotlin.uuid.Uuid

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier
) {
    var camera by remember { mutableStateOf(EditorCamera()) }
    var selectedTool by remember { mutableStateOf(EditorTool.SELECT) }
    var nodes by remember { mutableStateOf<List<EditorNode>>(emptyList()) }
    var selectedNodeId by remember { mutableStateOf<Uuid?>(null) }
    val selectedNode = nodes.firstOrNull { it.id == selectedNodeId }
    var rectangleStyle by remember { mutableStateOf(NodeStyle()) }
    var ellipseStyle by remember { mutableStateOf(NodeStyle()) }
    val activeStyle = selectedNode?.style ?: when (selectedTool) {
        EditorTool.RECTANGLE -> rectangleStyle
        EditorTool.ELLIPSE -> ellipseStyle
        else -> null
    }

    val onStyleChanged: (NodeStyle) -> Unit = { updated ->
        if (selectedNode != null) {
            nodes = nodes.map { node ->
                if (node.id == selectedNode.id) {
                    node.withStyle(updated)
                } else {
                    node
                }
            }
        } else {
            when (selectedTool) {
                EditorTool.RECTANGLE -> rectangleStyle = updated
                EditorTool.ELLIPSE -> ellipseStyle = updated
                else -> Unit
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        EditorCanvas(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            selectedTool = selectedTool,
            onNodeCreated = { node ->
                val styledNode = when (node) {
                    is EditorRectangle -> node.copy(style = rectangleStyle)
                    is EditorEllipse -> node.copy(style = ellipseStyle)
                }
                nodes += styledNode
            },
            selectedNodeId = selectedNodeId,
            onNodeSelected = { selectedNodeId = it },
            onNodeUpdated = { updated ->
                nodes = nodes.map { node ->
                    if (node.id == updated.id) updated else node
                }
            },
            camera = camera,
            onCameraChanged = { camera = it },
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
                onToolSelected = { selectedTool = it },
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