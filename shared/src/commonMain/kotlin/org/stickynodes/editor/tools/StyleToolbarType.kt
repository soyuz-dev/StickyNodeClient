package org.stickynodes.editor.tools

enum class StyleToolbarType {
    SHAPE,
    TEXT,
    FREEHAND,
}

fun EditorTool.preferredStyleToolbar(): StyleToolbarType? =
    when (this) {
        EditorTool.RECTANGLE,
        EditorTool.ELLIPSE -> StyleToolbarType.SHAPE

        EditorTool.TEXT -> StyleToolbarType.TEXT
        EditorTool.PEN -> StyleToolbarType.FREEHAND

        else -> null
    }
