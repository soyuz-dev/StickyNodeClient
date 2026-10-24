package org.stickynodes.editor.tools.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
                    .width(240.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                )

                palette.chunked(6).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        row.forEach { argb ->
                            StyleColorButton(
                                color = Color(argb),
                                onClick = {
                                    onColorChanged(Color(argb))
                                    expanded = false
                                },
                                description = "Select colour",
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onColorChanged(null)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("No colour")
                }
            }
        }
    }
}