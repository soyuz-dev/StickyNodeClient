package org.stickynodes.editor.draw.geometry.transform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.combinedVisualBounds
import org.stickynodes.editor.draw.model.EditorShape

fun List<EditorShape>.resizeHandleScreenPositions(
    camera: EditorCamera,
    padding: Float,
): Map<ResizeHandle, Offset> {
    val bounds = if (size == 1) {
        single().bounds
    } else {
        combinedVisualBounds()
    } ?: return emptyMap()

    val screenBounds = Rect(
        camera.worldToScreen(bounds.topLeft),
        camera.worldToScreen(
            Offset(bounds.right, bounds.bottom),
        ),
    ).inflate(padding)

    val positions = screenBounds.resizeHandlePositions()

    val shape = singleOrNull() ?: return positions

    return positions.mapValues { (_, position) ->
        position.rotatedAround(
            center = camera.worldToScreen(shape.bounds.center),
            angle = shape.rotation,
        )
    }
}

fun Rect.orientedResizeHandleScreenPositions(
    rotation: Float,
    camera: EditorCamera,
    padding: Float,
): Map<ResizeHandle, Offset> {
    require(rotation.isFinite()) {
        "Rotation must be finite"
    }
    require(padding.isFinite() && padding >= 0f) {
        "Padding must be finite and non-negative"
    }
    require(camera.zoom.isFinite() && camera.zoom > 0f) {
        "Camera zoom must be finite and positive"
    }

    val localBounds = inflate(padding / camera.zoom)

    return localBounds.resizeHandlePositions().mapValues {
            (_, localPosition) ->
        camera.worldToScreen(
            localPosition.rotatedAround(
                center = Offset.Zero,
                angle = rotation,
            )
        )
    }
}

fun Rect.orientedResizeHandleAt(
    pointer: Offset,
    rotation: Float,
    camera: EditorCamera,
    padding: Float,
    radius: Float,
): ResizeHandle? {
    require(radius.isFinite() && radius > 0f) {
        "Hit radius must be finite and positive"
    }

    val radiusSquared = radius * radius

    return orientedResizeHandleScreenPositions(
        rotation = rotation,
        camera = camera,
        padding = padding,
    ).entries.firstOrNull { (_, position) ->
        (pointer - position).getDistanceSquared() <= radiusSquared
    }?.key
}

fun List<EditorShape>.rotationHandleScreenPosition(
    camera: EditorCamera,
    offset: Float,
): Offset? {
    if (isEmpty()) return null

    val singleShape = singleOrNull()

    if (singleShape != null) {
        return singleShape.rotationHandlePosition(
            camera = camera,
            offset = offset,
        )
    }

    val bounds = combinedVisualBounds() ?: return null

    return camera.worldToScreen(
        Offset(
            x = bounds.center.x,
            y = bounds.top,
        )
    ) - Offset(0f, offset)
}

fun List<EditorShape>.resizeHandleAt(
    pointer: Offset,
    camera: EditorCamera,
    padding: Float,
    radius: Float,
): ResizeHandle? {
    val radiusSquared = radius * radius

    return resizeHandleScreenPositions(
        camera = camera,
        padding = padding,
    ).entries.firstOrNull { (_, position) ->
        (pointer - position).getDistanceSquared() <= radiusSquared
    }?.key
}

fun List<EditorShape>.isRotationHandleHit(
    pointer: Offset,
    camera: EditorCamera,
    offset: Float,
    radius: Float,
): Boolean {
    val position = rotationHandleScreenPosition(
        camera = camera,
        offset = offset,
    ) ?: return false

    return (pointer - position).getDistanceSquared() <=
            radius * radius
}