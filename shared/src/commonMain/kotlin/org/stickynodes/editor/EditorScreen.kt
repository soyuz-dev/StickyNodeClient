package org.stickynodes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import org.stickynodes.editor.tools.EditorTool
import org.stickynodes.editor.tools.EditorToolbar
import kotlin.uuid.Uuid

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier
) {
    var camera by remember { mutableStateOf(EditorCamera()) }
    var selectedTool by remember { mutableStateOf(EditorTool.SELECT) }
    var nodes by remember { mutableStateOf<List<EditorNode>>(emptyList()) }
    var selectedNodeId by remember { mutableStateOf<Uuid?>(null) }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        EditorCanvas(
            modifier = Modifier.fillMaxSize(),
            nodes = nodes,
            selectedTool = selectedTool,
            onNodeCreated = { nodes += it },
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

        EditorToolbar(
            selectedTool = selectedTool,
            onToolSelected = { selectedTool = it },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(6.dp),
        )
    }
}

@Composable
@Preview(widthDp = 800, heightDp = 800)
fun EditorScreenPreview() = EditorScreen()