package org.stickynodes.editor.draw.geometry.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.geometry.transform.translatedBy
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

enum class NodeAlignment {
    LEFT,
    HORIZONTAL_CENTER,
    RIGHT,
    TOP,
    VERTICAL_CENTER,
    BOTTOM
}

fun List<EditorNode>.alignNodes(
    selectedIds: Set<Uuid>,
    alignment: NodeAlignment,
    selectionBounds: (EditorNode) -> Rect
): List<EditorNode> {
    val selected = filter { it.id in selectedIds }

    if (selected.size < 2) return this

    val boundsById = selected.associate { node ->
        node.id to selectionBounds(node)
    }

    val allBounds = boundsById.values

    val left = allBounds.minOf { it.left }
    val right = allBounds.maxOf { it.right }
    val top = allBounds.minOf { it.top }
    val bottom = allBounds.maxOf { it.bottom }

    val centerX = (left + right)/2f
    val centerY = (top + bottom)/2f

    return map { node ->
        val bounds = boundsById[node.id]?: return@map node

        val delta = when (alignment) {
            NodeAlignment.LEFT ->
                Offset(left - bounds.left, 0f)

            NodeAlignment.HORIZONTAL_CENTER ->
                Offset(centerX - bounds.center.x, 0f)

            NodeAlignment.RIGHT ->
                Offset(right - bounds.right, 0f)

            NodeAlignment.TOP ->
                Offset(0f, top - bounds.top)

            NodeAlignment.VERTICAL_CENTER ->
                Offset(0f, centerY - bounds.center.y)

            NodeAlignment.BOTTOM ->
                Offset(0f, bottom - bounds.bottom)
        }

        if (delta == Offset.Zero) {
            node
        } else {
            node.translatedBy(delta)
        }
    }
}