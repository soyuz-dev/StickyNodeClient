package org.stickynodes.editor.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vicart.compose.material.symbols.MaterialSymbol
import org.stickynodes.editor.tools.components.ActionBarButton
import org.stickynodes.editor.tools.components.ActionBarDivider
import kotlin.math.roundToInt

@Composable
fun EditorActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    zoom: Float,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomReset: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {

        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionBarButton(
                icon = "folder_open",
                tooltip = "Open (Ctrl+O)",
                onClick = onOpen,
            )

            ActionBarButton(
                icon = "save",
                tooltip = "Save (Ctrl+S)",
                onClick = onSave,
            )

            ActionBarDivider()

            ActionBarButton(
                icon = "undo",
                tooltip = "Undo (Ctrl+Z)",
                onClick = onUndo,
                enabled = canUndo,
            )

            ActionBarButton(
                icon = "redo",
                tooltip = "Redo (Ctrl+Y)",
                onClick = onRedo,
                enabled = canRedo,
            )

            ActionBarDivider()

            ActionBarButton(
                icon = "remove",
                tooltip = "Zoom out",
                onClick = onZoomOut,
            )

            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                ),
                tooltip = {
                    PlainTooltip {
                        Text("Reset zoom to 100%")
                    }
                },
                state = rememberTooltipState(),
            ) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clickable(onClick = onZoomReset)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${(zoom * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            ActionBarButton(
                icon = "add",
                tooltip = "Zoom in",
                onClick = onZoomIn,
            )
        }

    }
}


@Composable
@Preview
private fun EditorActionBarPreview() {
    var undoAvailable by remember { mutableStateOf(true) }
    var redoAvailable by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(1f) }

    MaterialTheme {
        EditorActionBar(
            canUndo = undoAvailable,
            canRedo = redoAvailable,
            zoom = zoom,
            onUndo = {
                undoAvailable = false
                redoAvailable = true
            },
            onRedo = {
                undoAvailable = true
                redoAvailable = false
            },
            onZoomOut = {
                zoom = (zoom / 1.25f).coerceIn(0.1f, 10f)
            },
            onZoomIn = {
                zoom = (zoom * 1.25f).coerceIn(0.1f, 10f)
            },
            onZoomReset = {
                zoom = 1f
            },
            onOpen = {},
            onSave = {},
        )
    }
}