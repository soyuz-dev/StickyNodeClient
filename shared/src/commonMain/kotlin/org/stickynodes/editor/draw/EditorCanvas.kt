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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.EditorTool
import org.stickynodes.editor.draw.gesture.detectNodeSelection
import org.stickynodes.editor.draw.gesture.detectRectangleCreation
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.render.drawNode
import org.stickynodes.editor.draw.render.drawSelection
import kotlin.uuid.Uuid

@Composable
fun EditorCanvas(
    modifier: Modifier = Modifier,
    nodes: List<EditorNode>,
    selectedTool: EditorTool,
    onNodeCreated: (EditorNode) -> Unit,
    selectedNodeId: Uuid?,
    onNodeSelected: (Uuid?) -> Unit,
) {
    var preview by remember {
        mutableStateOf<Rect?>(null)
    }

    val nodeColor = MaterialTheme.colorScheme.secondaryContainer
    val selectionColor = Color(0xFF4589E8)

    val currentNodes by rememberUpdatedState(nodes)
    val currentOnNodeCreated by rememberUpdatedState(onNodeCreated)
    val currentOnNodeSelected by rememberUpdatedState(onNodeSelected)

    Canvas(
        modifier = modifier.pointerInput(selectedTool) {
            when (selectedTool) {
                EditorTool.RECTANGLE -> detectRectangleCreation(
                    onPreview = { preview = it },
                    onCommit = { currentOnNodeCreated(it) },
                )

                EditorTool.SELECT -> detectNodeSelection(
                    nodes = { currentNodes },
                    onSelect = { currentOnNodeSelected(it) },
                )

                else -> Unit
            }
        }
    ) {
        for (node in nodes) {
            drawNode(node, nodeColor)
        }

        preview?.let {
            drawRect(
                color = Color.Gray,
                topLeft = it.topLeft,
                size = it.size,
                style = Stroke(width = 2f),
            )
        }

        nodes.firstOrNull { it.id == selectedNodeId }?.let {
            drawSelection(it, selectionColor)
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

    EditorCanvas(
        modifier = Modifier.size(400.dp),
        nodes = nodes,
        selectedTool = EditorTool.RECTANGLE,
        onNodeCreated = { nodes += it },
        selectedNodeId = selectedNodeId,
        onNodeSelected = { selectedNodeId = it },
    )
}