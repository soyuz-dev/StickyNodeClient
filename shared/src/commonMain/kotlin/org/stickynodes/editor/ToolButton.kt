package org.stickynodes.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vicart.compose.material.symbols.MaterialSymbol

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
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
fun ToolButtonPreview() {
    Row {
        ToolButton(EditorTool.RECTANGLE, true, {})
        ToolButton(EditorTool.ELLIPSE, false, {})
    }
}