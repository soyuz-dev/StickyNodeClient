
package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorText
import kotlin.uuid.Uuid

fun EditorNode.duplicatedBy(
    offset: Offset,
    newId: Uuid = Uuid.random(),
): EditorNode = when (this) {
    is EditorRectangle -> copy(
        id = newId,
        x = x + offset.x,
        y = y + offset.y,
    )

    is EditorEllipse -> copy(
        id = newId,
        x = x + offset.x,
        y = y + offset.y,
    )

    is EditorPath -> copy(
        id = newId,
        x = x + offset.x,
        y = y + offset.y,
    )

    is EditorText -> copy(
        id = newId,
        x = x + offset.x,
        y = y + offset.y,
    )
}
