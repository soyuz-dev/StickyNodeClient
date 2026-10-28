package org.stickynodes.editor.tools

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.tools.components.ColorPicker
import org.stickynodes.editor.tools.components.StrokeWidthPicker

@Composable
fun FreehandStyleToolbar(
    style: NodeStyle,
    onStyleChanged: (NodeStyle) -> Unit,
    onStylePreview: (NodeStyle) -> Unit,
    onStyleChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL
) {
    val stroke = style.stroke ?: StrokeStyle(
        color = 0xFF264B72.toInt(),
        width = 3f
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        ToolbarLayout(
            orientation = orientation,
            modifier = Modifier
        ) {
            ColorPicker(
                color = Color(stroke.color),
                description = "Pen colour",
                onColorChanged = { color ->
                    if (color != null) {
                        onStyleChanged(
                            style.copy(
                                stroke = stroke.copy(
                                    color = color.toArgb()
                                )
                            )
                        )
                    }
                }
            )

            StrokeWidthPicker(
                width = stroke.width,
                onWidthChanged = { width ->
                    onStylePreview(
                        style.copy(
                            stroke = stroke.copy(width = width)
                        )
                    )
                },
                onWidthChangeFinished = onStyleChangeFinished,
            )
        }
    }
}