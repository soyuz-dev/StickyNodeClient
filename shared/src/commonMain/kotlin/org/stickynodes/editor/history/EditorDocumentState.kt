package org.stickynodes.editor.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.stickynodes.editor.draw.geometry.withStyle
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

class EditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
) {
    var history by mutableStateOf(EditorHistory(document = initialDocument))
        private set

    var selectedNodeIds by mutableStateOf<Set<Uuid>>(emptySet())
        private set

    val selectedNodeId: Uuid?
        get() = selectedNodeIds.singleOrNull()

    private var stylePreview by mutableStateOf<Pair<Uuid, NodeStyle>?>(null)

    val document: EditorDocument
        get() = history.document

    val nodes: List<EditorNode>
        get() = document.nodes

    val displayNodes: List<EditorNode>
        get() = stylePreview?.let { (id, style) ->
            nodes.map { node ->
                if (node.id == id) node.withStyle(style) else node
            }
        } ?: nodes

    val selectedNode: EditorNode?
        get() = nodes.firstOrNull { it.id == selectedNodeId }

    val selectedStyle: NodeStyle?
        get() = stylePreview
            ?.takeIf { it.first == selectedNodeId }
            ?.second ?: selectedNode?.style

    val canUndo: Boolean
        get() = history.canUndo

    val canRedo: Boolean
        get() = history.canRedo

    fun open(document: EditorDocument) {
        history = EditorHistory(document = document)
        selectedNodeIds = emptySet()
        stylePreview = null
    }

    fun selectNode(id: Uuid?) {
        val selection = if (id == null) emptySet() else setOf(id)

        if (selection != selectedNodeIds) {
            stylePreview = null
            selectedNodeIds = selection
        }
    }

    fun toggleNodeSelection(id: Uuid) {
        stylePreview = null

        selectedNodeIds = if (id in selectedNodeIds) {
            selectedNodeIds - id
        } else {
            selectedNodeIds + id
        }
    }

    fun clearSelection() {
        stylePreview = null
        selectedNodeIds = emptySet()
    }

    fun cancelStylePreview() {
        stylePreview = null
    }

    fun commitNodes(updatedNodes: List<EditorNode>) {
        stylePreview = null
        history = history.commit(document.copy(nodes = updatedNodes))
        validateSelection()
    }

    fun addNode(node: EditorNode) {
        commitNodes(nodes + node)
    }

    fun updateNode(updated: EditorNode) {
        commitNodes(nodes.map { node ->
            if (node.id == updated.id) updated else node
        })
    }

    fun deleteSelectedNodes() {
        if (selectedNodeIds.isEmpty()) return

        commitNodes(nodes.filterNot { it.id in selectedNodeIds })
        clearSelection()
    }

    fun undo() {
        if (stylePreview != null) {
            stylePreview = null
            return
        }
        history = history.undo()
        validateSelection()
    }

    fun redo() {
        stylePreview = null
        history = history.redo()
        validateSelection()
    }

    /** Returns false if no selected node exists; callers may instead change tool defaults. */
    fun previewSelectedStyle(style: NodeStyle): Boolean {
        val id = selectedNodeId ?: return false
        if (nodes.none { it.id == id }) return false
        stylePreview = id to style
        return true
    }

    fun commitSelectedStyle(style: NodeStyle): Boolean {
        val id = selectedNodeId ?: return false
        if (nodes.none { it.id == id }) return false
        commitNodes(nodes.map { node ->
            if (node.id == id) node.withStyle(style) else node
        })
        return true
    }

    fun finishStyleChange() {
        val (id, style) = stylePreview ?: return
        stylePreview = null
        if (nodes.none { it.id == id }) return
        commitNodes(nodes.map { node ->
            if (node.id == id) node.withStyle(style) else node
        })
    }

    private fun validateSelection() {
        val existingIds = nodes.mapTo(mutableSetOf()) { it.id }
        selectedNodeIds = selectedNodeIds.intersect(existingIds)
    }
}

@Composable
fun rememberEditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
): EditorDocumentState = remember { EditorDocumentState(initialDocument) }
