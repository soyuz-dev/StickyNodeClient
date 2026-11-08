package org.stickynodes.editor.draw.geometry.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.geometry.transform.translatedNodesBy
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode

fun List<EditorGroupMember>.distributedMemberNodes(
    distribution: NodeDistribution,
    selectionBounds: (EditorNode) -> Rect,
): List<EditorNode> {
    if (size < 3) return listOf()

    val horizontal = distribution == NodeDistribution.HORIZONTAL

    val ordered = map { member ->
        val bounds = member.selectionBoundsOrNull(selectionBounds)
            ?: return listOf()

        member to bounds
    }.sortedBy { (_, bounds) ->
        if (horizontal) bounds.left else bounds.top
    }

    val firstBounds = ordered.first().second
    val lastBounds = ordered.last().second

    val start = if (horizontal) {
        firstBounds.left
    } else {
        firstBounds.top
    }

    val end = if (horizontal) {
        lastBounds.right
    } else {
        lastBounds.bottom
    }

    val totalSize = ordered.sumOf { (_, bounds) ->
        (if (horizontal) bounds.width else bounds.height).toDouble()
    }.toFloat()

    val gap = (end - start - totalSize) / (ordered.size - 1)

    var cursor = start
    val updated = mutableListOf<EditorNode>()

    for ((member, bounds) in ordered) {
        val currentStart = if (horizontal) {
            bounds.left
        } else {
            bounds.top
        }

        val delta = cursor - currentStart

        val translation = if (horizontal) {
            Offset(delta, 0f)
        } else {
            Offset(0f, delta)
        }

        updated.addAll(member.translatedNodesBy(translation))

        cursor += if (horizontal) {
            bounds.width + gap
        } else {
            bounds.height + gap
        }
    }

    return updated
}
