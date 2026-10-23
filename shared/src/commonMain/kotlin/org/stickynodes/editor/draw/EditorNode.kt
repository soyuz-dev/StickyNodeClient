package org.stickynodes.editor.draw

import kotlin.uuid.Uuid

sealed interface EditorNode {
    val id: Uuid
}