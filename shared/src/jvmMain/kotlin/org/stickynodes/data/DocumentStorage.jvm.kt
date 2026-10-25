
package org.stickynodes.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.stickynodes.editor.history.EditorDocument
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class DesktopDocumentStorage : DocumentStorage {

    override suspend fun save(document: EditorDocument): Boolean {
        val file = chooseSaveFile() ?: return false
        val content = DocumentCodec.encode(document)

        withContext(Dispatchers.IO) {
            file.writeText(content)
        }

        return true
    }

    override suspend fun open(): EditorDocument? {
        val file = chooseOpenFile() ?: return null

        val content = withContext(Dispatchers.IO) {
            file.readText()
        }

        return DocumentCodec.decode(content)
    }

    private suspend fun chooseSaveFile(): File? = onSwingThread {
        val chooser = createChooser().apply {
            dialogTitle = "Save StickyNodes Document"
            selectedFile = File("Untitled.stn")
        }

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return@onSwingThread null
        }

        val selected = chooser.selectedFile

        val file = if (selected.extension.equals("stn", ignoreCase = true)) {
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

    private suspend fun chooseOpenFile(): File? = onSwingThread {
        val chooser = createChooser().apply {
            dialogTitle = "Open StickyNodes Document"
        }

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return@onSwingThread null
        }

        chooser.selectedFile
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
            if (!continuation.isActive) {
                return@invokeLater
            }

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

@Composable
actual fun rememberDocumentStorage(): DocumentStorage =
    remember { DesktopDocumentStorage() }
