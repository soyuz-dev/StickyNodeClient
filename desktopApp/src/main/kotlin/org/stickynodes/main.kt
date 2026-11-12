package org.stickynodes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    var closeRequested by remember { mutableStateOf(false) }
    Window(
        onCloseRequest = {
            closeRequested = true
        },
        onPreviewKeyEvent = { event ->
            false
        },
        state = WindowState(
            width = 1000.dp,
            height = 800.dp
        ),
        title = "StickyNodes",
    ) {
        DesktopApp(
            closeRequested = closeRequested,
            onCloseCancelled = {
                closeRequested = false
            },
            onExit = ::exitApplication,
        )
    }
}