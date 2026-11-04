package org.stickynodes.editor.draw.model

import kotlin.uuid.Uuid

data class EditorGroup(
    override val id: Uuid = Uuid.random(),
    val members: List<EditorGroupMember>
): EditorGroupMember