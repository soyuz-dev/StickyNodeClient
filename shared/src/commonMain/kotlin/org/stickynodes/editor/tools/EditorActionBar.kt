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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpen,
                modifier = Modifier.size(32.dp),
            ) {
                MaterialSymbol.Outlined(
                    icon = "folder_open",
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }


            IconButton(
                onClick = onSave,
                modifier = Modifier.size(32.dp),
            ) {
                MaterialSymbol.Outlined(
                    icon = "save",
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(32.dp),
            ) {
                MaterialSymbol.Outlined(
                    icon = "undo",
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (canUndo) 1f else 0.38f
                    ),
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(32.dp),
            ) {
                MaterialSymbol.Outlined(
                    icon = "redo",
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (canRedo) 1f else 0.38f
                    ),
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )


            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(32.dp),
            ) {
                MaterialSymbol.Outlined(
                    icon = "remove",
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

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

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(32.dp),
            ) {
                MaterialSymbol.Outlined(
                    icon = "add",
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

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