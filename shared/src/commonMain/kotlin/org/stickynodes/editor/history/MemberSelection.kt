
package org.stickynodes.editor.history

import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.model.pathToMember
import kotlin.uuid.Uuid

fun EditorDocument.normalizeMemberSelection(
    memberIds: Set<Uuid>,
): Set<Uuid> {
    if (memberIds.isEmpty()) return emptySet()

    val paths = memberIds.mapNotNull { id ->
        members.pathToMember(id)
    }

    if (paths.isEmpty()) return emptySet()

    val commonDepth = paths.commonPrefixLength()
    val targetDepth = minOf(
        commonDepth,
        paths.minOf { it.lastIndex },
    )

    return paths.mapTo(mutableSetOf()) { path ->
        path[targetDepth].id
    }
}

fun EditorDocument.resolveMemberSelection(
    hitNodeId: Uuid,
    selectedMemberIds: Set<Uuid>,
    extendSelection: Boolean = false,
): Set<Uuid> {
    val hitPath = members.pathToMember(hitNodeId)
        ?: return selectedMemberIds

    if (hitPath.last() !is EditorNode) {
        return selectedMemberIds
    }

    if (!extendSelection) {
        val selectedAncestorIndex = hitPath.indexOfLast { member ->
            member.id in selectedMemberIds
        }

        val targetIndex = if (selectedAncestorIndex == -1) {
            0
        } else {
            (selectedAncestorIndex + 1)
                .coerceAtMost(hitPath.lastIndex)
        }

        return setOf(hitPath[targetIndex].id)
    }

    val selectedPaths = selectedMemberIds.mapNotNull { id ->
        members.pathToMember(id)
    }

    if (selectedPaths.isEmpty()) {
        return setOf(hitPath.first().id)
    }

    val paths = selectedPaths + listOf(hitPath)
    val commonDepth = paths.commonPrefixLength()

    val targetDepth = minOf(
        commonDepth,
        paths.minOf { it.lastIndex },
    )

    val result = paths.mapTo(mutableSetOf()) { path ->
        path[targetDepth].id
    }

    val target = hitPath[targetDepth]

    if (
        target.id == hitNodeId &&
        hitNodeId in selectedMemberIds
    ) {
        result.remove(hitNodeId)
    }

    return result
}

private fun List<List<EditorGroupMember>>.commonPrefixLength(): Int {
    if (isEmpty()) return 0

    val shortestLength = minOf { it.size }
    var length = 0

    while (length < shortestLength) {
        val id = first()[length].id

        if (any { it[length].id != id }) {
            break
        }

        length++
    }

    return length
}