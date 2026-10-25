


package org.stickynodes.editor.draw.camera

import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.ui.geometry.Offset

val EditorCameraVectorConverter = TwoWayConverter<EditorCamera, AnimationVector3D>(
    convertToVector = { camera ->
        AnimationVector3D(
            camera.position.x,
            camera.position.y,
            camera.zoom,
        )
    },
    convertFromVector = { vector ->
        EditorCamera(
            position = Offset(vector.v1, vector.v2),
            zoom = vector.v3,
        )
    },
)
