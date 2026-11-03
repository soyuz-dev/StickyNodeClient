package org.stickynodes.editor.tools

import kotlin.math.round

internal fun Float.toTransformString() : String {
    if (!isFinite()) return toString()

    val rounded = round(toDouble() * 100.0) / 100.0

    if (rounded == 0.0) return "0"

    return rounded.toString().removeSuffix(".0")
}