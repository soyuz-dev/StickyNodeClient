package org.stickynodes

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.PathSampler
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun samplesAtFixedIntervals() {
        val sampler = PathSampler(8f)

        sampler.start(Offset.Zero)
        val points = sampler.finish(Offset(20f, 0f))

        assertEquals(
            listOf(
                Offset(0f, 0f),
                Offset(8f, 0f),
                Offset(16f, 0f),
                Offset(20f, 0f),
            ),
            points,
        )
    }

    @Test
    fun samplingIsIndependentOfEventFrequency() {
        val first = PathSampler(8f)
        first.start(Offset.Zero)
        val a = first.finish(Offset(20f, 0f))

        val second = PathSampler(8f)
        second.start(Offset.Zero)
        second.moveTo(Offset(3f, 0f))
        second.moveTo(Offset(9f, 0f))
        val b = second.finish(Offset(20f, 0f))

        assertEquals(a, b)
    }

    @Test
    fun shortStrokePreservesEndpoints() {
        val sampler = PathSampler(8f)

        sampler.start(Offset.Zero)
        val points = sampler.finish(Offset(3f, 2f))

        assertEquals(
            listOf(Offset.Zero, Offset(3f, 2f)),
            points,
        )
    }
}