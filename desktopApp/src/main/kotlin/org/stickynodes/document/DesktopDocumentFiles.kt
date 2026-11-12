
package org.stickynodes.document

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.stickynodes.data.DocumentCodec
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DesktopDocumentFiles(
    private val recentDocuments: RecentDocuments,
) {

    suspend fun open(session: DocumentSession): Boolean {
        val file = chooseOpenFile() ?: return false
        return open(session, file)
    }

    suspend fun open(
        session: DocumentSession,
        file: File
    ): Boolean {
        if (session.hasUnsavedChanges && !confirmDiscard())
            return false

        val document = withContext(Dispatchers.IO) {
            DocumentCodec.decode(file.readText())
        }

        session.openDocument(document, file)
        runCatching {
            recentDocuments.record(file)
        }.onFailure {
            println("Failed to update recent documents: ${it.message}")
        }
        return true
    }

    suspend fun save(
        session: DocumentSession,
        saveAs: Boolean = false,
    ): Boolean {
        val document = session.documentState.document

        val file = session.file
            ?.takeUnless { saveAs }
            ?: chooseSaveFile(session.file)
            ?: return false

        val content = DocumentCodec.encode(document)

        withContext(Dispatchers.IO) {
            file.writeText(content)
        }

        session.markSaved(file, document)
        runCatching {
            recentDocuments.record(file)
        }.onFailure {
            println("Failed to update recent documents: ${it.message}")
        }
        return true
    }

    private suspend fun confirmDiscard(): Boolean = onSwingThread {
        JOptionPane.showConfirmDialog(
            null,
            "Discard unsaved changes to the current document?",
            "Unsaved Changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
        ) == JOptionPane.YES_OPTION
    }

    private suspend fun chooseOpenFile(): File? = onSwingThread {
        val chooser = createChooser().apply {
            dialogTitle = "Open StickyNodes Document"
        }

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile
        } else {
            null
        }
    }

    private suspend fun chooseSaveFile(
        currentFile: File?,
    ): File? = onSwingThread {
        val chooser = createChooser().apply {
            dialogTitle = "Save StickyNodes Document"
            selectedFile = currentFile ?: File("Untitled.stn")
        }

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return@onSwingThread null
        }

        val selected = chooser.selectedFile

        val file = if (selected.extension.equals("stn", true)) {
            selected
        } else {
            File("${selected.path}.stn")
        }

        if (file.exists()) {
            val choice = JOptionPane.showConfirmDialog(
                null,
                "Overwrite ${file.name}?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
            )

            if (choice != JOptionPane.YES_OPTION) {
                return@onSwingThread null
            }
        }

        file
    }

    private fun createChooser(): JFileChooser =
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter(
                "StickyNodes Document (*.stn)",
                "stn",
            )
        }

    private suspend fun <T> onSwingThread(
        action: () -> T,
    ): T = suspendCancellableCoroutine { continuation ->
        SwingUtilities.invokeLater {
            if (!continuation.isActive) return@invokeLater

            try {
                val result = action()
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            } catch (e: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
}