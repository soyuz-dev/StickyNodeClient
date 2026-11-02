
package org.stickynodes.editor.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI

@Composable
fun TransformInspector(
    position: Offset,
    onPositionChanged: (Offset) -> Unit,
    dimensions: Size?,
    onDimensionsChanged: (Size) -> Unit,
    rotation: Float?,
    onRotationChanged: (Float) -> Unit,
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

            if (dimensions != null) {
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
                            onDimensionsChanged(
                                dimensions.copy(width = width)
                            )
                        },
                        isValid = { it > 0f },
                    )

                    CoordinateField(
                        label = "H",
                        value = dimensions.height,
                        onCommit = { height ->
                            onDimensionsChanged(
                                dimensions.copy(height = height)
                            )
                        },
                        isValid = { it > 0f },
                    )
                }
            }

            if (rotation != null) {
                Text(
                    text = "Rotation",
                    style = MaterialTheme.typography.labelLarge,
                )

                CoordinateField(
                    label = "°",
                    value = rotation * (180f / PI.toFloat()),
                    onCommit = { degrees ->
                        onRotationChanged(
                            degrees * (PI.toFloat() / 180f)
                        )
                    },
                )
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
    var draft by remember {
        mutableStateOf(value.toTransformString())
    }

    var wasFocused by remember {
        mutableStateOf(false)
    }

    var finalized by remember {
        mutableStateOf(false)
    }

    val focusManager = LocalFocusManager.current

    // Keep the displayed value synchronized with the model
    // whenever the field is not being edited.
    if (!wasFocused) {
        val formatted = value.toTransformString()

        if (draft != formatted) {
            draft = formatted
        }
    }

    fun commit() {
        val parsed = draft.toFloatOrNull()
            ?.takeIf { it.isFinite() && isValid(it) }

        if (parsed != null && parsed != value) {
            onCommit(parsed)
        } else {
            draft = value.toTransformString()
        }
    }

    fun finish(commitChanges: Boolean) {
        if (finalized) return

        finalized = true

        if (commitChanges) {
            commit()
        } else {
            draft = value.toTransformString()
        }
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .width(110.dp)
            .onPreviewKeyEvent { event ->
                if (
                    event.key == Key.Escape &&
                    event.type == KeyEventType.KeyDown
                ) {
                    finish(commitChanges = false)
                    focusManager.clearFocus()
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { state ->
                if (state.isFocused && !wasFocused) {
                    finalized = false
                }

                if (wasFocused && !state.isFocused) {
                    finish(commitChanges = true)
                }

                wasFocused = state.isFocused
            },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                finish(commitChanges = true)
                focusManager.clearFocus()
            },
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

    var rotation by remember {
        mutableStateOf(20f)
    }

    MaterialTheme {
        TransformInspector(
            position = position,
            onPositionChanged = { position = it },
            dimensions = dimensions,
            onDimensionsChanged = { dimensions = it },
            rotation = rotation,
            onRotationChanged = { rotation = it },
        )
    }
}
