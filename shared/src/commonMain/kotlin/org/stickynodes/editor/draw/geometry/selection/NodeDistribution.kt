package org.stickynodes.editor.draw.geometry.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.geometry.transform.translatedBy
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

enum class NodeDistribution {
    HORIZONTAL,
    VERTICAL,
}

fun List<EditorNode>.distributeNodes(
    selectedIds: Set<Uuid>,
    distribution: NodeDistribution,
    selectionBounds: (EditorNode) -> Rect,
): List<EditorNode> {
    val selected = filter { it.id in selectedIds }

    if (selected.size < 3) return this

    val horizontal = distribution == NodeDistribution.HORIZONTAL

    val ordered = selected
        .map { it to selectionBounds(it) }
        .sortedBy { (_, bounds) ->
            if (horizontal) bounds.left else bounds.top
        }

    val first = ordered.first().second
    val last = ordered.last().second

    val start = if (horizontal) first.left else first.top
    val end = if (horizontal) last.right else last.bottom

    val totalSize = ordered.sumOf { (_, bounds) ->
        (if (horizontal) bounds.width else bounds.height).toDouble()
    }.toFloat()

    val gap = (end - start - totalSize) / (ordered.size - 1)

    var cursor = start

    val deltas = ordered.associate { (node, bounds) ->
        val currentStart =
            if (horizontal) bounds.left else bounds.top

        val delta = cursor - currentStart

        cursor +=
            (if (horizontal) bounds.width else bounds.height) + gap

        node.id to if (horizontal) {
            Offset(delta, 0f)
        } else {
            Offset(0f, delta)
        }
    }

    return map { node ->
        val delta = deltas[node.id] ?: return@map node

        if (delta == Offset.Zero) node
        else node.translatedBy(delta)
    }
}