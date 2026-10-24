
package org.stickynodes.editor.tools.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
                    .width(176.dp)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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

                Surface(
                    onClick = {
                        onColorChanged(null)
                        expanded = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        NoColorSwatch(
                            modifier = Modifier.size(20.dp)
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
