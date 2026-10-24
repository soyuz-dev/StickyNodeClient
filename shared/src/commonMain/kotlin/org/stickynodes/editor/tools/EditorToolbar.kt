package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.tools.components.ToolButton

@Composable
fun EditorToolbar(
    selectedTool: EditorTool,
    modifier: Modifier = Modifier,
    orientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
    onToolSelected: (EditorTool) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        ToolbarLayout(
            orientation = orientation,
            modifier = Modifier.padding(4.dp),
        ) {
            for (tool in EditorTool.entries) {
                ToolButton(
                    tool,
                    selectedTool == tool,
                ) { onToolSelected(tool) }

                if (tool == EditorTool.PAN) {
                    if(orientation == ToolbarOrientation.VERTICAL) {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .width(40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    } else {
                        VerticalDivider(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun EditorToolbarPreview() {
    var selectedTool by remember {
        mutableStateOf(EditorTool.SELECT)
    }
    EditorToolbar(
        selectedTool,
        Modifier,
    ) {selectedTool = it}

}

@Preview(showBackground = true)
@Composable
fun HorizontalToolbarPreview() {
    var selectedTool by remember {
        mutableStateOf(EditorTool.SELECT)
    }

    EditorToolbar(
        selectedTool = selectedTool,
        orientation = ToolbarOrientation.HORIZONTAL,
        onToolSelected = { selectedTool = it },
    )
}