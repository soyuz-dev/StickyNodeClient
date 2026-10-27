package org.stickynodes.editor.draw.geometry

enum class ResizeHandle(
    val movesLeft: Boolean = false,
    val movesTop: Boolean = false,
    val movesRight: Boolean = false,
    val movesBottom: Boolean = false,
) {
    NW(movesLeft = true, movesTop = true),
    N(movesTop = true),
    NE(movesTop = true, movesRight = true),
    E(movesRight = true),
    SE(movesRight = true, movesBottom = true),
    S(movesBottom = true),
    SW(movesLeft = true, movesBottom = true),
    W(movesLeft = true),
}