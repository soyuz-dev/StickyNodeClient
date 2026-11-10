
package org.stickynodes.editor.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.stickynodes.editor.draw.geometry.*
import org.stickynodes.editor.draw.geometry.connector.anchorPosition
import org.stickynodes.editor.draw.geometry.connector.detachRemovedNodes
import org.stickynodes.editor.draw.geometry.selection.NodeAlignment
import org.stickynodes.editor.draw.geometry.selection.NodeDistribution
import org.stickynodes.editor.draw.geometry.selection.NodeSizing
import org.stickynodes.editor.draw.geometry.selection.alignedMemberNodes
import org.stickynodes.editor.draw.geometry.selection.distributedMemberNodes
import org.stickynodes.editor.draw.geometry.selection.matchShapeSizes
import org.stickynodes.editor.draw.geometry.transform.ResizeHandle
import org.stickynodes.editor.draw.geometry.transform.combinedBounds
import org.stickynodes.editor.draw.geometry.transform.positionedAt
import org.stickynodes.editor.draw.geometry.transform.resizedTogether
import org.stickynodes.editor.draw.geometry.transform.rotatedTogether
import org.stickynodes.editor.draw.geometry.transform.translatedBy
import org.stickynodes.editor.draw.model.*
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.withStyle
import kotlin.uuid.Uuid

class EditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
    val clipboard: EditorNodeClipboard = EditorNodeClipboard(),
) {
    var history by mutableStateOf(
        EditorHistory(document = initialDocument)
    )
        private set

    var selectedMemberIds by mutableStateOf<Set<Uuid>>(emptySet())
        private set

    val selectedMemberId: Uuid?
        get() = selectedMemberIds.singleOrNull()

    val selectedMembers: List<EditorGroupMember>
        get() = document.members.flattenedMembers()
            .filter { it.id in selectedMemberIds }

    var selectedConnectorId by mutableStateOf<Uuid?>(null)
        private set

    val selectedConnector: EditorConnector?
        get() = connectors.firstOrNull {
            it.id == selectedConnectorId
        }

    val selectedMember: EditorGroupMember?
        get() = selectedMembers.singleOrNull()

    val selectedNodes: List<EditorNode>
        get() = selectedMembers.flatMap { it.flattenedNodes() }

    val selectedNodeIds: Set<Uuid>
        get() = selectedNodes.mapTo(mutableSetOf()) { it.id }

    val selectedNodeId: Uuid?
        get() = (selectedMember as? EditorNode)?.id

    val selectedNode: EditorNode?
        get() = selectedMember as? EditorNode

    val connectors: List<EditorConnector>
        get() = document.connectors

    fun addConnector(connector: EditorConnector) {
        require(connectors.none { it.id == connector.id }) {
            "A connector with this ID already exists"
        }

        history = history.commit(
            document.copy(
                connectors = connectors + connector,
            )
        )
    }

    fun updateConnector(connector: EditorConnector) {
        val index = connectors.indexOfFirst {
            it.id == connector.id
        }

        if (index == -1) return

        if (connectors[index] == connector) return

        history = history.commit(
            document.copy(
                connectors = connectors.toMutableList().apply {
                    this[index] = connector
                },
            )
        )
    }

    fun deleteConnector(id: Uuid) {
        val updated = connectors.filterNot {
            it.id == id
        }

        if (updated == connectors) return

        history = history.commit(
            document.copy(connectors = updated)
        )

        if (selectedConnectorId == id) {
            selectedConnectorId = null
        }
    }

    private var stylePreview by
    mutableStateOf<Pair<Uuid, NodeStyle>?>(null)

    val document: EditorDocument
        get() = history.document

    val nodes: List<EditorNode>
        get() = document.nodes

    val displayNodes: List<EditorNode>
        get() {
            val baseNodes = nudgePreviewNodes ?: nodes

            return stylePreview?.let { (id, style) ->
                baseNodes.map { node ->
                    if (node.id == id) node.withStyle(style)
                    else node
                }
            } ?: baseNodes
        }

    val selectedStyle: NodeStyle?
        get() = stylePreview
            ?.takeIf { it.first == selectedNodeId }
            ?.second ?: selectedNode?.style

    val canUndo: Boolean
        get() = history.canUndo

    val canRedo: Boolean
        get() = history.canRedo

    val canAlignSelection: Boolean
        get() = selectedMembers.size >= 2

    val canDistributeSelection: Boolean
        get() = selectedMembers.size >= 3

    val canMatchShapeSizes: Boolean
        get() = selectedNodes.count { it is EditorShape } >= 2

    val canResizeGroup: Boolean
        get() = selectedNodes.size >= 2 &&
                selectedNodes.all { node ->
                    node is EditorShape && node.rotation == 0f
                }

    val canRotateGroup: Boolean
        get() = selectedNodes.size >= 2 &&
                selectedNodes.all { it is EditorShape }

    val canBringSelectionForward: Boolean
        get() = reorderedMembers(ReorderOperation.FORWARD) != document.members

    val canBringSelectionToFront: Boolean
        get() = reorderedMembers(ReorderOperation.FRONT) != document.members

    val canSendSelectionBackward: Boolean
        get() = reorderedMembers(ReorderOperation.BACKWARD) != document.members

    val canSendSelectionToBack: Boolean
        get() = reorderedMembers(ReorderOperation.BACK) != document.members

    private var nudgeStartNodes: List<EditorNode>? = null
    private var nudgeDelta = Offset.Zero
    private var nudgeSelectedIds: Set<Uuid>? = null

    private var nudgePreviewNodes by
    mutableStateOf<List<EditorNode>?>(null)

    fun alignSelectedNodes(
        alignment: NodeAlignment,
        selectionBounds: (EditorNode) -> Rect,
    ) {
        if (!canAlignSelection) return

        val alignedNodes = selectedMembers.alignedMemberNodes(
            alignment = alignment,
            selectionBounds = selectionBounds,
        )

        commitNodes(alignedNodes)
    }

    fun distributeSelectedNodes(
        distribution: NodeDistribution,
        selectionBounds: (EditorNode) -> Rect,
    ) {
        if (!canDistributeSelection) return

        val distributedNodes = selectedMembers.distributedMemberNodes(
            distribution = distribution,
            selectionBounds = selectionBounds
        )

        commitNodes(distributedNodes)
    }

    fun matchSelectedShapeSizes(sizing: NodeSizing) {
        if (!canMatchShapeSizes) return

        val resizedNodes = nodes.matchShapeSizes(
            selectedIds = selectedNodeIds,
            sizing = sizing,
        )

        if (resizedNodes != nodes) commitNodes(resizedNodes)
    }

    fun positionSelectedNodeAt(position: Offset) {
        require(position.x.isFinite() && position.y.isFinite())

        val node = selectedNode ?: return
        updateNode(node.positionedAt(position))
    }

    fun positionSelectedNodesAt(
        position: Offset,
        selectionBounds: (EditorNode) -> Rect,
    ) {
        require(position.x.isFinite() && position.y.isFinite())
        if (selectedNodes.size < 2) return

        val bounds = selectedNodes
            .map(selectionBounds)
            .enclosingBounds() ?: return

        val delta = position - bounds.topLeft
        if (delta == Offset.Zero) return

        commitNodes(
            selectedNodes.map { it.translatedBy(delta) }
        )
    }

    fun resizeSelectedShapesTo(size: Size) {
        require(size.width.isFinite() && size.height.isFinite())
        require(size.width > 0f && size.height > 0f)

        if (!canResizeGroup) return

        val shapes = selectedNodes.filterIsInstance<EditorShape>()
        val bounds = shapes.combinedBounds() ?: return

        val delta = Offset(
            size.width - bounds.width,
            size.height - bounds.height,
        )

        if (delta == Offset.Zero) return

        updateNodes(
            shapes.resizedTogether(
                handle = ResizeHandle.SE,
                delta = delta,
            )
        )
    }

    fun rotateSelectedShapesBy(angle: Float) {
        require(angle.isFinite()) {
            "Rotation must be finite"
        }

        if (!canRotateGroup || angle == 0f) return

        val shapes = selectedNodes.filterIsInstance<EditorShape>()
        val center = shapes.combinedVisualBounds()?.center ?: return

        updateNodes(
            shapes.rotatedTogether(
                angle = angle,
                center = center,
            )
        )
    }

    fun open(document: EditorDocument) {
        cancelNudge()
        history = EditorHistory(document = document)
        selectedMemberIds = emptySet()
        stylePreview = null
    }

    fun selectMember(id: Uuid?) {
        selectMembers(
            if (id == null) emptySet() else setOf(id)
        )
    }

    fun selectMembers(ids: Set<Uuid>) {
        val selection = document.normalizeMemberSelection(ids)

        if (selection != selectedMemberIds) {
            cancelNudge()
            stylePreview = null
            selectedMemberIds = selection
        }

        selectedConnectorId = null
    }

    fun selectHitNode(
        hitNodeId: Uuid,
        extendSelection: Boolean = false,
    ) {
        val selection = document.resolveMemberSelection(
            hitNodeId = hitNodeId,
            selectedMemberIds = selectedMemberIds,
            extendSelection = extendSelection,
        )

        selectMembers(selection)
    }

    fun selectNode(id: Uuid?) {
        if (id != null && nodes.none { it.id == id }) return
        selectMember(id)
    }

    fun toggleNodeSelection(id: Uuid) {
        if (nodes.none { it.id == id }) return

        val selection = if (id in selectedMemberIds) {
            selectedMemberIds - id
        } else {
            selectedMemberIds + id
        }

        selectMembers(selection)
    }

    fun selectNodes(ids: Set<Uuid>) {
        val validIds = nodes.mapTo(mutableSetOf()) { it.id }
        selectMembers(ids.intersect(validIds))
    }

    fun selectConnector(id: Uuid?) {
        if (id != null && connectors.none { it.id == id }) return

        if (id != null) selectMembers(emptySet())

        selectedConnectorId = id
    }

    fun clearSelection() {
        cancelNudge()
        stylePreview = null
        selectedMemberIds = emptySet()
        selectedConnectorId = null
    }

    fun selectAllNodes() {
        selectNodes(nodes.mapTo(mutableSetOf()) { it.id })
    }

    fun selectAllMembers() {
        selectMembers(document.members.mapTo(mutableSetOf()) { it.id })
    }

    fun cancelStylePreview() {
        stylePreview = null
    }

    private fun commitMembers(
        updatedMembers: List<EditorGroupMember>,
        updatedConnectors: List<EditorConnector> = document.connectors,
    ) {
        if (updatedMembers == document.members && updatedConnectors == document.connectors) return

        cancelNudge()
        stylePreview = null

        history = history.commit(
            document.copy(
                members = updatedMembers,
                connectors = updatedConnectors,
            )
        )

        validateSelection()
    }

    fun commitNodes(updatedNodes: List<EditorNode>) {
        if (updatedNodes.isEmpty()) return

        val replacements = updatedNodes.associateBy { it.id }

        val updatedMembers = document.members.mapNodes { node ->
            replacements[node.id] ?: node
        }

        commitMembers(updatedMembers)
    }

    fun addNode(node: EditorNode) {
        require(document.members.pathToMember(node.id) == null) {
            "A member with this ID already exists"
        }

        commitMembers(document.members + node)
    }

    fun updateNode(updated: EditorNode) {
        commitNodes(listOf(updated))
    }

    fun updateNodes(updatedNodes: List<EditorNode>) {
        commitNodes(updatedNodes)
    }


    fun deleteSelectedMembers() {
        if (selectedMemberIds.isEmpty()) return

        val originalDocument = document

        val updatedMembers = originalDocument.members.removeMembers(
            selectedMemberIds
        )

        val remainingNodeIds = updatedMembers
            .flatMap { it.flattenedNodes() }
            .mapTo(mutableSetOf()) { it.id }

        val removedNodeIds = originalDocument.nodes
            .mapTo(mutableSetOf()) { it.id }
            .apply {
                removeAll(remainingNodeIds)
            }

        val originalNodesById = originalDocument.nodes.associateBy {
            it.id
        }

        val updatedConnectors =
            originalDocument.connectors.detachRemovedNodes(
                removedNodeIds = removedNodeIds,
                resolvePosition = { endpoint ->
                    val node = originalNodesById[endpoint.nodeId]

                    when (node) {
                        is EditorShape ->
                            endpoint.anchor?.let {
                                node.anchorPosition(it)
                            } ?: node.bounds.center

                        else -> null
                    }
                },
            )

        commitMembers(
            updatedMembers = updatedMembers,
            updatedConnectors = updatedConnectors,
        )

        clearSelection()
    }

    fun deleteSelectedNodes() {
        deleteSelectedMembers()
    }

    fun groupSelectedMembers(): Boolean {
        if (selectedMemberIds.size < 2) return false

        val result = document.members.groupMembers(
            selectedMemberIds
        ) ?: return false

        commitMembers(result.members)
        selectMember(result.groupId)

        return true
    }

    fun groupSelectedNodes(): Boolean {
        if (selectedMemberIds.size < 2) return false
        if (selectedMembers.any { it !is EditorNode }) return false

        val result = document.members.groupMembers(
            selectedMemberIds
        ) ?: return false

        commitMembers(result.members)
        clearSelection()

        return true
    }

    fun ungroup(id: Uuid): Boolean {
        val updated = document.members.ungroup(id) ?: return false

        commitMembers(updated)
        return true
    }

    fun ungroupSelectedMembers(): Boolean {
        val group = selectedMember as? EditorGroup ?: return false
        return ungroup(group.id)
    }

    fun undo() {
        if (stylePreview != null) {
            stylePreview = null
            return
        }

        cancelNudge()
        history = history.undo()

        selectedConnectorId = selectedConnectorId
            ?.takeIf { id -> connectors.any { it.id == id } }
        validateSelection()
    }

    fun redo() {
        cancelNudge()
        stylePreview = null
        history = history.redo()


        selectedConnectorId = selectedConnectorId
            ?.takeIf { id -> connectors.any { it.id == id } }
        validateSelection()
    }

    fun previewSelectedStyle(style: NodeStyle): Boolean {
        val id = selectedNodeId ?: return false
        if (nodes.none { it.id == id }) return false

        stylePreview = id to style
        return true
    }

    fun commitSelectedStyle(style: NodeStyle): Boolean {
        val node = selectedNode ?: return false
        updateNode(node.withStyle(style))
        return true
    }

    fun finishStyleChange() {
        val (id, style) = stylePreview ?: return
        stylePreview = null

        val node = nodes.firstOrNull { it.id == id } ?: return
        updateNode(node.withStyle(style))
    }

    private fun validateSelection() {
        selectMembers(selectedMemberIds)
    }


    fun duplicateSelectedMembers(
        offset: Offset = Offset(20f, 20f),
    ) {
        if (selectedMemberIds.isEmpty()) return

        val result = document.members.duplicateSelectedSiblings(
            selectedIds = selectedMemberIds,
            offset = offset,
        ) ?: return

        commitMembers(result.members)

        selectMembers(result.duplicateIds)
    }

    fun duplicateSelectedNodes(
        offset: Offset = Offset(20f, 20f),
    ) {
        duplicateSelectedMembers(offset)
    }

    fun copySelectedMembers() {
        if (selectedMemberIds.isEmpty()) return

        val originals = document.members.selectedSiblings(
            selectedMemberIds
        ) ?: return

        clipboard.copyMembers(
            members = originals,
            selectedIds = selectedMemberIds,
        )
    }

    fun copySelectedNodes() {
        copySelectedMembers()
    }

    fun cutSelectedMembers() {
        if (selectedMemberIds.isEmpty()) return

        copySelectedMembers()
        deleteSelectedMembers()
    }

    fun cutSelectedNodes() {
        cutSelectedMembers()
    }

    fun pasteMembers(
        offset: Offset = Offset(20f, 20f),
    ) {
        val pasted = clipboard.pasteMembers(offset)
        if (pasted.isEmpty()) return

        commitMembers(document.members + pasted)

        selectMembers(
            pasted.mapTo(mutableSetOf()) { it.id }
        )
    }

    fun pasteNodes(
        offset: Offset = Offset(20f, 20f),
    ) {
        pasteMembers(offset)
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

    private fun reorderedMembers(
        operation: ReorderOperation,
    ): List<EditorGroupMember> {
        if (selectedMemberIds.isEmpty()) return document.members

        return document.members.reorderMembers(
            selectedIds = selectedMemberIds,
            operation = operation,
        )
    }

    fun bringSelectionForward() {
        commitMembers(reorderedMembers(ReorderOperation.FORWARD))
    }

    fun sendSelectionBackward() {
        commitMembers(reorderedMembers(ReorderOperation.BACKWARD))
    }

    fun bringSelectionToFront() {
        commitMembers(reorderedMembers(ReorderOperation.FRONT))
    }

    fun sendSelectionToBack() {
        commitMembers(reorderedMembers(ReorderOperation.BACK))
    }
}

@Composable
fun rememberEditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
): EditorDocumentState =
    remember { EditorDocumentState(initialDocument) }
