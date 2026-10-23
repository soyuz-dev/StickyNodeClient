package org.stickynodes.editor.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.EditorTool
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


@Composable
fun EditorCanvas(
    modifier: Modifier = Modifier,
    nodes: List<EditorNode>,
    selectedTool: EditorTool,
    onNodeCreated: (EditorNode) -> Unit,
) {

    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

    val color = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier = modifier.pointerInput(selectedTool) {
        if (selectedTool == EditorTool.RECTANGLE)
        detectDragGestures(
            onDragStart = { offset ->
                dragStart = offset
                dragEnd = offset
            },
            onDrag = { change, dragAmount ->
                dragEnd = dragEnd?.plus(dragAmount)
            },
            onDragEnd = {
                val start = dragStart
                val end = dragEnd

                if (start != null && end != null) {
                    val topLeft = Offset(
                        min(start.x, end.x),
                        min(start.y, end.y),
                    )

                    val size = Size(
                        abs(end.x - start.x),
                        abs(end.y - start.y),
                    )

                    if (size.width > 0f && size.height > 0f) {
                        onNodeCreated(
                            EditorRectangle(
                                x = topLeft.x,
                                y = topLeft.y,
                                width = size.width,
                                height = size.height,
                            )
                        )
                    }
                }

                dragStart = null
                dragEnd = null
            },
            onDragCancel = {
                dragStart = null
                dragEnd = null
            },
        )
    }) {
        for (node in nodes) {
            when(node) {
                is EditorRectangle -> {
                    val offset = Offset(node.x, node.y)
                    val size = Size(node.width, node.height)
                    drawRect(
                        color = color,
                        topLeft = offset,
                        size = size,
                    )
                }
            }
        }

        val start = dragStart
        val end = dragEnd

        if (start != null && end != null) {
            val topLeft = Offset(
                min(start.x, end.x),
                min(start.y, end.y),
            )

            val size = Size(
                abs(end.x - start.x),
                abs(end.y - start.y),
            )

            drawRect(
                color = Color.Gray,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 2f),
            )
        }
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
    ) {
        nodes += it
    }
}

private fun rectangleBetween(start: Offset, end: Offset): Rect =
    Rect(
        left = min(start.x, end.x),
        top = min(start.y, end.y),
        right = max(start.x, end.x),
        bottom = max(start.y, end.y),
    )