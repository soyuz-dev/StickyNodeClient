package org.stickynodes.editor.draw.geometry

import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

fun List<EditorNode>.bringToFront(
    selectedIds: Set<Uuid>
): List<EditorNode> {
    val selected = filter { it.id in selectedIds }
    val others = filterNot { it.id in selectedIds }

    return others + selected
}

fun List<EditorNode>.sendToBack(
    selectedIds: Set<Uuid>,
): List<EditorNode> {
    val selected = filter { it.id in selectedIds }
    val others = filterNot { it.id in selectedIds }

    return selected + others
}

fun List<EditorNode>.bringForward(
    selectedIds: Set<Uuid>,
): List<EditorNode> {
    val result = toMutableList()

    for (index in result.lastIndex - 1 downTo 0) {
        if (
            result[index].id in selectedIds &&
            result[index + 1].id !in selectedIds
        ) {
            val node = result[index]
            result[index] = result[index + 1]
            result[index + 1] = node
        }
    }

    return result
}

fun List<EditorNode>.sendBackward(
    selectedIds: Set<Uuid>,
): List<EditorNode> {
    val result = toMutableList()

    for (index in 1..result.lastIndex) {
        if (
            result[index].id in selectedIds &&
            result[index - 1].id !in selectedIds
        ) {
            val node = result[index]
            result[index] = result[index - 1]
            result[index - 1] = node
        }
    }

    return result
}