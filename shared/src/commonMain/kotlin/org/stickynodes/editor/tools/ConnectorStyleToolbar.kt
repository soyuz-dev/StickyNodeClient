package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.tools.components.ColorPicker
import org.stickynodes.editor.tools.components.StrokeWidthPicker

@Composable
fun ConnectorStyleToolbar(
    stroke: StrokeStyle,
    onStrokeChanged: (StrokeStyle) -> Unit,
    onStrokePreview: (StrokeStyle) -> Unit,
    onStrokeChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        ToolbarLayout(
            orientation = orientation,
            modifier = Modifier.padding(4.dp)
        ) {
            ColorPicker(
                color = Color(stroke.color),
                description = "Connector colour",
                onColorChanged = { color ->
                    if (color != null) {
                        onStrokeChanged(
                            stroke.copy(
                                color = color.toArgb(),
                            )
                        )
                    }
                },
            )

            StrokeWidthPicker(
                width = stroke.width,
                onWidthChanged = { width ->
                    onStrokePreview(
                        stroke.copy(width = width)
                    )
                },
                onWidthChangeFinished =
                    onStrokeChangeFinished,
            )
        }
    }
}