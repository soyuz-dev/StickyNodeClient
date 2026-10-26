package org.stickynodes.editor.draw

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.stickynodes.editor.draw.camera.EditorCamera
import kotlin.math.roundToInt

@Composable
fun EditorTextOverlay(
    position: Offset,
    text: String,
    camera: EditorCamera,
    onTextChanged: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Float = 24f
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }

    val screenPosition = camera.worldToScreen(position)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onCommit) {
                detectTapGestures{
                    onCommit
                }
            },
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .offset {
                    IntOffset(
                        screenPosition.x.roundToInt(),
                        screenPosition.y.roundToInt(),
                    )
                }
                .widthIn(min = 80.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                color = Color(
                    0xFF0D0D0D
                ),
                fontSize = with(density) {
                    (fontSize * camera.zoom).toSp()
                },
            ),
            singleLine = false,
        )

        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }
    }
}