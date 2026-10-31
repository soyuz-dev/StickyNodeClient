
package org.stickynodes.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import org.stickynodes.editor.tools.EditorTool

sealed interface EditorShortcut {
    data object Save : EditorShortcut
    data object Open : EditorShortcut
    data object Undo : EditorShortcut
    data object Redo : EditorShortcut
    data object DeleteSelection : EditorShortcut
    data object DuplicateSelection : EditorShortcut
    data object SelectAll : EditorShortcut
    data object Escape : EditorShortcut
    data object Cut: EditorShortcut
    data object Copy: EditorShortcut
    data object Paste: EditorShortcut

    data class SelectTool(
        val tool: EditorTool,
    ) : EditorShortcut

    data class Nudge(val delta: Offset) : EditorShortcut
}

fun KeyEvent.editorShortcut(): EditorShortcut? {
    if (type != KeyEventType.KeyDown) return null

    val ctrl = isCtrlPressed
    val alt = isAltPressed
    val shift = isShiftPressed
    val meta = isMetaPressed

    // Preserve the current Ctrl-based shortcuts.
    if (ctrl && !alt && !meta) {
        return when {
            !shift && key == Key.S -> EditorShortcut.Save
            !shift && key == Key.O -> EditorShortcut.Open
            !shift && key == Key.D -> EditorShortcut.DuplicateSelection
            !shift && key == Key.Z -> EditorShortcut.Undo
            !shift && key == Key.A -> EditorShortcut.SelectAll
            !shift && key == Key.C -> EditorShortcut.Copy
            !shift && key == Key.X -> EditorShortcut.Cut
            !shift && key == Key.V -> EditorShortcut.Paste
            key == Key.Y || (shift && key == Key.Z) ->
                EditorShortcut.Redo

            else -> null
        }
    }

    if (!ctrl && !alt && !meta) {
        if (key == Key.Escape) return EditorShortcut.Escape

        if (key == Key.Delete || key == Key.Backspace) {
            return EditorShortcut.DeleteSelection
        }

        if (!shift) {
            val tool = when (key) {
                Key.S -> EditorTool.SELECT
                Key.D -> EditorTool.PEN
                Key.E -> EditorTool.ELLIPSE
                Key.R -> EditorTool.RECTANGLE
                Key.T -> EditorTool.TEXT
                Key.W -> EditorTool.PAN
                else -> null
            }

            if (tool != null) {
                return EditorShortcut.SelectTool(tool)
            }
        }

        val distance = if (shift) 10f else 1f

        val delta = when (key) {
            Key.DirectionLeft -> Offset(-distance, 0f)
            Key.DirectionRight -> Offset(distance, 0f)
            Key.DirectionUp -> Offset(0f, -distance)
            Key.DirectionDown -> Offset(0f, distance)
            else -> null
        }

        if (delta != null) return EditorShortcut.Nudge(delta)
    }

    return null
}
