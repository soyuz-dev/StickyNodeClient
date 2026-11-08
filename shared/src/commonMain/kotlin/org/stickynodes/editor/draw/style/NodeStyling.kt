package org.stickynodes.editor.draw.style

import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.model.EditorText

fun EditorNode.withStyle(style: NodeStyle): EditorNode =
    when (this) {
        is EditorRectangle -> copy(style = style)
        is EditorEllipse -> copy(style = style)
        is EditorPath -> copy(style = style)
        is EditorText -> copy(style = style)
    }