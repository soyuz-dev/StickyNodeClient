package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.tools.components.StyleColorButton

@Composable
fun StylingToolbar(
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
    style: NodeStyle,
    onStyleChanged: (NodeStyle) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {

        val fillColor = when (val fill = style.fill) {
            FillStyle.None -> null
            is FillStyle.Solid -> Color(fill.color)
        }

        val strokeColor = style.stroke?.let {
            Color(it.color)
        }
        ToolbarLayout(
            orientation = orientation,
            modifier = Modifier.padding(4.dp)
        ) {

            StyleColorButton(
                color = fillColor,
                onClick = { /* Open fill picker */ },
                description = "Fill colour",
            )

            StyleColorButton(
                color = strokeColor,
                onClick = { /* Open stroke picker */ },
                description = "Stroke colour",
            )
        }
    }
}





