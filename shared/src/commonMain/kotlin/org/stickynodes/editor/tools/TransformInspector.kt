
package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun TransformInspector(
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    dimensions: Size?,
    onDimensionsChanged: (Size) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Position",
                style = MaterialTheme.typography.labelLarge,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CoordinateField(
                    label = "X",
                    value = position.x,
                    onCommit = { x ->
                        onPositionChanged(
                            Offset(x, position.y)
                        )
                    },
                )

                CoordinateField(
                    label = "Y",
                    value = position.y,
                    onCommit = { y ->
                        onPositionChanged(
                            Offset(position.x, y)
                        )
                    },
                )
            }

            if (dimensions!=null) {
                Text(
                    text = "Dimensions",
                    style = MaterialTheme.typography.labelLarge,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoordinateField(
                        label = "W",
                        value = dimensions.width,
                        onCommit = { width ->
                            if( width > 0f ) {
                                onDimensionsChanged(
                                    dimensions.copy(width = width)
                                )
                            }
                        },
                        isValid = { it > 0f }
                    )

                    CoordinateField(
                        label = "H",
                        value = dimensions.height,
                        onCommit = { height ->
                            if( height > 0f ) {
                                onDimensionsChanged(
                                    dimensions.copy(height = height)
                                )
                            }
                        },
                        isValid = { it > 0f }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoordinateField(
    label: String,
    value: Float,
    onCommit: (Float) -> Unit,
    isValid: (Float) -> Boolean = { true },
) {
    var draft by remember(value) {
        mutableStateOf(value.toString())
    }

    var wasFocused by remember {
        mutableStateOf(false)
    }

    val focusManager = LocalFocusManager.current

    fun commit() {
        val parsed = draft.toFloatOrNull()
            ?.takeIf { it.isFinite() && isValid(it) }

        if (parsed != null && parsed != value) {
            onCommit(parsed)
        } else {
            draft = value.toString()
        }
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .width(110.dp)
            .onFocusChanged { state ->
                if (wasFocused && !state.isFocused) {
                    commit()
                }
                wasFocused = state.isFocused
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                commit()
                focusManager.clearFocus()
            }
        ),
    )
}

@Preview
@Composable
private fun TransformInspectorInteractivePreview() {
    var position by remember {
        mutableStateOf(Offset(200f, 150f))
    }

    var dimensions by remember {
        mutableStateOf(Size(20f, 10f))
    }

    MaterialTheme {
        TransformInspector(
            position = position,
            onPositionChanged = { position = it },
            dimensions = dimensions,
            onDimensionsChanged = { dimensions = it }
        )
    }
}