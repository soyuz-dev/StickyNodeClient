package org.stickynodes.editor.tools.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
internal fun NoColorSwatch(
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(4.dp)

    Canvas(
        modifier = modifier
            .size(24.dp)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = shape,
            )
    ) {
        drawRoundRect(
            color = Color.White,
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        val inset = 3.dp.toPx()

        drawLine(
            color = Color.Red,
            start = Offset(inset, inset),
            end = Offset(size.width - inset, size.height - inset),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
