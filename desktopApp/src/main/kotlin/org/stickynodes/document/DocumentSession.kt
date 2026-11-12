
package org.stickynodes.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.stickynodes.editor.history.EditorDocument
import org.stickynodes.editor.history.EditorDocumentState
import java.io.File

class DocumentSession {
    var documentState by mutableStateOf(EditorDocumentState())
        private set

    var file by mutableStateOf<File?>(null)
        private set

    private var savedDocument by mutableStateOf(EditorDocument())

    val isUntitled: Boolean
        get() = file == null

    val displayName: String
        get() = file?.name ?: "Untitled"

    val hasUnsavedChanges: Boolean
        get() = documentState.document != savedDocument

    fun newDocument() {
        documentState = EditorDocumentState()
        file = null
        savedDocument = documentState.document
    }

    fun openDocument(
        document: EditorDocument,
        file: File,
    ) {
        documentState = EditorDocumentState(document)
        this.file = file
        savedDocument = document
    }

    fun markSaved(
        file: File,
        document: EditorDocument,
    ) {
        this.file = file
        savedDocument = document
    }
}