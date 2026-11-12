package org.stickynodes.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun HomeScreen(
    recentFiles: List<File>,
    onNewDocument: () -> Unit,
    onOpenDocument: () -> Unit,
    onOpenRecent: (File) -> Unit,
    onRemoveRecent: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "StickyNodes",
            style = MaterialTheme.typography.headlineLarge,
        )

        Text(
            text = "Create something new, or continue where you left off.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onNewDocument) {
                Text("New Document")
            }

            Button(onClick = onOpenDocument) {
                Text("Open Document")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Recent Documents",
            style = MaterialTheme.typography.titleLarge,
        )

        if (recentFiles.isEmpty()) {
            Text(
                text = "No recent documents yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                recentFiles.forEach { file ->
                    RecentDocumentRow(
                        file = file,
                        onOpen = { onOpenRecent(file) },
                        onRemove = { onRemoveRecent(file) },
                    )

                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RecentDocumentRow(
    file: File,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val exists = file.isFile

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (exists) Modifier.clickable(onClick = onOpen)
                    else Modifier
                )
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = file.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (!exists) {
                Text(
                    text = "File not found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        TextButton(onClick = onRemove) {
            Text("Remove")
        }
    }
}