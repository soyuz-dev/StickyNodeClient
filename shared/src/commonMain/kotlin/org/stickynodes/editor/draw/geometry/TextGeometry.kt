package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import org.stickynodes.editor.draw.model.EditorText

fun EditorText.measureBounds(
    textMeasurer: TextMeasurer,
    style: TextStyle,
): Rect {
    val layout = textMeasurer.measure(
        text = text,
        style = style,
        softWrap = false,
    )

    return Rect(
        left = x,
        top = y,
        right = x + layout.size.width,
        bottom = y + layout.size.height,
    )
}
