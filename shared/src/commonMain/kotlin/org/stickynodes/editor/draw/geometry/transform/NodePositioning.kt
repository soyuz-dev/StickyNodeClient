package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.translatedBy
import org.stickynodes.editor.draw.model.EditorNode

fun EditorNode.positionedAt(
    position: Offset,
): EditorNode {
    require(position.x.isFinite() && position.y.isFinite()) {
        "Node position must be finite"
    }

    val currentPosition = Offset(x, y)
    val delta = position - currentPosition

    return if (delta == Offset.Zero) this else translatedBy(delta)
}