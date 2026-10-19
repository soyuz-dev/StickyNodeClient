package org.stickynodes.editor

enum class EditorTool {
    SELECT,
    PAN,
    RECTANGLE,
    ELLIPSE,
    PEN,
    TEXT;

    val iconName: String
        get() = when(this) {
            SELECT -> "arrow_selector_tool"
            PAN -> "pan_tool"
            RECTANGLE -> "rectangle"
            ELLIPSE -> "circle"
            PEN -> "edit"
            TEXT -> "text_fields"
        }
}