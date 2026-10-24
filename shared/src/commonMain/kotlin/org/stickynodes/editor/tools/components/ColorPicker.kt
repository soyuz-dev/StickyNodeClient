
package org.stickynodes.editor.tools.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.style.FillStyle

private val palette = listOf(
    0xFF52A6C4.toInt(),
    0xFF264B72.toInt(),
    0xFFE28C9F.toInt(),
    0xFF8C85CE.toInt(),
    0xFF68B68D.toInt(),
    0xFFE9BF69.toInt(),
    0xFFF28C52.toInt(),
    0xFF292D38.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFE84D4D.toInt(),
    0xFF19A7A0.toInt(),
    0xFFB88CE3.toInt(),
)

@Composable
internal fun ColorPicker(
    color: Color?,
    onColorChanged: (Color?) -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Choose colour",
) {
    var expanded by remember { mutableStateOf(false) }
    var hexInput by remember { mutableStateOf("") }

    LaunchedEffect(expanded) {
        if (expanded) {
            hexInput = color?.toHexRgb() ?: ""
        }
    }

    val parsedColor = parseHexColor(hexInput)

    fun applyHex() {
        parsedColor?.let {
            onColorChanged(it)
            expanded = false
        }
    }

    Box(modifier) {
        StyleColorButton(
            color = color,
            description = description,
            onClick = { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column(
                modifier = Modifier
                    .width(176.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    maxItemsInEachRow = 4,
                ) {
                    palette.forEach { argb ->
                        val selected = color?.toArgb() == argb

                        Surface(
                            modifier = Modifier.size(30.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                if (selected) 2.dp else 1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            onClick = {
                                onColorChanged(Color(argb))
                                expanded = false
                            },
                        ) {
                            ColorSwatch(
                                color = Color(argb),
                                modifier = Modifier.padding(3.dp),
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (hexInput.isNotEmpty() && parsedColor == null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                    ) {
                        BasicTextField(
                            value = hexInput,
                            onValueChange = { input ->
                                hexInput = input
                                    .removePrefix("#")
                                    .filter { it.isHexDigit() }
                                    .take(6)
                                    .uppercase()
                            },
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 8.dp,
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(
                                MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { applyHex() },
                            ),
                            decorationBox = { innerTextField ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "#",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (hexInput.isEmpty()) {
                                            Text(
                                                text = "RRGGBB",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                            )
                                        }

                                        innerTextField()
                                    }
                                }
                            },
                        )
                    }

                    Surface(
                        onClick = { applyHex() },
                        enabled = parsedColor != null,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (parsedColor != null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (parsedColor != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        onColorChanged(null)
                        expanded = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NoColorSwatch(
                            modifier = Modifier.size(20.dp),
                        )

                        Text(
                            text = "No colour",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' ||
            this in 'a'..'f' ||
            this in 'A'..'F'

private fun parseHexColor(text: String): Color? {
    if (text.length != 6 || !text.all { it.isHexDigit() }) {
        return null
    }

    val rgb = text.toLong(16)
    return Color((0xFF000000L or rgb).toInt())
}

private fun Color.toHexRgb(): String =
    (toArgb().toLong() and 0xFFFFFFL)
        .toString(16)
        .uppercase()
        .padStart(6, '0')
