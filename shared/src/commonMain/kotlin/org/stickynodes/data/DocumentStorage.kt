package org.stickynodes.data

import androidx.compose.runtime.Composable
import org.stickynodes.editor.history.EditorDocument

interface DocumentStorage {
    suspend fun open(): EditorDocument?
    suspend fun save(document: EditorDocument): Boolean
}

@Composable
expect fun rememberDocumentStorage(): DocumentStorage