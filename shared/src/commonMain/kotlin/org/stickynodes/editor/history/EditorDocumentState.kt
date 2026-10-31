package org.stickynodes.editor.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.internal.composableLambdaInstance
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.bringForward
import org.stickynodes.editor.draw.geometry.bringToFront
import org.stickynodes.editor.draw.geometry.duplicatedBy
import org.stickynodes.editor.draw.geometry.sendBackward
import org.stickynodes.editor.draw.geometry.sendToBack
import org.stickynodes.editor.draw.geometry.translatedBy
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
        get() {
            val baseNodes = nudgePreviewNodes ?: nodes

            return stylePreview?.let { (id, style) ->
                baseNodes.map { node ->
                    if (node.id == id) node.withStyle(style) else node
                }
            } ?: baseNodes
        }

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

    private var nudgeStartNodes: List<EditorNode>? = null
    private var nudgeDelta = Offset.Zero
    private var nudgeSelectedIds: Set<Uuid>? = null
    private var nudgePreviewNodes by
    mutableStateOf<List<EditorNode>?>(null)

    fun open(document: EditorDocument) {
        cancelNudge()
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

    fun selectNodes(ids: Set<Uuid>) {
        val validIds = nodes.mapTo(mutableSetOf()) { it.id }
        val selection = ids.intersect(validIds)

        if (selection != selectedNodeIds) {
            stylePreview = null
            selectedNodeIds = selection
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
        cancelNudge()
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

    fun updateNodes(updatedNodes: List<EditorNode>) {
        if (updatedNodes.isEmpty()) return

        val replacements = updatedNodes.associateBy { it.id }

        commitNodes(nodes.map { node ->
            replacements[node.id] ?: node
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

    fun duplicateSelectedNodes(
        offset: Offset = Offset(20f, 20f)
    ) {
        if (selectedNodeIds.isEmpty()) return

        val originals = nodes.filter { node ->
            node.id in selectedNodeIds
        }

        if (originals.isEmpty()) return

        val duplicates = originals.map { node ->
            node.duplicatedBy(offset)
        }

        commitNodes(nodes + duplicates)

        selectedNodeIds = duplicates
            .mapTo(mutableSetOf()) { it.id }
    }

    fun beginNudge() {
        if (nudgeStartNodes != null) return
        if (selectedNodeIds.isEmpty()) return

        nudgeStartNodes = nodes
        nudgeSelectedIds = selectedNodeIds.toSet()
        nudgeDelta = Offset.Zero
    }

    fun updateNudge(delta: Offset) {
        val originalNodes = nudgeStartNodes ?: return
        val ids = nudgeSelectedIds ?: return

        nudgeDelta += delta

        nudgePreviewNodes = originalNodes.map { node ->
            if (node.id in ids) {
                node.translatedBy(nudgeDelta)
            } else {
                node
            }
        }
    }

    fun finishNudge() {
        val preview = nudgePreviewNodes
        val moved = nudgeDelta != Offset.Zero

        nudgeStartNodes = null
        nudgeSelectedIds = null
        nudgeDelta = Offset.Zero
        nudgePreviewNodes = null

        if (moved && preview != null) {
            commitNodes(preview)
        }
    }

    fun cancelNudge() {
        nudgeStartNodes = null
        nudgeSelectedIds = null
        nudgeDelta = Offset.Zero
        nudgePreviewNodes = null
    }

    fun bringSelectionForward() {
        if(selectedNodeIds.isEmpty()) return
        commitNodes(nodes.bringForward(selectedNodeIds))
    }

    fun sendSelectionBackward() {
        if(selectedNodeIds.isEmpty()) return
        commitNodes(nodes.sendBackward(selectedNodeIds))
    }

    fun bringSelectionToFront() {
        if(selectedNodeIds.isEmpty()) return
        commitNodes(nodes.bringToFront(selectedNodeIds))
    }

    fun sendSelectionToBack() {
        if(selectedNodeIds.isEmpty()) return
        commitNodes(nodes.sendToBack(selectedNodeIds))
    }

}

@Composable
fun rememberEditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
): EditorDocumentState = remember { EditorDocumentState(initialDocument) }
