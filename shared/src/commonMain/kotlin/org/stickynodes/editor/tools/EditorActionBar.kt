package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vicart.compose.material.symbols.MaterialSymbol

@Composable
fun EditorActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    zoom: Float,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onZoomReset: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
        }
    }
}