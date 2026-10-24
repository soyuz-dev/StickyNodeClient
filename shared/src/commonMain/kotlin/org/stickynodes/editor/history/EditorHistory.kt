package org.stickynodes.editor.history

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
