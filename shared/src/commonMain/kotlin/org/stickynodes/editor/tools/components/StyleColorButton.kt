package org.stickynodes.editor.tools.components

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun StyleColorButton(
    color: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Choose colour",
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = description
        },
    ) {
        if (color == null) {
            NoColorSwatch()
        } else {
            ColorSwatch(color)
        }
    }
}