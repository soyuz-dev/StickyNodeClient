package org.stickynodes.editor.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun EditorCanvas(
    rectangles: List<EditorRectangle>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier = modifier) {
        for ((_, x, y, width, height) in rectangles) {
            val offset = Offset(x, y)
            val size = Size(width, height)
            drawRect(
                color = color,
                topLeft = offset,
                size = size,
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun EditorCanvasPreview() {
    EditorCanvas(
        rectangles = listOf(
            EditorRectangle(
                x = 40f,
                y = 40f,
                width = 120f,
                height = 80f,
            ),
            EditorRectangle(
                x = 180f,
                y = 130f,
                width = 140f,
                height = 100f,
            ),
        ),
        modifier = Modifier.size(400.dp),
    )
}