package org.stickynodes.editor.draw.style

data class StrokeStyle(
    val color: Int,
    val width: Float = 2f,
    val cap: StrokeCap = StrokeCap.Round,
    val join: StrokeJoin = StrokeJoin.Round,
    val dashPattern: List<Float>? = null,
)

enum class StrokeCap {
    Round,
    Square
}

enum class StrokeJoin {
    Round,
    Bevel
}