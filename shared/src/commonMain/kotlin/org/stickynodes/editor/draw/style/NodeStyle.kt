package org.stickynodes.editor.draw.style

data class NodeStyle(
    val fill: FillStyle = FillStyle.Solid(0xFF52A6C4.toInt()),
    val stroke: StrokeStyle? = StrokeStyle(
        color = 0xFF264B72.toInt()
    ),
)