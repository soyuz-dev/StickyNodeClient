
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
import org.stickynodes.editor.draw.model.*
import org.stickynodes.editor.draw.style.NodeStyle
import kotlin.uuid.Uuid

class EditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
    val clipboard: EditorNodeClipboard = EditorNodeClipboard(),
) {
    var history by mutableStateOf(
        EditorHistory(document = initialDocument)
    )
        private set

    var selectedNodeIds by mutableStateOf<Set<Uuid>>(emptySet())
        private set

    val selectedNodeId: Uuid?
        get() = selectedNodeIds.singleOrNull()

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

    val selectedNode: EditorNode?
        get() = nodes.firstOrNull { it.id == selectedNodeId }

    val selectedStyle: NodeStyle?
        get() = stylePreview
            ?.takeIf { it.first == selectedNodeId }
            ?.second ?: selectedNode?.style

    val selectedNodes: List<EditorNode>
        get() = nodes.filter { it.id in selectedNodeIds }

    val canUndo: Boolean
        get() = history.canUndo

    val canRedo: Boolean
        get() = history.canRedo

    val canAlignSelection: Boolean
        get() = selectedNodeIds.size >= 2

    val canDistributeSelection: Boolean
        get() = selectedNodeIds.size >= 3

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

        val alignedNodes = nodes.alignNodes(
            selectedIds = selectedNodeIds,
            alignment = alignment,
            selectionBounds = selectionBounds,
        )

        if (alignedNodes != nodes) commitNodes(alignedNodes)
    }

    fun distributeSelectedNodes(
        distribution: NodeDistribution,
        selectionBounds: (EditorNode) -> Rect,
    ) {
        if (!canDistributeSelection) return

        val distributedNodes = nodes.distributeNodes(
            selectedIds = selectedNodeIds,
            distribution = distribution,
            selectionBounds = selectionBounds,
        )

        if (distributedNodes != nodes) commitNodes(distributedNodes)
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

        if (selectedNodeIds.size < 2) return

        val rectangles = selectedNodes.map(selectionBounds)
        val bounds = rectangles.enclosingBounds() ?: return

        val delta = position - bounds.topLeft
        if (delta == Offset.Zero) return

        val updatedNodes = selectedNodes.map { node ->
            node.translatedBy(delta)
        }

        commitNodes(updatedNodes)
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

        val resized = shapes.resizedTogether(
            handle = ResizeHandle.SE,
            delta = delta,
        )

        updateNodes(resized)
    }

    fun rotateSelectedShapesBy(angle: Float) {
        require(angle.isFinite()) {
            "Rotation must be finite"
        }

        if (!canRotateGroup || angle == 0f) return

        val shapes = selectedNodes.filterIsInstance<EditorShape>()
        val center = shapes.combinedVisualBounds()?.center ?: return

        val rotated = shapes.rotatedTogether(
            angle = angle,
            center = center,
        )

        updateNodes(rotated)
    }

    fun open(document: EditorDocument) {
        cancelNudge()
        history = EditorHistory(document = document)
        selectedNodeIds = emptySet()
        stylePreview = null
    }

    fun selectNode(id: Uuid?) {
        val selection = if (id == null) emptySet() else setOf(id)
        selectNodes(selection)
    }

    fun toggleNodeSelection(id: Uuid) {
        if (nodes.none { it.id == id }) return

        val selection = if (id in selectedNodeIds) {
            selectedNodeIds - id
        } else {
            selectedNodeIds + id
        }

        selectNodes(selection)
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

    fun selectAllNodes() {
        selectNodes(nodes.mapTo(mutableSetOf()) { it.id })
    }

    fun cancelStylePreview() {
        stylePreview = null
    }

    private fun commitMembers(
        updatedMembers: List<EditorGroupMember>,
    ) {
        if (updatedMembers == document.members) return

        cancelNudge()
        stylePreview = null

        history = history.commit(
            document.copy(members = updatedMembers)
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
        require(nodes.none { it.id == node.id }) {
            "A node with this ID already exists"
        }

        commitMembers(document.members + node)
    }

    fun updateNode(updated: EditorNode) {
        commitNodes(listOf(updated))
    }

    fun updateNodes(updatedNodes: List<EditorNode>) {
        commitNodes(updatedNodes)
    }

    fun deleteSelectedNodes() {
        if (selectedNodeIds.isEmpty()) return

        val updatedMembers = document.members.removeNodes(
            selectedNodeIds
        )

        commitMembers(updatedMembers)
        clearSelection()
    }

    fun groupSelectedNodes(): Boolean {
        if (selectedNodeIds.size < 2) return false

        val updatedMembers = document.members.groupNodes(
            selectedNodeIds
        ) ?: return false

        commitMembers(updatedMembers)
        clearSelection()

        return true
    }


    fun ungroup(id: Uuid): Boolean {
        val updatedMembers = document.members.ungroup(id) ?: return false

        commitMembers(updatedMembers)
        return true
    }


    fun undo() {
        if (stylePreview != null) {
            stylePreview = null
            return
        }

        cancelNudge()
        history = history.undo()
        validateSelection()
    }

    fun redo() {
        cancelNudge()
        stylePreview = null
        history = history.redo()
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
        val existingIds = nodes.mapTo(mutableSetOf()) { it.id }
        selectedNodeIds = selectedNodeIds.intersect(existingIds)
    }

    fun duplicateSelectedNodes(
        offset: Offset = Offset(20f, 20f),
    ) {
        if (selectedNodeIds.isEmpty()) return

        val originals = selectedNodes
        if (originals.isEmpty()) return

        val duplicates = originals.map { node ->
            node.duplicatedBy(offset)
        }

        commitMembers(document.members + duplicates)

        selectedNodeIds = duplicates
            .mapTo(mutableSetOf()) { it.id }
    }

    fun copySelectedNodes() {
        clipboard.copy(
            nodes = nodes,
            selectedIds = selectedNodeIds,
        )
    }

    fun cutSelectedNodes() {
        if (selectedNodeIds.isEmpty()) return

        copySelectedNodes()
        deleteSelectedNodes()
    }

    fun pasteNodes(
        offset: Offset = Offset(20f, 20f),
    ) {
        val pasted = clipboard.paste(offset)
        if (pasted.isEmpty()) return

        commitMembers(document.members + pasted)

        selectedNodeIds = pasted
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

    private fun reorderedMembers(
        operation: ReorderOperation,
    ): List<EditorGroupMember> {
        if (selectedNodeIds.isEmpty()) return document.members

        return document.members.reorderNodes(
            selectedIds = selectedNodeIds,
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

private enum class ReorderOperation {
    FORWARD,
    BACKWARD,
    FRONT,
    BACK,
}

private fun List<EditorGroupMember>.removeNodes(
    selectedIds: Set<Uuid>,
): List<EditorGroupMember> {
    val result = mutableListOf<EditorGroupMember>()

    for (member in this) {
        when (member) {
            is EditorNode -> {
                if (member.id !in selectedIds) {
                    result.add(member)
                }
            }

            is EditorGroup -> {
                val remaining = member.members.removeNodes(selectedIds)

                if (remaining.isNotEmpty()) {
                    val updated = if (remaining == member.members) {
                        member
                    } else {
                        member.copy(members = remaining)
                    }

                    result.add(updated)
                }
            }
        }
    }

    return result
}

private fun List<EditorGroupMember>.reorderNodes(
    selectedIds: Set<Uuid>,
    operation: ReorderOperation,
): List<EditorGroupMember> {
    val reordered = map { member ->
        when (member) {
            is EditorNode -> member

            is EditorGroup -> {
                val children = member.members.reorderNodes(
                    selectedIds = selectedIds,
                    operation = operation,
                )

                if (children == member.members) {
                    member
                } else {
                    member.copy(members = children)
                }
            }
        }
    }.toMutableList()

    fun isSelected(member: EditorGroupMember): Boolean =
        member is EditorNode && member.id in selectedIds

    when (operation) {
        ReorderOperation.FRONT -> {
            val selected = reordered.filter(::isSelected)
            reordered.removeAll(selected)
            reordered.addAll(selected)
        }

        ReorderOperation.BACK -> {
            val selected = reordered.filter(::isSelected)
            reordered.removeAll(selected)
            reordered.addAll(0, selected)
        }

        ReorderOperation.FORWARD -> {
            for (index in reordered.lastIndex - 1 downTo 0) {
                if (
                    isSelected(reordered[index]) &&
                    !isSelected(reordered[index + 1])
                ) {
                    val current = reordered[index]
                    reordered[index] = reordered[index + 1]
                    reordered[index + 1] = current
                }
            }
        }

        ReorderOperation.BACKWARD -> {
            for (index in 1..reordered.lastIndex) {
                if (
                    isSelected(reordered[index]) &&
                    !isSelected(reordered[index - 1])
                ) {
                    val current = reordered[index]
                    reordered[index] = reordered[index - 1]
                    reordered[index - 1] = current
                }
            }
        }
    }

    return reordered
}


private fun List<EditorGroupMember>.groupNodes(
    selectedIds: Set<Uuid>,
): List<EditorGroupMember>? {
    val selectedIndices = indices.filter { index ->
        val member = this[index]
        member is EditorNode && member.id in selectedIds
    }

    if (selectedIndices.size == selectedIds.size &&
        selectedIndices.size >= 2
    ) {
        val selectedMembers = selectedIndices.map { this[it] }
        val insertIndex = selectedIndices.last()

        val group = EditorGroup(
            members = selectedMembers
        )

        return buildList {
            for (index in this@groupNodes.indices) {
                if (index == insertIndex) {
                    add(group)
                }

                val member = this@groupNodes[index]

                if (index !in selectedIndices) {
                    add(member)
                }
            }
        }
    }

    for ((index, member) in withIndex()) {
        if (member !is EditorGroup) continue

        val updatedChildren = member.members.groupNodes(
            selectedIds
        ) ?: continue

        return mapIndexed { i, current ->
            if (i == index) {
                member.copy(members = updatedChildren)
            } else {
                current
            }
        }
    }

    return null
}

private fun List<EditorGroupMember>.ungroup(
    id: Uuid,
): List<EditorGroupMember>? {
    for ((index, member) in withIndex()) {
        if (member !is EditorGroup) continue

        if (member.id == id) {
            return buildList {
                addAll(this@ungroup.take(index))
                addAll(member.members)
                addAll(this@ungroup.drop(index + 1))
            }
        }

        val updatedChildren = member.members.ungroup(id)
            ?: continue

        return mapIndexed { i, current ->
            if (i == index) {
                member.copy(members = updatedChildren)
            } else {
                current
            }
        }
    }

    return null
}

@Composable
fun rememberEditorDocumentState(
    initialDocument: EditorDocument = EditorDocument(),
): EditorDocumentState =
    remember { EditorDocumentState(initialDocument) }
