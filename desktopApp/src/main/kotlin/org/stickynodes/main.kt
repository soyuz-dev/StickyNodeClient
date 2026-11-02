package org.stickynodes

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        onPreviewKeyEvent = { event ->
            // Handle editor-wide shortcuts here.
            false
        },
        state = WindowState(
            width = 1000.dp,
            height = 800.dp
        ),
        title = "StickyNodes",
    ) {
        App()
    }
}