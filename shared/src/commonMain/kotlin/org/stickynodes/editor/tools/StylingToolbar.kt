
package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.tools.components.ColorPicker
import org.stickynodes.editor.tools.components.StrokeWidthPicker

@Composable
fun StylingToolbar(
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
    style: NodeStyle,
    onStyleChanged: (NodeStyle) -> Unit,
) {
    val fillColor = when (val fill = style.fill) {
        FillStyle.None -> null
        is FillStyle.Solid -> Color(fill.color)
    }

    val strokeColor = style.stroke?.let { Color(it.color) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        ToolbarLayout(
            orientation = orientation,
            modifier = Modifier.padding(4.dp),
        ) {
            ColorPicker(
                color = fillColor,
                description = "Fill colour",
                onColorChanged = { color ->
                    onStyleChanged(
                        style.copy(
                            fill = if (color == null) {
                                FillStyle.None
                            } else {
                                FillStyle.Solid(color.toArgb())
                            }
                        )
                    )
                },
            )

            ColorPicker(
                color = strokeColor,
                description = "Stroke colour",
                onColorChanged = { color ->
                    onStyleChanged(
                        style.copy(
                            stroke = if (color == null) {
                                null
                            } else {
                                style.stroke?.copy(
                                    color = color.toArgb()
                                ) ?: StrokeStyle(
                                    color = color.toArgb()
                                )
                            }
                        )
                    )
                },
            )
            StrokeWidthPicker(
                width = style.stroke?.width ?: 2f,
                enabled = style.stroke != null,
                onWidthChanged = { width ->
                    style.stroke?.let { stroke ->
                        onStyleChanged(
                            style.copy(stroke = stroke.copy(width = width))
                        )
                    }
                }
            )
        }
    }
}


@Preview
@Composable
fun StylingToolbarPreview() {
    var style by remember { mutableStateOf(NodeStyle()) }

    MaterialTheme {
        StylingToolbar(
            style = style,
            onStyleChanged = { style = it },
        )
    }
}