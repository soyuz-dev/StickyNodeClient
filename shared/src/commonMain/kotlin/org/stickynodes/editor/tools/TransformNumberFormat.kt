package org.stickynodes.editor.tools

internal fun Float.toTransformString() : String {
    if(!isFinite()) return toString()

    val rounded = toInt()

    if(this == rounded.toFloat()) {
        return rounded.toString()
    }

    return toString()
}