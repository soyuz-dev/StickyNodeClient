package org.stickynodes.editor.tools.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .size(24.dp)
            .background(color, shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
    )
}

@Preview
@Composable
private fun ColorSwatchPreview() {
    MaterialTheme {
        ColorSwatch(Color(0xFF52A6C4))
    }
}