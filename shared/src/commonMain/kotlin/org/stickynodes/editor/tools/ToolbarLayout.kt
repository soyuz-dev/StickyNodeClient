package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ToolbarLayout(
    orientation: ToolbarOrientation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (orientation) {
        ToolbarOrientation.HORIZONTAL -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {content()}
        ToolbarOrientation.VERTICAL -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {content()}
    }
}