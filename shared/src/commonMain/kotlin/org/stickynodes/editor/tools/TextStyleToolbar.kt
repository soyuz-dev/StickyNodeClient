
package org.stickynodes.editor.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.tools.components.ColorPicker

@Composable
fun TextStyleToolbar(
    style: NodeStyle,
    fontSize: Float,
    onStyleChanged: (NodeStyle) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
) {
    val textColor = when (val fill = style.fill) {
        FillStyle.None -> null
        is FillStyle.Solid -> Color(fill.color)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        ToolbarLayout(
            orientation = orientation,
            modifier = Modifier.padding(4.dp),
        ) {
            ColorPicker(
                color = textColor,
                description = "Text colour",
                onColorChanged = { color ->
                    if (color != null) {
                        onStyleChanged(
                            style.copy(
                                fill = FillStyle.Solid(color.toArgb())
                            )
                        )
                    }
                },
            )

            CompactFontButton(
                label = "A−",
                enabled = fontSize > 1f,
                onClick = {
                    onFontSizeChanged(
                        (fontSize - 2f).coerceAtLeast(1f)
                    )
                },
            )

            Text(
                text = "${fontSize.toInt()}",
                style = MaterialTheme.typography.labelMedium,
            )

            CompactFontButton(
                label = "A+",
                enabled = fontSize < 256f,
                onClick = {
                    onFontSizeChanged(
                        (fontSize + 2f).coerceAtMost(256f)
                    )
                },
            )
        }
    }
}

@Composable
private fun CompactFontButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.size(
            width = 40.dp,
            height = 32.dp,
        ),
    ) {
        Box(
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                },
            )
        }
    }
}
