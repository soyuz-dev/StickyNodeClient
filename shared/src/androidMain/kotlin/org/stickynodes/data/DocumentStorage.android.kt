package org.stickynodes.data


import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.stickynodes.editor.history.EditorDocument
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


private class AndroidDocumentStorage(
    private val context: Context,
): DocumentStorage {
    var openPicker: (() -> Unit)? = null
    var savePicker: (() -> Unit)? = null

    private var pending: CancellableContinuation<Uri?>? = null

    private suspend fun pick(
        launch: () -> Unit,
    ): Uri? = suspendCancellableCoroutine { continuation ->
        check(pending == null) {"Picker already active!"}

        pending = continuation
        continuation.invokeOnCancellation {
            if(pending === continuation) pending = null
        }

        try {
            launch()
        } catch (e: Throwable) {
            pending = null
            continuation.resumeWithException(e)
        }
    }

    fun complete(uri: Uri?) {
        val continuation = pending ?: return
        pending = null

        if (continuation.isActive) {
            continuation.resume(uri)
        }
    }

    override suspend fun open(): EditorDocument? {
        val uri = pick(requireNotNull(openPicker)) ?: return null

        return withContext(Dispatchers.IO) {
            val content = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Unable to read document")

            DocumentCodec.decode(content)
        }
    }

    override suspend fun save(document: EditorDocument): Boolean {
        val content = DocumentCodec.encode(document)
        val uri = pick(requireNotNull(savePicker)) ?: return false

        withContext(Dispatchers.IO) {
            context.contentResolver
                .openOutputStream(uri, "wt")
                ?.bufferedWriter()
                ?.use { it.write(content) }
                ?: error("Unable to write document")
        }

        return true
    }
}

@Composable
actual fun rememberDocumentStorage(): DocumentStorage {
    val context = LocalContext.current

    val storage = remember(context) {
        AndroidDocumentStorage(context)
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        storage.complete(uri)
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        storage.complete(uri)
    }

    storage.openPicker = {
        openLauncher.launch(arrayOf("*/*"))
    }

    storage.savePicker = {
        saveLauncher.launch("Untitled.stn")
    }

    return storage
}