
package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.tools.components.ActionBarButton

@Composable
fun ArrangementBar(
    onBringToFront: () -> Unit,
    onBringForward: () -> Unit,
    onSendBackward: () -> Unit,
    onSendToBack: () -> Unit,
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
                icon = "vertical_align_top",
                tooltip = "Bring to front",
                onClick = onBringToFront,
            )

            ActionBarButton(
                icon = "arrow_upward",
                tooltip = "Bring forward",
                onClick = onBringForward,
            )

            ActionBarButton(
                icon = "arrow_downward",
                tooltip = "Send backward",
                onClick = onSendBackward,
            )

            ActionBarButton(
                icon = "vertical_align_bottom",
                tooltip = "Send to back",
                onClick = onSendToBack,
            )
        }
    }
}

@Preview
@Composable
private fun ArrangementBarPreview() {
    MaterialTheme {
        ArrangementBar(
            onBringToFront = {},
            onBringForward = {},
            onSendBackward = {},
            onSendToBack = {},
        )
    }
}
