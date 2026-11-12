
package org.stickynodes.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Properties

class RecentDocuments(
    private val registryPath: Path = defaultRegistryPath(),
) {
    var files by mutableStateOf(load())
        private set

    fun record(file: File) {
        val path = file.absoluteFile.normalize()

        val updated = (listOf(path) + files)
            .distinct()
            .take(MAX_ENTRIES)

        persist(updated)
        files = updated
    }

    fun remove(file: File) {
        val updated = files.filterNot {
            it == file.absoluteFile.normalize()
        }

        persist(updated)
        files = updated
    }

    private fun load(): List<File> {
        if (!Files.exists(registryPath)) {
            return emptyList()
        }

        return runCatching {
            val properties = Properties()

            Files.newInputStream(registryPath).use {
                properties.load(it)
            }

            val count = properties
                .getProperty("count")
                ?.toIntOrNull()
                ?.coerceIn(0, MAX_ENTRIES)
                ?: 0

            (0 until count)
                .mapNotNull { index ->
                    properties.getProperty("file.$index")
                        ?.takeIf(String::isNotBlank)
                        ?.let { File(it).absoluteFile.normalize() }
                }
                .distinct()
        }.getOrElse {
            println("Failed to read recent documents: ${it.message}")
            emptyList()
        }
    }

    private fun persist(files: List<File>) {
        val properties = Properties().apply {
            setProperty("count", files.size.toString())

            files.forEachIndexed { index, file ->
                setProperty("file.$index", file.path)
            }
        }

        val parent = registryPath.toAbsolutePath().parent
        Files.createDirectories(parent)

        val temporary = Files.createTempFile(
            parent,
            "recent-",
            ".tmp",
        )

        try {
            Files.newOutputStream(temporary).use {
                properties.store(it, "StickyNodes recent documents")
            }

            try {
                Files.move(
                    temporary,
                    registryPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary,
                    registryPath,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    companion object {
        private const val MAX_ENTRIES = 10

        private fun defaultRegistryPath(): Path {
            val home = Path.of(System.getProperty("user.home"))
            val os = System.getProperty("os.name").lowercase()

            val directory = when {
                "win" in os -> {
                    val appData = System.getenv("APPDATA")
                    if (appData.isNullOrBlank()) {
                        home.resolve("AppData/Roaming/StickyNodes")
                    } else {
                        Path.of(appData).resolve("StickyNodes")
                    }
                }

                "mac" in os ->
                    home.resolve(
                        "Library/Application Support/StickyNodes"
                    )

                else -> {
                    val xdgData = System.getenv("XDG_DATA_HOME")

                    val base = if (xdgData.isNullOrBlank()) {
                        home.resolve(".local/share")
                    } else {
                        Path.of(xdgData)
                    }

                    base.resolve("stickynodes")
                }
            }

            return directory.resolve("recent.properties")
        }
    }
}
