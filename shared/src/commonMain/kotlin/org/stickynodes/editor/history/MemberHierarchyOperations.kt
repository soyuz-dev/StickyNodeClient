
package org.stickynodes.editor.history

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.duplicatedBy
import org.stickynodes.editor.draw.model.EditorGroup
import org.stickynodes.editor.draw.model.EditorGroupMember
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

internal enum class ReorderOperation {
    FORWARD,
    BACKWARD,
    FRONT,
    BACK,
}

internal data class GroupingResult(
    val members: List<EditorGroupMember>,
    val groupId: Uuid,
)

internal fun List<EditorGroupMember>.removeMembers(
    selectedIds: Set<Uuid>,
): List<EditorGroupMember> {
    val result = mutableListOf<EditorGroupMember>()

    for (member in this) {
        if (member.id in selectedIds) continue

        when (member) {
            is EditorNode -> result.add(member)

            is EditorGroup -> {
                val remaining = member.members.removeMembers(
                    selectedIds
                )

                if (remaining.isNotEmpty()) {
                    result.add(
                        if (remaining == member.members) member
                        else member.copy(members = remaining)
                    )
                }
            }
        }
    }

    return result
}

internal fun List<EditorGroupMember>.selectedSiblings(
    selectedIds: Set<Uuid>,
): List<EditorGroupMember>? {
    if (selectedIds.isEmpty()) return null

    val selected = filter { it.id in selectedIds }

    if (selected.size == selectedIds.size) {
        return selected
    }

    for (member in this) {
        if (member !is EditorGroup) continue

        val children = member.members.selectedSiblings(
            selectedIds
        )

        if (children != null) return children
    }

    return null
}

internal fun EditorGroupMember.duplicateMember(
    offset: Offset,
): EditorGroupMember = when (this) {
    is EditorNode -> duplicatedBy(offset)

    is EditorGroup -> EditorGroup(
        members = members.map { member ->
            member.duplicateMember(offset)
        }
    )
}

internal fun List<EditorGroupMember>.reorderMembers(
    selectedIds: Set<Uuid>,
    operation: ReorderOperation,
): List<EditorGroupMember> {
    val reordered = map { member ->
        if (member !is EditorGroup) {
            member
        } else {
            val children = member.members.reorderMembers(
                selectedIds,
                operation,
            )

            if (children == member.members) member
            else member.copy(members = children)
        }
    }.toMutableList()

    fun isSelected(member: EditorGroupMember): Boolean =
        member.id in selectedIds

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

internal fun List<EditorGroupMember>.groupMembers(
    selectedIds: Set<Uuid>,
): GroupingResult? {
    val selectedIndices = indices.filter { index ->
        this[index].id in selectedIds
    }

    if (
        selectedIndices.size == selectedIds.size &&
        selectedIndices.size >= 2
    ) {
        val group = EditorGroup(
            members = selectedIndices.map { this[it] }
        )

        val insertIndex = selectedIndices.last()

        val updated = buildList {
            for (index in this@groupMembers.indices) {
                if (index == insertIndex) add(group)

                if (index !in selectedIndices) {
                    add(this@groupMembers[index])
                }
            }
        }

        return GroupingResult(updated, group.id)
    }

    for ((index, member) in withIndex()) {
        if (member !is EditorGroup) continue

        val result = member.members.groupMembers(
            selectedIds
        ) ?: continue

        val updated = mapIndexed { i, current ->
            if (i == index) {
                member.copy(members = result.members)
            } else {
                current
            }
        }

        return GroupingResult(updated, result.groupId)
    }

    return null
}

internal fun List<EditorGroupMember>.ungroup(
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


internal data class DuplicationResult(
    val members: List<EditorGroupMember>,
    val duplicateIds: Set<Uuid>,
)

internal fun List<EditorGroupMember>.duplicateSelectedSiblings(
    selectedIds: Set<Uuid>,
    offset: Offset,
): DuplicationResult? {
    if (selectedIds.isEmpty()) return null

    val selected = filter { it.id in selectedIds }

    if (selected.size == selectedIds.size) {
        val duplicateIds = mutableSetOf<Uuid>()

        val updated = buildList {
            for (member in this@duplicateSelectedSiblings) {
                add(member)

                if (member.id in selectedIds) {
                    val duplicate = member.duplicateMember(offset)

                    add(duplicate)
                    duplicateIds.add(duplicate.id)
                }
            }
        }

        return DuplicationResult(
            members = updated,
            duplicateIds = duplicateIds,
        )
    }

    for ((index, member) in withIndex()) {
        if (member !is EditorGroup) continue

        val result = member.members.duplicateSelectedSiblings(
            selectedIds = selectedIds,
            offset = offset,
        ) ?: continue

        val updated = mapIndexed { i, current ->
            if (i == index) {
                member.copy(members = result.members)
            } else {
                current
            }
        }

        return DuplicationResult(
            members = updated,
            duplicateIds = result.duplicateIds,
        )
    }

    return null
}