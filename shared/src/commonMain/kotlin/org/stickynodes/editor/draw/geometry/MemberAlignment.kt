package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode

fun List<EditorGroupMember>.alignedMemberNodes(
    alignment: NodeAlignment,
    selectionBounds: (EditorNode) -> Rect
): List<EditorNode> {
    if (size < 2) return listOf()

    val entries = map { member ->
        val nodes = member.flattenedNodes()

        val bounds = nodes
            .map(selectionBounds)
            .enclosingBounds()
            ?: return listOf()

        member to bounds
    }

    val allBounds = entries.map { it.second }

    val left = allBounds.minOf { it.left }
    val right = allBounds.maxOf { it.right }
    val top = allBounds.minOf { it.top }
    val bottom = allBounds.maxOf { it.bottom }

    val centerX = (left + right) / 2f
    val centerY = (top + bottom) / 2f

    val updated = mutableListOf<EditorNode>()

    for ((member, bounds) in entries) {
        val delta  = when (alignment) {
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

        for (node in member.flattenedNodes()) {
            updated += if (delta == Offset.Zero) {
                node
            } else {
                node.translatedBy(delta)
            }
        }
    }

    return updated
}