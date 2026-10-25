
@file:OptIn(ExperimentalWasmJsInterop::class)

package org.stickynodes.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import org.stickynodes.editor.history.EditorDocument
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString


@JsFun(
    """() => new Promise((resolve, reject) => {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = ".stn,application/json";
        input.style.display = "none";

        let settled = false;

        function finish(value) {
            if (settled) return;
            settled = true;
            input.remove();
            resolve(value);
        }

        input.addEventListener("change", function () {
            const file = input.files && input.files.length > 0
                ? input.files[0]
                : null;

            if (file === null) {
                finish(null);
                return;
            }

            file.text().then(
                function (content) {
                    finish(content);
                },
                function (error) {
                    input.remove();
                    reject(error);
                }
            );
        }, { once: true });

        input.addEventListener("cancel", function () {
            finish(null);
        }, { once: true });

        document.body.appendChild(input);
        input.click();
    })"""
)
private external fun pickDocument(): Promise<JsString?>

@JsFun(
    """(content) => {
        const blob = new Blob(
            [content],
            { type: "application/json" }
        );

        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = url;
        link.download = "Untitled.stn";
        document.body.appendChild(link);
        link.click();
        link.remove();

        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }"""
)
private external fun downloadDocument(content: JsString)

private suspend fun Promise<JsString?>.awaitValue(): JsString? =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = {
                if (continuation.isActive) {
                    continuation.resume(it)
                }
                null
            },
            onRejected = {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        it.asJsException()
                    )
                }
                null
            },
        )
    }

private class WebDocumentStorage : DocumentStorage {

    override suspend fun open(): EditorDocument? {
        val content = pickDocument().awaitValue()
            ?: return null

        return DocumentCodec.decode(content.toString())
    }

    override suspend fun save(
        document: EditorDocument,
    ): Boolean {
        downloadDocument(
            DocumentCodec.encode(document).toJsString()
        )
        return true
    }
}

@Composable
actual fun rememberDocumentStorage(): DocumentStorage =
    remember { WebDocumentStorage() }
