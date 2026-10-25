package org.stickynodes.editor.tools

enum class EditorTool(
    val label: String,
    val shortcut: String,
    val iconName: String,
) {
    SELECT("Select", "S", "arrow_selector_tool"),
    PAN("Pan", "W", "pan_tool"),
    RECTANGLE("Rectangle", "R", "rectangle"),
    ELLIPSE("Ellipse", "E", "circle"),
    PEN("Pen", "D", "edit"),
    TEXT("Text", "T", "text_fields"),
}