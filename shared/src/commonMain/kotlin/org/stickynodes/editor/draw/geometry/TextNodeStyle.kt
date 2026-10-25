package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import org.stickynodes.editor.draw.model.EditorText
import org.stickynodes.editor.draw.style.FillStyle

fun EditorText.layoutStyle(density: Density): TextStyle =
    with(density) {
        TextStyle(
            color = Color(
                (style.fill as? FillStyle.Solid)?.color
                    ?: 0xFF0D0D0D.toInt()
            ),
            fontSize = fontSize.toSp(),
        )
    }
