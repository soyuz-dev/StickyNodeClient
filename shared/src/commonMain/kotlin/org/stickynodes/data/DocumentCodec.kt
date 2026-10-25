package org.stickynodes.data

import kotlinx.serialization.json.Json
import org.stickynodes.data.mapper.toDocument
import org.stickynodes.data.mapper.toDto
import org.stickynodes.data.model.DocumentDto
import org.stickynodes.editor.history.EditorDocument

object DocumentCodec {
    private val json = Json {
        prettyPrint = true
        classDiscriminator = "type"
    }

    fun encode(document: EditorDocument): String =
        json.encodeToString(document.toDto())

    fun decode(content: String): EditorDocument =
        json.decodeFromString<DocumentDto>(content).toDocument()
}