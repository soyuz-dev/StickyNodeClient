package org.stickynodes.editor.draw.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.uuid.Uuid

sealed interface EditorNode {
    val id: Uuid
    val x: Float
    val y: Float
}