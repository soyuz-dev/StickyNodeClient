
package org.stickynodes.editor.tools.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
internal fun StrokeWidthPicker(
    width: Float,
    onWidthChanged: (Float) -> Unit,
    onWidthChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("${width.toInt()} px")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
        ) {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Stroke width",
                    style = MaterialTheme.typography.titleSmall,
                )

                Text(
                    "${(width * 10).toInt() / 10f} units",
                    style = MaterialTheme.typography.bodySmall,
                )

                val activeColor = MaterialTheme.colorScheme.primary
                val inactiveColor =
                    MaterialTheme.colorScheme.primaryContainer

                Slider(
                    value = width.coerceIn(0.5f, 20f),
                    onValueChange = onWidthChanged,
                    onValueChangeFinished = onWidthChangeFinished,
                    valueRange = 0.5f..20f,
                    steps = 0,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    activeColor,
                                    CircleShape,
                                )
                        )
                    },
                    track = { state: SliderState ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .offset(y = (-2).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(inactiveColor)
                                    .drawWithContent {
                                        drawContent()

                                        val fraction = (
                                                (state.value - 0.5f) / 19.5f
                                                ).coerceIn(0f, 1f)

                                        drawRect(
                                            color = activeColor,
                                            size = Size(
                                                size.width * fraction,
                                                size.height,
                                            ),
                                        )
                                    }
                            )
                        }
                    },
                )
            }
        }
    }
}
