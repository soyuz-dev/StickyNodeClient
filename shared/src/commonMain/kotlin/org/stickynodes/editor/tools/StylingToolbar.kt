
package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import org.stickynodes.editor.draw.style.NodeStyle

@Composable
fun StylingToolbar(
    type: StyleToolbarType,
    onTypeSelected: (StyleToolbarType) -> Unit,
    style: NodeStyle,
    fontSize: Float,
    onStyleChanged: (NodeStyle) -> Unit,
    onStylePreview: (NodeStyle) -> Unit,
    onStyleChangeFinished: () -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (option in StyleToolbarType.entries) {
                FilterChip(
                    selected = type == option,
                    onClick = { onTypeSelected(option) },
                    label = {
                        Text(
                            when (option) {
                                StyleToolbarType.SHAPE -> "Shape"
                                StyleToolbarType.TEXT -> "Text"
                                StyleToolbarType.FREEHAND -> "Pen"
                            }
                        )
                    },
                )
            }
        }

        when (type) {
            StyleToolbarType.SHAPE -> ShapeStyleToolbar(
                style = style,
                onStyleChanged = onStyleChanged,
                onStylePreview = onStylePreview,
                onStyleChangeFinished = onStyleChangeFinished,
                orientation = orientation,
            )

            StyleToolbarType.TEXT -> TextStyleToolbar(
                style = style,
                fontSize = fontSize,
                onStyleChanged = onStyleChanged,
                onFontSizeChanged = onFontSizeChanged,
                orientation = orientation,
            )

            StyleToolbarType.FREEHAND -> FreehandStyleToolbar(
                style = style,
                onStyleChanged = onStyleChanged,
                onStylePreview = onStylePreview,
                onStyleChangeFinished = onStyleChangeFinished,
                orientation = orientation,
            )
        }
    }
}
