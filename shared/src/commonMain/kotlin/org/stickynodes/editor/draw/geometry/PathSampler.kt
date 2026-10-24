package org.stickynodes.editor.draw.geometry

import androidx.compose.ui.geometry.Offset

class PathSampler(
    private val spacing: Float = 12f,
) {
    init {
        require(spacing > 0f && spacing.isFinite())
    }

    private val samples = mutableListOf<Offset>()
    private var previousPosition: Offset? = null
    private var distanceSinceSample = 0f


    val points: List<Offset>
        get() = samples.toList()

    fun start(position: Offset) {
        samples.clear()
        samples.add(position)

        previousPosition = position
        distanceSinceSample = 0f
    }

    fun moveTo(position: Offset) {
        var start = previousPosition?: return
        val delta = position - start
        val length = delta.getDistance()

        if(length == 0f) {
            previousPosition = position
            return
        }

        val direction = delta/length
        var remaining = length

        while (distanceSinceSample + remaining >= spacing) {
            val distanceToNext = spacing - distanceSinceSample

            start += direction * distanceToNext
            samples.add(start)

            remaining -= distanceToNext
            distanceSinceSample = 0f
        }

        distanceSinceSample += remaining
        previousPosition = position
    }

    fun finish(position: Offset): List<Offset> {
        moveTo(position)
        if(samples.isEmpty()) start(position)
        else if(samples.last() != position) samples.add(position)

        return samples.toList()
    }
}