package org.stickynodes.editor.draw.geometry.selection

import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.EditorShape
import kotlin.uuid.Uuid

enum class NodeSizing {
    MATCH_WIDTH,
    MATCH_HEIGHT,
    MATCH_BOTH
}

fun List<EditorNode>.matchShapeSizes(
    selectedIds: Set<Uuid>,
    sizing: NodeSizing
): List<EditorNode> {
    val selected = filterIsInstance<EditorShape>()
        .filter { it.id in selectedIds }

    if (selected.size < 2) return this

    val targetWidth = selected.maxOf { it.bounds.width }
    val targetHeight = selected.maxOf { it.bounds.height }

    val replacements = selected.associate { shape ->
        val original = shape.bounds

        val width = when (sizing) {
            NodeSizing.MATCH_WIDTH,
            NodeSizing.MATCH_BOTH -> targetWidth

            NodeSizing.MATCH_HEIGHT -> original.width
        }

        val height = when (sizing) {
            NodeSizing.MATCH_HEIGHT,
            NodeSizing.MATCH_BOTH -> targetHeight

            NodeSizing.MATCH_WIDTH -> original.height
        }

        val center = original.center

        shape.id to shape.withBounds(
            Rect(
                left = center.x - width / 2f,
                top = center.y - height / 2f,
                right = center.x + width / 2f,
                bottom = center.y + height / 2f,
            )
        )
    }

    return map { node ->
        replacements[node.id] ?: node
    }
}