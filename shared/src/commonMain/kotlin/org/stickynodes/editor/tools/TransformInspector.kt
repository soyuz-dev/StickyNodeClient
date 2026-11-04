
package org.stickynodes.editor.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
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
import dev.vicart.compose.material.symbols.MaterialSymbol
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
    onEditingChanged: (Boolean) -> Unit = {},
    readOnly: Boolean = false,
    positionReadOnly: Boolean = readOnly,
    dimensionsReadOnly: Boolean = readOnly,
    rotationReadOnly: Boolean = readOnly,
    rotationLabel: String = "°",
) {

    val currentOnEditingChanged by rememberUpdatedState(onEditingChanged)

    DisposableEffect(Unit) {
        onDispose {
            currentOnEditingChanged(false)
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .onFocusChanged { state ->
                    onEditingChanged(state.hasFocus)
                }
                .focusGroup()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransformIcon("open_with")

                CoordinateField(
                    label = "X",
                    value = position.x,
                    onCommit = { x ->
                        onPositionChanged(
                            Offset(x, position.y)
                        )
                    },
                    readOnly = positionReadOnly,
                )

                CoordinateField(
                    label = "Y",
                    value = position.y,
                    onCommit = { y ->
                        onPositionChanged(
                            Offset(position.x, y)
                        )
                    },
                    readOnly = positionReadOnly,
                )
            }

            if (dimensions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransformIcon("aspect_ratio")

                    CoordinateField(
                        label = "W",
                        value = dimensions.width,
                        onCommit = { width ->
                            onDimensionsChanged(
                                dimensions.copy(width = width)
                            )
                        },
                        isValid = { it > 0f },
                        readOnly = dimensionsReadOnly,
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
                        readOnly = dimensionsReadOnly,
                    )
                }
            }

            if (rotation != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransformIcon("rotate_right")

                    CoordinateField(
                        label = rotationLabel,
                        value = rotation * (180f / PI.toFloat()),
                        onCommit = { degrees ->
                            onRotationChanged(
                                degrees * (PI.toFloat() / 180f)
                            )
                        },
                        readOnly = rotationReadOnly,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransformIcon(icon: String) {
    MaterialSymbol.Outlined(
        icon = icon,
        size = 18.dp,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CoordinateField(
    label: String,
    value: Float,
    onCommit: (Float) -> Unit,
    isValid: (Float) -> Boolean = { true },
    readOnly: Boolean = false,
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

    // Synchronize with the model when not editing.
    if (!wasFocused) {
        val formatted = value.toTransformString()

        if (draft != formatted) {
            draft = formatted
        }
    }

    fun commit() {
        if (readOnly) return

        val formatted = value.toTransformString()

        if (draft == formatted) return

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


    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .width(90.dp)
            .height(32.dp)
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
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    innerTextField()
                }
            }
        },
        readOnly = readOnly,
    )
}

@Preview
@Composable
private fun TransformInspectorPreview() {
    var position by remember {
        mutableStateOf(Offset(200f, 150f))
    }

    var dimensions by remember {
        mutableStateOf(Size(20f, 10f))
    }

    var rotation by remember {
        mutableStateOf(1f)
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
