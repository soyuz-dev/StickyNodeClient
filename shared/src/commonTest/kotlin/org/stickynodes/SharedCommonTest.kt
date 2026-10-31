package org.stickynodes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.stickynodes.data.DocumentCodec
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.*
import org.stickynodes.editor.draw.model.*
import org.stickynodes.editor.draw.style.*
import org.stickynodes.editor.history.*
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

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

    @Test
    fun `deleting a node can be undone`() {
        val node = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val original = EditorDocument(nodes = listOf(node))

        val history = EditorHistory(document = original)
            .commit(EditorDocument())
            .undo()

        assertEquals(original, history.document)
    }

    @Test
    fun `redo reapplies node deletion`() {
        val node = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val original = EditorDocument(nodes = listOf(node))

        val history = EditorHistory(document = original)
            .commit(EditorDocument())
            .undo()
            .redo()

        assertTrue(history.document.nodes.isEmpty())
    }


    @Test
    fun `document survives JSON round trip`() {
        val original = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                ),
                EditorEllipse(
                    x = 150f,
                    y = 40f,
                    width = 80f,
                    height = 80f,
                ),
                EditorPath(
                    x = 200f,
                    y = 120f,
                    points = listOf(
                        Offset.Zero,
                        Offset(12f, 8f),
                        Offset(24f, -5f),
                    ),
                ),
            ),
        )

        val encoded = DocumentCodec.encode(original)
        val restored = DocumentCodec.decode(encoded)

        assertEquals(original, restored)
    }

    @Test
    fun `empty document survives JSON round trip`() {
        val original = EditorDocument()

        assertEquals(
            original,
            DocumentCodec.decode(
                DocumentCodec.encode(original)
            ),
        )
    }

    private val original = Rect(
        left = 10f,
        top = 20f,
        right = 110f,
        bottom = 80f,
    )

    @Test
    fun `southeast handle resizes right and bottom edges`() {
        val result = resizeBounds(
            original,
            ResizeHandle.SE,
            Offset(30f, 20f),
        )

        assertEquals(Rect(10f, 20f, 140f, 100f), result)
    }

    @Test
    fun `northwest handle resizes left and top edges`() {
        val result = resizeBounds(
            original,
            ResizeHandle.NW,
            Offset(15f, 10f),
        )

        assertEquals(Rect(25f, 30f, 110f, 80f), result)
    }

    @Test
    fun `east handle preserves vertical bounds`() {
        val result = resizeBounds(
            original,
            ResizeHandle.E,
            Offset(25f, 100f),
        )

        assertEquals(Rect(10f, 20f, 135f, 80f), result)
    }

    @Test
    fun `resize cannot cross opposite corner`() {
        val result = resizeBounds(
            original,
            ResizeHandle.NW,
            Offset(200f, 200f),
            minimumSize = 5f,
        )

        assertEquals(Rect(105f, 75f, 110f, 80f), result)
    }

    @Test
    fun `zero resize delta preserves bounds`() {
        for (handle in ResizeHandle.entries) {
            assertEquals(
                original,
                resizeBounds(original, handle, Offset.Zero),
            )
        }
    }



    @Test
    fun `southeast proportional resize preserves aspect ratio`() {
        val result = resizeBoundsProportionally(
            original,
            ResizeHandle.SE,
            Offset(50f, 25f),
        )

        assertEquals(10f, result.left)
        assertEquals(20f, result.top)
        assertEquals(157.794f, result.right, 0.001f)
        assertEquals(108.676f, result.bottom, 0.001f)

        assertEquals(
            original.width / original.height,
            result.width / result.height,
            0.0001f,
        )
    }


    @Test
    fun `northwest proportional resize preserves aspect ratio`() {
        val result = resizeBoundsProportionally(
            original,
            ResizeHandle.NW,
            Offset(20f, 12f),
        )

        assertEquals(Rect(30f, 32f, 110f, 80f), result)
    }

    @Test
    fun `northeast proportional resize preserves aspect ratio`() {
        val result = resizeBoundsProportionally(
            original,
            ResizeHandle.NE,
            Offset(50f, -30f),
        )

        assertEquals(Rect(10f, -10f, 160f, 80f), result)
    }

    @Test
    fun `southwest proportional resize preserves aspect ratio`() {
        val result = resizeBoundsProportionally(
            original,
            ResizeHandle.SW,
            Offset(-50f, 30f),
        )

        assertEquals(Rect(-40f, 20f, 110f, 110f), result)
    }

    @Test
    fun `proportional resize cannot collapse below minimum size`() {
        val result = resizeBoundsProportionally(
            original,
            ResizeHandle.SE,
            Offset(-500f, -500f),
            minimumSize = 5f,
        )

        assertEquals(5f, result.height)
        assertEquals(5f * original.width / original.height, result.width, 0.0001f)
    }

    @Test
    fun `zero proportional resize delta preserves bounds`() {
        for (handle in listOf(
            ResizeHandle.NW,
            ResizeHandle.NE,
            ResizeHandle.SE,
            ResizeHandle.SW,
        )) {
            assertEquals(
                original,
                resizeBoundsProportionally(
                    original,
                    handle,
                    Offset.Zero,
                )
            )
        }
    }

    val a = EditorRectangle(
        x = 0f, y = 0f,
        width = 20f, height = 20f,
    )

    val b = EditorEllipse(
        x = 40f, y = 30f,
        width = 20f, height = 20f,
    )

    val shapes = listOf<EditorShape>(a, b)


    @Test
    fun `group bounds enclose all shapes`() {
        val result = shapes.combinedBounds()

        assertEquals(
            Rect(0f, 0f, 60f, 50f),
            result,
        )
    }

    @Test
    fun `group resize scales positions and dimensions`() {
        val result = shapes.resizedTogether(
            handle = ResizeHandle.SE,
            delta = Offset(60f, 50f),
        )

        assertEquals(
            Rect(0f, 0f, 40f, 40f),
            result[0].bounds,
        )

        assertEquals(
            Rect(80f, 60f, 120f, 100f),
            result[1].bounds,
        )
    }

    @Test
    fun `zero group resize delta preserves all shapes`() {
        val result = shapes.resizedTogether(
            handle = ResizeHandle.NW,
            delta = Offset.Zero,
        )

        assertEquals(shapes, result)
    }

    @Test
    fun `empty selection has no combined bounds`() {
        assertNull(emptyList<EditorShape>().combinedBounds())
    }

    @Test
    fun `rotated rectangle contains point outside original bounds`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 20f,
            rotation = (PI / 2).toFloat(),
        )

        // Rectangle rotates around (50, 10).
        // It now spans roughly x=40..60, y=-40..60.
        assertTrue(
            rectangle.contains(Offset(50f, -20f))
        )
    }

    @Test
    fun `rotated rectangle rejects point inside original bounds`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 20f,
            rotation = (PI / 2).toFloat(),
        )

        // Inside original bounds, but outside rotated geometry.
        assertFalse(
            rectangle.contains(Offset(5f, 10f))
        )
    }


    @Test
    fun `zero rotation uses existing resize behaviour`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 60f,
        )

        val resized = rectangle.resizedRotatedBy(
            handle = ResizeHandle.SE,
            delta = Offset(30f, 20f),
        )

        assertEquals(
            Rect(10f, 20f, 140f, 100f),
            resized.bounds,
        )
    }

    @Test
    fun `rotated east handle follows local horizontal axis`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 40f,
            rotation = (PI / 2).toFloat(),
        )

        val resized = rectangle.resizedRotatedBy(
            handle = ResizeHandle.E,
            delta = Offset(0f, 30f),
        )

        assertEquals(130f, resized.width, 0.0001f)
        assertEquals(40f, resized.height, 0.0001f)

        // The centre moves 15 units downward,
        // because the opposite edge remains fixed.
        assertEquals(50f, resized.bounds.center.x, 0.0001f)
        assertEquals(35f, resized.bounds.center.y, 0.0001f)
    }

    @Test
    fun `rotated southeast resize preserves northwest corner`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 40f,
            rotation = (PI / 4).toFloat(),
        )

        fun northwestCorner(shape: EditorShape): Offset =
            shape.bounds.topLeft.rotatedAround(
                center = shape.bounds.center,
                angle = shape.rotation,
            )

        val originalCorner = northwestCorner(rectangle)

        val resized = rectangle.resizedRotatedBy(
            handle = ResizeHandle.SE,
            delta = Offset(30f, 20f),
        )

        val resizedCorner = northwestCorner(resized)

        assertEquals(originalCorner.x, resizedCorner.x, 0.0001f)
        assertEquals(originalCorner.y, resizedCorner.y, 0.0001f)
    }

    @Test
    fun `rotated proportional resize preserves aspect ratio`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
            rotation = (PI / 3).toFloat(),
        )

        val resized = rectangle.resizedRotatedBy(
            handle = ResizeHandle.SE,
            delta = Offset(30f, 40f),
            proportional = true,
        )

        assertEquals(
            rectangle.width / rectangle.height,
            resized.width / resized.height,
            0.0001f,
        )
    }

    @Test
    fun `zero delta preserves rotated shape`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
            rotation = (PI / 3).toFloat(),
        )

        for (handle in ResizeHandle.entries) {
            val resized = rectangle.resizedRotatedBy(
                handle = handle,
                delta = Offset.Zero,
            )

            assertEquals(rectangle, resized)
        }
    }


    @Test
    fun `unrotated visual bounds equal logical bounds`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 40f,
        )

        assertEquals(rectangle.bounds, rectangle.visualBounds)
    }

    @Test
    fun `ninety degree rotation swaps visual dimensions`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 40f,
            rotation = (PI / 2).toFloat(),
        )

        val visual = rectangle.visualBounds

        assertEquals(40f, visual.width, 0.0001f)
        assertEquals(100f, visual.height, 0.0001f)
        assertEquals(rectangle.bounds.center.x, visual.center.x, 0.0001f)
        assertEquals(rectangle.bounds.center.y, visual.center.y, 0.0001f)
    }

    @Test
    fun `combined visual bounds enclose rotated shapes`() {
        val shapes = listOf<EditorShape>(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 100f,
                height = 20f,
                rotation = (PI / 2).toFloat(),
            ),
            EditorEllipse(
                x = 120f,
                y = 0f,
                width = 40f,
                height = 40f,
            ),
        )

        val combined = shapes.combinedVisualBounds()!!

        for (shape in shapes) {
            val visual = shape.visualBounds

            assertTrue(combined.left <= visual.left)
            assertTrue(combined.top <= visual.top)
            assertTrue(combined.right >= visual.right)
            assertTrue(combined.bottom >= visual.bottom)
        }
    }

    @Test
    fun `empty selection has no combined visual bounds`() {
        assertNull(
            emptyList<EditorShape>().combinedVisualBounds()
        )
    }


    @Test
    fun `document preserves shape rotations through JSON round trip`() {
        val original = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                    rotation = (PI / 4).toFloat(),
                ),
                EditorEllipse(
                    x = 150f,
                    y = 40f,
                    width = 80f,
                    height = 60f,
                    rotation = (PI / 2).toFloat(),
                ),
            ),
        )

        val restored = DocumentCodec.decode(
            DocumentCodec.encode(original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun `legacy documents without rotation remain readable`() {
        val original = EditorDocument(
            nodes = listOf(
                EditorRectangle(
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                ),
            ),
        )

        val encoded = DocumentCodec.encode(original)

        // Simulate a document created before rotation existed.
        val legacyJson = encoded.replace(
            Regex(""","rotation"\s*:\s*[-\d.Ee+]+"""),
            "",
        )

        val restored = DocumentCodec.decode(legacyJson)

        assertEquals(original, restored)
    }


    @Test
    fun `unrotated shape has eight resize handles`() {
        val shape = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 60f,
        )

        val positions = listOf<EditorShape>(shape)
            .resizeHandleScreenPositions(
                camera = EditorCamera(),
                padding = 8f,
            )

        assertEquals(ResizeHandle.entries.toSet(), positions.keys)
        assertEquals(8, positions.size)
    }

    @Test
    fun `rotated resize handle follows shape orientation`() {
        val shape = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 40f,
            rotation = (PI / 2).toFloat(),
        )

        val positions = listOf<EditorShape>(shape)
            .resizeHandleScreenPositions(
                camera = EditorCamera(),
                padding = 0f,
            )

        val east = positions.getValue(ResizeHandle.E)

        assertEquals(50f, east.x, 0.0001f)
        assertEquals(70f, east.y, 0.0001f)
    }

    @Test
    fun `resize hit testing matches rendered handle position`() {
        val shapes = listOf<EditorShape>(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 100f,
                height = 40f,
                rotation = (PI / 4).toFloat(),
            ),
        )

        val camera = EditorCamera()
        val padding = 8f

        val handlePosition = shapes.resizeHandleScreenPositions(
            camera = camera,
            padding = padding,
        ).getValue(ResizeHandle.SE)

        assertEquals(
            ResizeHandle.SE,
            shapes.resizeHandleAt(
                pointer = handlePosition,
                camera = camera,
                padding = padding,
                radius = 10f,
            ),
        )
    }

    @Test
    fun `group rotation handle lies above visual bounds`() {
        val shapes = listOf<EditorShape>(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 100f,
                height = 20f,
                rotation = (PI / 2).toFloat(),
            ),
            EditorEllipse(
                x = 150f,
                y = 30f,
                width = 40f,
                height = 40f,
            ),
        )

        val visualBounds = shapes.combinedVisualBounds()!!

        val handle = shapes.rotationHandleScreenPosition(
            camera = EditorCamera(),
            offset = 28f,
        )!!

        assertEquals(visualBounds.center.x, handle.x, 0.0001f)
        assertEquals(visualBounds.top - 28f, handle.y, 0.0001f)
    }

    @Test
    fun `rotation handle hit testing rejects distant pointer`() {
        val shapes = listOf<EditorShape>(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 100f,
                height = 40f,
            )
        )

        assertFalse(
            shapes.isRotationHandleHit(
                pointer = Offset(500f, 500f),
                camera = EditorCamera(),
                offset = 28f,
                radius = 12f,
            )
        )
    }



    @Test
    fun `selection bounds include rotated rectangle corners`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 20f,
            rotation = (PI / 2).toFloat(),
        )

        val bounds = rectangle.selectionBounds

        assertEquals(40f, bounds.left, 0.001f)
        assertEquals(-40f, bounds.top, 0.001f)
        assertEquals(60f, bounds.right, 0.001f)
        assertEquals(60f, bounds.bottom, 0.001f)
    }

    @Test
    fun `selection bounds match visual bounds for rotated shapes`() {
        val ellipse = EditorEllipse(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
            rotation = (PI / 4).toFloat(),
        )

        assertEquals(ellipse.visualBounds, ellipse.selectionBounds)
    }



    @Test
    fun `fit to nodes contains rotated rectangle`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 200f,
            height = 40f,
            rotation = (PI / 4).toFloat(),
        )

        val camera = EditorCamera().fitToNodes(
            nodes = listOf(rectangle),
            viewport = IntSize(400, 400),
            padding = 32f,
        )

        val bounds = rectangle.selectionBounds

        val topLeft = camera.worldToScreen(bounds.topLeft)
        val bottomRight = camera.worldToScreen(bounds.bottomRight)

        assertTrue(topLeft.x >= 32f - 0.001f)
        assertTrue(topLeft.y >= 32f - 0.001f)
        assertTrue(bottomRight.x <= 368f + 0.001f)
        assertTrue(bottomRight.y <= 368f + 0.001f)
    }

    @Test
    fun `fit to nodes contains multiple rotated shapes`() {
        val shapes = listOf(
            EditorRectangle(
                x = -100f,
                y = 0f,
                width = 100f,
                height = 40f,
                rotation = (PI / 3).toFloat(),
            ),
            EditorEllipse(
                x = 200f,
                y = 150f,
                width = 120f,
                height = 80f,
                rotation = (PI / 6).toFloat(),
            ),
        )

        val camera = EditorCamera().fitToNodes(
            nodes = shapes,
            viewport = IntSize(600, 400),
            padding = 24f,
        )

        for (shape in shapes) {
            val bounds = shape.selectionBounds

            val topLeft = camera.worldToScreen(bounds.topLeft)
            val bottomRight = camera.worldToScreen(bounds.bottomRight)

            assertTrue(topLeft.x >= 24f - 0.001f)
            assertTrue(topLeft.y >= 24f - 0.001f)
            assertTrue(bottomRight.x <= 576f + 0.001f)
            assertTrue(bottomRight.y <= 376f + 0.001f)
        }
    }



    @Test
    fun `duplicating rotated rectangle preserves geometry and style`() {
        val original = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 60f,
            rotation = (PI / 4).toFloat(),
        )

        val newId = Uuid.random()

        val duplicate = original.duplicatedBy(
            offset = Offset(20f, 20f),
            newId = newId,
        ) as EditorRectangle

        assertEquals(newId, duplicate.id)
        assertEquals(30f, duplicate.x)
        assertEquals(40f, duplicate.y)
        assertEquals(original.width, duplicate.width)
        assertEquals(original.height, duplicate.height)
        assertEquals(original.rotation, duplicate.rotation)
        assertEquals(original.style, duplicate.style)

        // The original must remain unchanged.
        assertEquals(10f, original.x)
        assertEquals(20f, original.y)
    }

    @Test
    fun `duplicating path preserves local points`() {
        val original = EditorPath(
            x = 50f,
            y = 60f,
            points = listOf(
                Offset.Zero,
                Offset(20f, 10f),
                Offset(40f, 30f),
            ),
        )

        val duplicate = original.duplicatedBy(
            offset = Offset(15f, -10f),
        ) as EditorPath

        assertNotEquals(original.id, duplicate.id)
        assertEquals(65f, duplicate.x)
        assertEquals(50f, duplicate.y)
        assertEquals(original.points, duplicate.points)
        assertEquals(original.style, duplicate.style)
    }


    @Test
    fun `duplicating selection adds copies and selects them`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.duplicateSelectedNodes(Offset(20f, 20f))

        assertEquals(2, state.nodes.size)

        val duplicate = state.nodes.last() as EditorRectangle

        assertNotEquals(rectangle.id, duplicate.id)
        assertEquals(30f, duplicate.x)
        assertEquals(40f, duplicate.y)
        assertEquals(setOf(duplicate.id), state.selectedNodeIds)
    }

    @Test
    fun `duplicating multiple nodes creates one undo entry`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 50f,
        )

        val ellipse = EditorEllipse(
            x = 120f,
            y = 20f,
            width = 60f,
            height = 60f,
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle, ellipse))
        )

        state.selectNodes(setOf(rectangle.id, ellipse.id))
        state.duplicateSelectedNodes()

        assertEquals(4, state.nodes.size)
        assertEquals(2, state.selectedNodeIds.size)

        state.undo()

        assertEquals(listOf(rectangle, ellipse), state.nodes)
        assertFalse(state.canUndo)

        state.redo()

        assertEquals(4, state.nodes.size)
    }

    @Test
    fun `nudge preview does not modify committed nodes`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.beginNudge()
        state.updateNudge(Offset(5f, -3f))


        val preview = state.displayNodes.single() as EditorRectangle

        assertEquals(15f, preview.x)
        assertEquals(17f, preview.y)

        assertEquals(listOf(rectangle), state.nodes)
        assertEquals(listOf(rectangle), state.document.nodes)
        assertFalse(state.canUndo)
    }

    @Test
    fun `repeated nudge updates accumulate displacement`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.beginNudge()

        state.updateNudge(Offset(1f, 0f))
        state.updateNudge(Offset(1f, 0f))
        state.updateNudge(Offset(0f, -10f))

        val preview = state.displayNodes.single() as EditorRectangle

        assertEquals(12f, preview.x)
        assertEquals(10f, preview.y)

        // Original document remains unchanged.
        assertEquals(listOf(rectangle), state.nodes)
    }

    @Test
    fun `finishing multiple nudges creates one undo entry`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.beginNudge()

        repeat(20) {
            state.updateNudge(Offset(1f, 0f))
        }

        state.finishNudge()

        val moved = state.nodes.single() as EditorRectangle

        assertEquals(30f, moved.x)
        assertEquals(20f, moved.y)
        assertEquals(1, state.history.undoStack.size)

        state.undo()

        assertEquals(listOf(rectangle), state.nodes)
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)

        state.redo()

        assertEquals(moved, state.nodes.single())
    }

    @Test
    fun `cancelling a nudge discards its preview`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.beginNudge()
        state.updateNudge(Offset(50f, 30f))

        assertNotEquals(state.nodes, state.displayNodes)

        state.cancelNudge()

        assertEquals(listOf(rectangle), state.nodes)
        assertEquals(listOf(rectangle), state.displayNodes)
        assertFalse(state.canUndo)

        // Finishing after cancellation must not commit anything.
        state.finishNudge()

        assertEquals(listOf(rectangle), state.nodes)
        assertFalse(state.canUndo)
    }

    @Test
    fun `nudge moves selected nodes together`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val ellipse = EditorEllipse(
            x = 200f,
            y = 100f,
            width = 60f,
            height = 40f,
        )

        val untouched = EditorRectangle(
            x = 400f,
            y = 300f,
            width = 50f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(
                nodes = listOf(rectangle, ellipse, untouched)
            )
        )

        state.selectNodes(setOf(rectangle.id, ellipse.id))
        state.beginNudge()

        state.updateNudge(Offset(10f, -5f))
        state.finishNudge()

        val movedRectangle = state.nodes[0] as EditorRectangle
        val movedEllipse = state.nodes[1] as EditorEllipse

        assertEquals(20f, movedRectangle.x)
        assertEquals(15f, movedRectangle.y)

        assertEquals(210f, movedEllipse.x)
        assertEquals(95f, movedEllipse.y)

        assertEquals(untouched, state.nodes[2])
        assertEquals(
            setOf(rectangle.id, ellipse.id),
            state.selectedNodeIds,
        )

        assertEquals(1, state.history.undoStack.size)
    }

    @Test
    fun `zero displacement nudge does not create history`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(nodes = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.beginNudge()

        state.updateNudge(Offset(10f, 0f))
        state.updateNudge(Offset(-10f, 0f))

        state.finishNudge()

        assertEquals(listOf(rectangle), state.nodes)
        assertEquals(listOf(rectangle), state.displayNodes)
        assertFalse(state.canUndo)
        assertEquals(0, state.history.undoStack.size)
    }
}
