package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode


fun EditorGroupMember.selectionBoundsOrNull(
    nodeBounds: (EditorNode) -> Rect,
): Rect? = flattenedNodes()
    .map(nodeBounds)
    .enclosingBounds()