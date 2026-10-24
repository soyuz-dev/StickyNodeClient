package org.stickynodes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        onPreviewKeyEvent = { event ->
            // Handle editor-wide shortcuts here.
            false
        },
        title = "StickyNodes",
    ) {
        App()
    }
}