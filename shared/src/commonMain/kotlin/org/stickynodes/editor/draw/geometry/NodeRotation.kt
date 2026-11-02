package org.stickynodes.editor.draw.geometry

import org.stickynodes.editor.draw.model.EditorShape

fun EditorShape.rotatedTo(
    rotation:Float,
): EditorShape {
    require(rotation.isFinite()) {
        "Rotation must be finite"
    }

    if(
        this.rotation == rotation
    ) return this

    return withRotation(rotation)
}