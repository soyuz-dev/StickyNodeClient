
package org.stickynodes

import androidx.compose.ui.geometry.Offset
import org.stickynodes.editor.draw.geometry.PathSampler
import org.stickynodes.editor.draw.geometry.bezierSegments
import org.stickynodes.editor.draw.geometry.bounds
import org.stickynodes.editor.draw.geometry.containsStroke
import org.stickynodes.editor.draw.model.EditorEllipse
import org.stickynodes.editor.draw.model.EditorPath
import org.stickynodes.editor.draw.model.EditorRectangle
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.history.EditorDocument
import org.stickynodes.editor.history.EditorHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun `samples at fixed intervals`() {
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
    fun `sampling is independent of event frequency`() {
        val first = PathSampler(8f)
        first.start(Offset.Zero)
        val firstPoints = first.finish(Offset(20f, 0f))

        val second = PathSampler(8f)
        second.start(Offset.Zero)
        second.moveTo(Offset(3f, 0f))
        second.moveTo(Offset(9f, 0f))
        val secondPoints = second.finish(Offset(20f, 0f))

        assertEquals(firstPoints, secondPoints)
    }

    @Test
    fun `short stroke preserves endpoints`() {
        val sampler = PathSampler(8f)

        sampler.start(Offset.Zero)
        val points = sampler.finish(Offset(3f, 2f))

        assertEquals(
            listOf(Offset.Zero, Offset(3f, 2f)),
            points,
        )
    }

    private fun createPath(points: List<Offset>) = EditorPath(
        x = 100f,
        y = 200f,
        points = points,
        style = NodeStyle(
            fill = FillStyle.None,
            stroke = StrokeStyle(
                color = 0xFF000000.toInt(),
                width = 4f,
            ),
        ),
    )

    @Test
    fun `selects point near straight stroke`() {
        val node = createPath(
            listOf(
                Offset.Zero,
                Offset(100f, 0f),
            )
        )

        assertTrue(
            node.containsStroke(Offset(150f, 203f))
        )

        assertFalse(
            node.containsStroke(Offset(150f, 220f))
        )
    }

    @Test
    fun `respects path translation`() {
        val node = createPath(
            listOf(
                Offset.Zero,
                Offset(100f, 0f),
            )
        )

        assertTrue(
            node.containsStroke(Offset(150f, 200f))
        )

        assertFalse(
            node.containsStroke(Offset(50f, 100f))
        )
    }

    @Test
    fun `hit tolerance depends on zoom`() {
        val node = createPath(
            listOf(
                Offset.Zero,
                Offset(100f, 0f),
            )
        )

        val pointer = Offset(150f, 205f)

        assertTrue(node.containsStroke(pointer, zoom = 1f))
        assertFalse(node.containsStroke(pointer, zoom = 4f))
    }

    @Test
    fun `single point stroke is selectable`() {
        val node = createPath(listOf(Offset.Zero))

        assertTrue(
            node.containsStroke(Offset(102f, 201f))
        )
    }


    @Test
    fun `path bounds contain all bezier control points`() {
        val node = createPath(
            listOf(
                Offset(0f, 0f),
                Offset(30f, 60f),
                Offset(60f, -20f),
                Offset(100f, 40f),
            )
        )

        val origin = Offset(node.x, node.y)

        for ((start, control1, control2, end) in bezierSegments(node.points)) {
            val controlPoints = listOf(
                start,
                control1,
                control2,
                end,
            )

            for (point in controlPoints) {
                assertTrue(node.bounds.contains(origin + point))
            }
        }
    }

    @Test
    fun `path bounds account for stroke width`() {
        val node = createPath(
            listOf(
                Offset.Zero,
                Offset(100f, 0f),
            )
        )

        // Test stroke width is 4, giving a radius of 2.
        assertEquals(98f, node.bounds.left)
        assertEquals(198f, node.bounds.top)
        assertEquals(202f, node.bounds.bottom)
        assertEquals(202f, node.bounds.right)
    }


    @Test
    fun `committing a document creates an undo entry`() {
        val initial = EditorHistory()
        val document = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                )
            )
        )

        val updated = initial.commit(document)

        assertEquals(document, updated.document)
        assertTrue(updated.canUndo)
        assertFalse(updated.canRedo)
    }

    @Test
    fun `undo restores the previous document`() {
        val initial = EditorHistory()
        val document = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                )
            )
        )

        val updated = initial.commit(document).undo()

        assertEquals(initial.document, updated.document)
        assertTrue(updated.canRedo)
    }

    @Test
    fun `redo restores an undone document`() {
        val initial = EditorHistory()
        val document = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                )
            )
        )

        val updated = initial
            .commit(document)
            .undo()
            .redo()

        assertEquals(document, updated.document)
        assertFalse(updated.canRedo)
    }

    @Test
    fun `new commits clear redo history`() {
        val first = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 0f,
                    y = 0f,
                    width = 50f,
                    height = 50f,
                )
            )
        )

        val second = EditorDocument(
            nodes = listOf(
                EditorEllipse(
                    x = 10f,
                    y = 10f,
                    width = 50f,
                    height = 50f,
                )
            )
        )

        val history = EditorHistory()
            .commit(first)
            .undo()
            .commit(second)

        assertFalse(history.canRedo)
        assertEquals(second, history.document)
    }

    @Test
    fun `identical commits do not create history entries`() {
        val history = EditorHistory()
        val updated = history.commit(history.document)

        assertEquals(history, updated)
        assertFalse(updated.canUndo)
    }

}
