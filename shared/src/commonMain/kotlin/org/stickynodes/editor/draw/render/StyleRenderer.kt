package org.stickynodes.editor.draw.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import org.stickynodes.editor.draw.style.StrokeStyle
import androidx.compose.ui.graphics.StrokeCap as ComposeCap
import androidx.compose.ui.graphics.StrokeJoin as ComposeJoin
import org.stickynodes.editor.draw.style.StrokeCap as ModelCap
import org.stickynodes.editor.draw.style.StrokeJoin as ModelJoin

internal fun Int.toComposeColor(): Color = Color(this)

internal fun StrokeStyle.toDrawStyle(): DrawStyle =
    Stroke(
        width = width,
        cap = when (cap) {
            ModelCap.Round -> ComposeCap.Round
            ModelCap.Square -> ComposeCap.Square
        },
        join = when (join) {
            ModelJoin.Round -> ComposeJoin.Round
            ModelJoin.Bevel -> ComposeJoin.Bevel
        },
        pathEffect = dashPattern?.let {
            PathEffect.dashPathEffect(it.toFloatArray())
        }
    )