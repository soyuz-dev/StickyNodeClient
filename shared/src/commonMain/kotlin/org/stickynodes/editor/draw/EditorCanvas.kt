package org.stickynodes.editor.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.EditorTool
import kotlin.math.max
import kotlin.math.min

@Composable
fun EditorCanvas(
    modifier: Modifier = Modifier,
    nodes: List<EditorNode>,
    selectedTool: EditorTool,
    onNodeCreated: (EditorNode) -> Unit,
) {
    var drag by remember { mutableStateOf<RectangleDrag?>(null) }

    val color = MaterialTheme.colorScheme.secondaryContainer
    val currentOnNodeCreated by rememberUpdatedState(onNodeCreated)

    Canvas(
        modifier = modifier.pointerInput(selectedTool) {
            if (selectedTool == EditorTool.RECTANGLE) {
                detectDragGestures(
                    onDragStart = { offset ->
                        drag = RectangleDrag(
                            start = offset,
                            end = offset,
                        )
                    },
                    onDrag = { change, dragAmount ->
                        drag = drag?.let {
                            it.copy(end = it.end + dragAmount)
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        drag?.let {
                            val rect = rectangleBetween(it.start, it.end)

                            if (rect.width > 0f && rect.height > 0f) {
                                currentOnNodeCreated(
                                    EditorRectangle(
                                        x = rect.left,
                                        y = rect.top,
                                        width = rect.width,
                                        height = rect.height,
                                    )
                                )
                            }
                        }

                        drag = null
                    },
                    onDragCancel = {
                        drag = null
                    },
                )
            }
        }
    ) {
        for (node in nodes) {
            drawNode(node, color)
        }

        drag?.let {
            val rect = rectangleBetween(it.start, it.end)

            drawRect(
                color = Color.Gray,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 2f),
            )
        }
    }
}

private data class RectangleDrag(
    val start: Offset,
    val end: Offset,
)

private fun rectangleBetween(start: Offset, end: Offset): Rect =
    Rect(
        left = min(start.x, end.x),
        top = min(start.y, end.y),
        right = max(start.x, end.x),
        bottom = max(start.y, end.y),
    )

private fun DrawScope.drawNode(
    node: EditorNode,
    color: Color,
) {
    when (node) {
        is EditorRectangle -> drawRect(
            color = color,
            topLeft = Offset(node.x, node.y),
            size = Size(node.width, node.height),
        )
    }
}

@Composable
@Preview(showBackground = true)
fun EditorCanvasPreview() {
    var nodes by remember {
        mutableStateOf<List<EditorNode>>(emptyList())
    }

    EditorCanvas(
        nodes = nodes,
        modifier = Modifier.size(400.dp),
        selectedTool = EditorTool.RECTANGLE,
        onNodeCreated = { nodes += it },
    )
}