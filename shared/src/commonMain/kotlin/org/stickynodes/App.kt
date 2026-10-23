package org.stickynodes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.stickynodes.editor.EditorScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        EditorScreen()
    }
}