package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorText

fun EditorNode.translatedBy(delta: Offset): EditorNode =
    when (this) {
        is EditorRectangle -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )

        is EditorEllipse -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )

        is EditorPath -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )

        is EditorText -> copy(
            x = x + delta.x,
            y = y + delta.y,
        )
    }