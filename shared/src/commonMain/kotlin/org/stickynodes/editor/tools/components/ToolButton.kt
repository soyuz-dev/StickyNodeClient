package org.stickynodes.editor.tools.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vicart.compose.material.symbols.MaterialSymbol
import org.stickynodes.editor.tools.EditorTool

@Composable
fun ToolButton(
    tool: EditorTool,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
        ),
        tooltip = {
            PlainTooltip {
                Text("${tool.label} (${tool.shortcut})")
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick,
            Modifier,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
            ),
        ) {
            MaterialSymbol.Outlined(
                icon = tool.iconName,
                size = 24.dp,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Preview
@Composable
fun ToolButtonPreview() {
    ToolButton(EditorTool.ELLIPSE, false){}
}