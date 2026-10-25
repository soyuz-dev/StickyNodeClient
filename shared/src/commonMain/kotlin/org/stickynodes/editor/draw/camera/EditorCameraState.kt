
package org.stickynodes.editor.draw.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.stickynodes.editor.draw.model.EditorNode

class EditorCameraState(
    private val scope: CoroutineScope,
    initialCamera: EditorCamera = EditorCamera(),
) {
    var camera by mutableStateOf(initialCamera)
        private set

    var viewportSize by mutableStateOf(IntSize.Zero)
        private set

    private var animationJob: Job? = null

    fun updateViewportSize(size: IntSize) {
        viewportSize = size
    }

    fun updateCamera(updated: EditorCamera) {
        animationJob?.cancel()
        animationJob = null
        camera = updated
    }

    fun zoomIn() {
        zoomTo(camera.zoom * 1.25f)
    }

    fun zoomOut() {
        zoomTo(camera.zoom / 1.25f)
    }

    fun resetZoom() {
        zoomTo(1f)
    }

    private fun zoomTo(zoom: Float) {
        updateCamera(
            camera.zoomAt(
                screenPoint = viewportCentre,
                newZoom = zoom,
            )
        )
    }

    fun fitToNodes(
        nodes: List<EditorNode>,
        padding: Float = 64f,
        animated: Boolean = true,
    ) {
        if (nodes.isEmpty() ||
            viewportSize.width <= 0 ||
            viewportSize.height <= 0
        ) {
            return
        }

        val target = camera.fitToNodes(
            nodes = nodes,
            viewport = viewportSize,
            padding = padding,
        )

        if (animated) {
            animateTo(target)
        } else {
            updateCamera(target)
        }
    }

    fun animateTo(
        target: EditorCamera,
        durationMillis: Int = 250,
    ) {
        animationJob?.cancel()

        val start = camera
        val screenCentre = viewportCentre

        val startCentre =
            start.position + screenCentre / start.zoom

        val targetCentre =
            target.position + screenCentre / target.zoom

        animationJob = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) { progress, _ ->
                val centre = startCentre +
                        (targetCentre - startCentre) * progress

                val zoom = start.zoom +
                        (target.zoom - start.zoom) * progress

                camera = EditorCamera(
                    position = centre - screenCentre / zoom,
                    zoom = zoom,
                )
            }

            camera = target
            animationJob = null
        }
    }

    private val viewportCentre: Offset
        get() = Offset(
            viewportSize.width / 2f,
            viewportSize.height / 2f,
        )
}

@Composable
fun rememberEditorCameraState(
    initialCamera: EditorCamera = EditorCamera(),
): EditorCameraState {
    val scope = rememberCoroutineScope()

    return remember(scope) {
        EditorCameraState(
            scope = scope,
            initialCamera = initialCamera,
        )
    }
}
