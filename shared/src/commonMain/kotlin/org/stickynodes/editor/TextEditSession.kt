
package org.stickynodes.editor

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.EditorText
import kotlin.uuid.Uuid

sealed interface TextEditSession {
    val position: Offset
    val initialText: String
    val fontSize: Float

    data class Create(
        override val position: Offset,
        override val fontSize: Float,
    ) : TextEditSession {
        override val initialText: String = ""
    }

    data class Edit(
        val nodeId: Uuid,
        override val position: Offset,
        override val initialText: String,
        override val fontSize: Float,
    ) : TextEditSession
}

fun EditorText.toEditSession(): TextEditSession.Edit =
    TextEditSession.Edit(
        nodeId = id,
        position = Offset(x, y),
        initialText = text,
        fontSize = fontSize,
    )
