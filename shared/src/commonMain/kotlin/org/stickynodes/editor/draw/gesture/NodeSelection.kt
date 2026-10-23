package org.stickynodes.editor.draw.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import org.stickynodes.editor.draw.geometry.contains
import org.stickynodes.editor.draw.model.EditorNode
import kotlin.uuid.Uuid

suspend fun PointerInputScope.detectNodeSelection(
    nodes: () -> List<EditorNode>,
    screenToWorld: (Offset) -> Offset,
    onSelect: (Uuid?) -> Unit,
) {
    detectTapGestures(
        onTap = { position ->
            val worldPosition = screenToWorld(position)

            val selected = nodes()
                .asReversed()
                .firstOrNull { it.contains(worldPosition) }

            onSelect(selected?.id)
        },
    )
}