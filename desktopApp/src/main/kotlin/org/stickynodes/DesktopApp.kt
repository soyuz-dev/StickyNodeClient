package org.stickynodes

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import org.stickynodes.nav.DesktopNavHost

@Composable
fun DesktopApp(
    closeRequested: Boolean,
    onCloseCancelled: () -> Unit,
    onExit: () -> Unit,
) {
    MaterialTheme {
        DesktopNavHost(
            closeRequested = closeRequested,
            onCloseCancelled = onCloseCancelled,
            onExit = onExit,
        )
    }
}