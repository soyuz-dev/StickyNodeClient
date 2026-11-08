package org.stickynodes.editor.history

import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

data class EditorHistory(
    val document: EditorDocument = EditorDocument(),
    val undoStack: List<EditorDocument> = listOf(),
    val redoStack: List<EditorDocument> = listOf(),
) {
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()
    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun commit(
        document: EditorDocument,
        maxHistory: Int = 100,
    ): EditorHistory {
        require(maxHistory > 0)

        if(document == this.document) return this

        return copy(
            document = document,
            undoStack = (undoStack + this.document)
                .takeLast(maxHistory),
            redoStack = listOf()
        )
    }

    fun undo(): EditorHistory {
        if (!canUndo) return this

        return copy(
            document = undoStack.last(),
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + document,
        )
    }


    fun redo(): EditorHistory {
        if (!canRedo) return this

        return copy(
            document = redoStack.last(),
            undoStack = undoStack + document,
            redoStack = redoStack.dropLast(1),
        )
    }

}

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