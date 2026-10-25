
package org.stickynodes.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.stickynodes.editor.history.EditorDocument
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private class IOSDocumentStorage : DocumentStorage {

    private var delegate: PickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun open(): EditorDocument? =
        suspendCancellableCoroutine { continuation ->
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeJSON)
            )

            val handler = PickerDelegate(
                onPick = { url ->
                    if (url == null) {
                        continuation.resume(null)
                    } else {
                        try {
                            val accessed =
                                url.startAccessingSecurityScopedResource()

                            try {
                                val content = NSString.stringWithContentsOfURL(
                                    url,
                                    NSUTF8StringEncoding,
                                    null,
                                ) ?: error("Unable to read document")

                                continuation.resume(
                                    DocumentCodec.decode(content)
                                )
                            } finally {
                                if (accessed) {
                                    url.stopAccessingSecurityScopedResource()
                                }
                            }
                        } catch (e: Throwable) {
                            continuation.resumeWithException(e)
                        }
                    }

                    delegate = null
                }
            )

            delegate = handler
            picker.delegate = handler

            presentPicker(picker)
        }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun save(
        document: EditorDocument,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val path = NSTemporaryDirectory() + "Untitled.stn"

            val content = DocumentCodec.encode(document)

            val success = NSString.create(content).writeToFile(
                path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )

            check(success) { "Failed to write temporary document" }

            val url = NSURL.fileURLWithPath(path)

            val picker = UIDocumentPickerViewController(
                forExportingURLs = listOf(url),
                asCopy = true,
            )

            val handler = PickerDelegate(
                onPick = { destination ->
                    continuation.resume(destination != null)
                    delegate = null
                }
            )

            delegate = handler
            picker.delegate = handler

            presentPicker(picker)
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
        }
    }
}

private class PickerDelegate(
    private val onPick: (NSURL?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onPick(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(
        controller: UIDocumentPickerViewController,
    ) {
        onPick(null)
    }
}

private fun presentPicker(
    picker: UIDocumentPickerViewController,
) {
    val scene = UIApplication.sharedApplication
        .connectedScenes
        .firstOrNull() as? UIWindowScene

    val window = scene?.windows
        ?.firstOrNull() as? UIWindow

    val root = window?.rootViewController
        ?: error("No active view controller")

    var presenter = root

    while (presenter.presentedViewController != null) {
        presenter = presenter.presentedViewController!!
    }

    presenter.presentViewController(
        picker,
        animated = true,
        completion = null,
    )
}

@Composable
actual fun rememberDocumentStorage(): DocumentStorage =
    remember { IOSDocumentStorage() }
