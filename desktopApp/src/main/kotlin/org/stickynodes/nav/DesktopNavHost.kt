package org.stickynodes.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.stickynodes.document.DesktopDocumentFiles
import org.stickynodes.document.DocumentSession
import org.stickynodes.document.RecentDocuments
import org.stickynodes.editor.EditorScreen
import org.stickynodes.home.HomeScreen
import org.stickynodes.splash.SplashScreen

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Screen.Splash::class, Screen.Splash.serializer())
            subclass(Screen.Home::class, Screen.Home.serializer())
            subclass(Screen.Editor::class, Screen.Editor.serializer())
        }
    }
}
@Composable
fun DesktopNavHost(
    closeRequested: Boolean,
    onCloseCancelled: () -> Unit,
    onExit: () -> Unit,
) {
    val backStack = rememberNavBackStack(navConfig, Screen.Splash)

    val documentSession = remember { DocumentSession() }
    val recentDocuments = remember { RecentDocuments() }
    val documentFiles = remember { DesktopDocumentFiles(recentDocuments) }

    val scope = rememberCoroutineScope()

    var isClosingWithSave by remember { mutableStateOf(false) }
    var closeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(closeRequested, documentSession.hasUnsavedChanges) {
        if (closeRequested && !documentSession.hasUnsavedChanges)
            onExit()
    }

    NavDisplay(
        backStack = backStack,
        onBack = {}, //disabled for now
        entryProvider = entryProvider {
            entry<Screen.Splash> {
                SplashScreen(
                    onContinue = {
                        backStack.removeLastOrNull()
                        backStack.add(Screen.Home)
                    }
                )
            }


            entry<Screen.Home> {
                var openError by remember {
                    mutableStateOf<String?>(null)
                }

                fun openDocument(action: suspend () -> Boolean) {
                    scope.launch {
                        try {
                            if (action()) {
                                backStack.add(Screen.Editor)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            openError = e.message ?: "Unable to open document."
                        }
                    }
                }

                HomeScreen(
                    recentFiles = recentDocuments.files,
                    onNewDocument = {
                        documentSession.newDocument()
                        backStack.add(Screen.Editor)
                    },
                    onOpenDocument = {
                        openDocument {
                            documentFiles.open(documentSession)
                        }
                    },
                    onOpenRecent = { file ->
                        openDocument {
                            documentFiles.open(documentSession, file)
                        }
                    },
                    onRemoveRecent = { file ->
                        try {
                            recentDocuments.remove(file)
                        } catch (e: Exception) {
                            openError =
                                e.message ?: "Unable to remove recent document."
                        }
                    },
                )

                openError?.let { message ->
                    AlertDialog(
                        onDismissRequest = { openError = null },
                        title = { Text("Document Error") },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(onClick = { openError = null }) {
                                Text("OK")
                            }
                        },
                    )
                }
            }

            entry<Screen.Editor> {
                var showLeaveDialog by remember { mutableStateOf(false) }
                var isSaving by remember { mutableStateOf(false) }
                var saveError by remember { mutableStateOf<String?>(null) }

                fun goHome() = backStack.removeLastOrNull()

                fun requestHome() = if (documentSession.hasUnsavedChanges) {
                    saveError = null
                    showLeaveDialog = true
                } else goHome()

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = ::requestHome) {
                            Text("Home")
                        }

                        Text(
                            text = documentSession.displayName +
                                    if (documentSession.hasUnsavedChanges) " *"
                                    else "",
                        )


                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        documentFiles.save(
                                            session = documentSession,
                                            saveAs = true,
                                        )
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        println("Failed to save document as: ${e.message}")
                                    }
                                }
                            },
                        ) {
                            Text("Save As...")
                        }
                    }

                    EditorScreen(
                        modifier = Modifier.weight(1f),
                        documentState = documentSession.documentState,
                        onSave = {
                            scope.launch {
                                try {
                                    documentFiles.save(documentSession)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    println("Failed to save: ${e.message}")
                                }
                            }
                        },
                        onOpen = {
                            scope.launch {
                                try {
                                    documentFiles.open(documentSession)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    println("Failed to open: ${e.message}")
                                }
                            }
                        },
                    )
                }

                if (showLeaveDialog) AlertDialog(
                    onDismissRequest = {
                        if (!isSaving) showLeaveDialog = false
                    },
                    title = {
                        Text("Unsaved Changes")
                    },
                    text = {
                        Column {
                            Text(
                                "Save changes to ${documentSession.displayName} before returning Home?"
                            )

                            saveError?.let {
                                Text(it)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !isSaving,
                            onClick = {
                                isSaving = true
                                saveError = null

                                scope.launch {
                                    try {
                                        val saved = documentFiles.save(
                                            documentSession
                                        )

                                        if (saved) {
                                            if (!documentSession.hasUnsavedChanges) {
                                                showLeaveDialog = false
                                                goHome()
                                            } else {
                                                saveError =
                                                    "The document changed during saving. Save again to include the latest changes."
                                            }
                                        }
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        saveError =
                                            e.message ?: "Save failed."
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        ) {
                            Text(if (isSaving) "Saving..." else "Save")
                        }
                    },
                    dismissButton =  {
                        Row {
                            TextButton(
                                enabled = !isSaving,
                                onClick = {
                                    documentSession.newDocument()
                                    showLeaveDialog = false
                                    goHome()
                                },
                            ) {
                                Text("Discard")
                            }

                            TextButton(
                                enabled = !isSaving,
                                onClick = {
                                    showLeaveDialog = false
                                },
                            ) {
                                Text("Cancel")
                            }
                        }
                    },
                )
            }
        }
    )
    if (closeRequested && documentSession.hasUnsavedChanges) {
        AlertDialog(
            onDismissRequest = {
                if (!isClosingWithSave) {
                    closeError = null
                    onCloseCancelled()
                }
            },
            title = { Text("Unsaved Changes") },
            text = {
                Column {
                    Text(
                        "Save changes to ${documentSession.displayName} " +
                                "before closing StickyNodes?"
                    )

                    closeError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isClosingWithSave,
                    onClick = {
                        isClosingWithSave = true
                        closeError = null

                        scope.launch {
                            try {
                                val saved = documentFiles.save(
                                    documentSession
                                )

                                if (saved) {
                                    if (!documentSession.hasUnsavedChanges) {
                                        onExit()
                                    } else {
                                        closeError =
                                            "The document changed during saving. " +
                                                    "Please save again."
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                closeError =
                                    e.message ?: "Unable to save document."
                            } finally {
                                isClosingWithSave = false
                            }
                        }
                    },
                ) {
                    Text(
                        if (isClosingWithSave) "Saving..."
                        else "Save"
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        enabled = !isClosingWithSave,
                        onClick = onExit,
                    ) {
                        Text("Discard")
                    }

                    TextButton(
                        enabled = !isClosingWithSave,
                        onClick = {
                            closeError = null
                            onCloseCancelled()
                        },
                    ) {
                        Text("Cancel")
                    }
                }
            },
        )
    }
}