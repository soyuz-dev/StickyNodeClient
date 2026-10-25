
@file:OptIn(ExperimentalWasmJsInterop::class)

package org.stickynodes.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

        function cleanup() {
            input.remove();
        }

        function finish(value) {
            if (settled) return;
            settled = true;
            cleanup();
            resolve(value);
        }

        function fail(error) {
            if (settled) return;
            settled = true;
            cleanup();
            reject(error);
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
                    fail(error);
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

        setTimeout(function () {
            URL.revokeObjectURL(url);
        }, 1000);
    }"""
)
private external fun downloadDocument(content: JsString)

@JsFun(
    """() => {
        if (window.stickyNodesOpenShortcutHandler) return;

        const handler = function(event) {
            if (
                (event.ctrlKey || event.metaKey) &&
                !event.shiftKey &&
                !event.altKey &&
                event.key.toLowerCase() === "o"
            ) {
                event.preventDefault();
            }
        };

        window.addEventListener("keydown", handler, true);
        window.stickyNodesOpenShortcutHandler = handler;
    }"""
)
private external fun installOpenShortcutHandler()

@JsFun(
    """() => {
        const handler = window.stickyNodesOpenShortcutHandler;

        if (handler) {
            window.removeEventListener("keydown", handler, true);
            delete window.stickyNodesOpenShortcutHandler;
        }
    }"""
)
private external fun removeOpenShortcutHandler()

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

    override suspend fun save(document: EditorDocument): Boolean {
        downloadDocument(
            DocumentCodec.encode(document).toJsString()
        )

        return true
    }
}

@Composable
actual fun rememberDocumentStorage(): DocumentStorage {
    DisposableEffect(Unit) {
        installOpenShortcutHandler()

        onDispose {
            removeOpenShortcutHandler()
        }
    }

    return remember {
        WebDocumentStorage()
    }
}
