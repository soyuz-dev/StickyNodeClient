package org.stickynodes

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.stickynodes.data.DocumentCodec
import org.stickynodes.data.mapper.toDocument
import org.stickynodes.data.mapper.toDto
import org.stickynodes.data.model.DocumentDto
import org.stickynodes.data.model.GroupDto
import org.stickynodes.data.model.StyleDto
import org.stickynodes.data.model.TextDto
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.*
import org.stickynodes.editor.draw.model.*
import org.stickynodes.editor.draw.style.*
import org.stickynodes.editor.history.*
import org.stickynodes.editor.*
import org.stickynodes.editor.draw.geometry.connector.ConnectorAnchor
import org.stickynodes.editor.draw.geometry.connector.ConnectorHandle
import org.stickynodes.editor.draw.geometry.connector.ConnectorRoute
import org.stickynodes.editor.draw.geometry.connector.anchorPosition
import org.stickynodes.editor.draw.geometry.connector.connectorEndpointAt
import org.stickynodes.editor.draw.geometry.connector.contains
import org.stickynodes.editor.draw.geometry.connector.detachRemovedNodes
import org.stickynodes.editor.draw.geometry.connector.distanceToSegment
import org.stickynodes.editor.draw.geometry.connector.handleAt
import org.stickynodes.editor.draw.geometry.connector.roundedCorner
import org.stickynodes.editor.draw.geometry.connector.route
import org.stickynodes.editor.draw.geometry.connector.routeConnector
import org.stickynodes.editor.draw.geometry.connector.withEndpoint
import org.stickynodes.editor.draw.geometry.path.PathSampler
import org.stickynodes.editor.draw.geometry.path.bezierSegments
import org.stickynodes.editor.draw.geometry.path.containsStroke
import org.stickynodes.editor.draw.geometry.selection.NodeAlignment
import org.stickynodes.editor.draw.geometry.selection.NodeDistribution
import org.stickynodes.editor.draw.geometry.selection.NodeSizing
import org.stickynodes.editor.draw.geometry.selection.alignNodes
import org.stickynodes.editor.draw.geometry.selection.alignedMemberNodes
import org.stickynodes.editor.draw.geometry.selection.commonRotationOrNull
import org.stickynodes.editor.draw.geometry.selection.distributeNodes
import org.stickynodes.editor.draw.geometry.selection.distributedMemberNodes
import org.stickynodes.editor.draw.geometry.selection.matchShapeSizes
import org.stickynodes.editor.draw.geometry.selection.orientedGroupBoundsOrNull
import org.stickynodes.editor.draw.geometry.selection.orientedShapeSelectionOrNull
import org.stickynodes.editor.draw.geometry.selection.selectionBoundsOrNull
import org.stickynodes.editor.draw.geometry.selection.visibleNodeIds
import org.stickynodes.editor.draw.geometry.selection.visibleWorldBounds
import org.stickynodes.editor.draw.geometry.snapping.SpacingAxis
import org.stickynodes.editor.draw.geometry.snapping.SpacingPlacement
import org.stickynodes.editor.draw.geometry.snapping.findEqualSpacingSnap
import org.stickynodes.editor.draw.geometry.snapping.snapTranslation
import org.stickynodes.editor.draw.geometry.transform.OrientedGroupResizeTransform
import org.stickynodes.editor.draw.geometry.transform.ResizeHandle
import org.stickynodes.editor.history.bringForward
import org.stickynodes.editor.history.bringToFront
import org.stickynodes.editor.draw.geometry.transform.combinedBounds
import org.stickynodes.editor.draw.geometry.transform.constrainedOrientedResizeTransform
import org.stickynodes.editor.draw.geometry.transform.duplicatedBy
import org.stickynodes.editor.draw.geometry.transform.isRotationHandleHit
import org.stickynodes.editor.draw.geometry.transform.orientedGroupResizeTransform
import org.stickynodes.editor.draw.geometry.transform.orientedResizeHandleAt
import org.stickynodes.editor.draw.geometry.transform.orientedResizeHandleScreenPositions
import org.stickynodes.editor.draw.geometry.transform.orientedRotationHandlePosition
import org.stickynodes.editor.draw.geometry.transform.positionedAt
import org.stickynodes.editor.draw.geometry.transform.resizeBounds
import org.stickynodes.editor.draw.geometry.transform.resizeBoundsProportionally
import org.stickynodes.editor.draw.geometry.transform.resizeHandleAt
import org.stickynodes.editor.draw.geometry.transform.resizeHandleScreenPositions
import org.stickynodes.editor.draw.geometry.transform.resizedRotatedBy
import org.stickynodes.editor.draw.geometry.transform.resizedTo
import org.stickynodes.editor.draw.geometry.transform.resizedTogether
import org.stickynodes.editor.draw.geometry.transform.resizedWith
import org.stickynodes.editor.draw.geometry.transform.rotatedAround
import org.stickynodes.editor.draw.geometry.transform.rotatedNodesTogether
import org.stickynodes.editor.draw.geometry.transform.rotatedTo
import org.stickynodes.editor.draw.geometry.transform.rotationHandleScreenPosition
import org.stickynodes.editor.draw.geometry.transform.translatedBy
import org.stickynodes.editor.history.sendBackward
import org.stickynodes.editor.history.sendToBack
import org.stickynodes.editor.draw.geometry.transform.translatedNodesBy
import org.stickynodes.editor.tools.toTransformString
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
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
            members = listOf(
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
            members = listOf(
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
            members = listOf(
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
            members = listOf(
                EditorRectangle(
                    x = 0f,
                    y = 0f,
                    width = 50f,
                    height = 50f,
                )
            )
        )

        val second = EditorDocument(
            members = listOf(
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

        val original = EditorDocument(members = listOf(node))

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

        val original = EditorDocument(members = listOf(node))

        val history = EditorHistory(document = original)
            .commit(EditorDocument())
            .undo()
            .redo()

        assertTrue(history.document.nodes.isEmpty())
    }


    @Test
    fun `document survives JSON round trip`() {
        val original = EditorDocument(
            members = listOf(
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

    val shapes = listOf(a, b)


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
            members = listOf(
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
            members = listOf(
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
            EditorDocument(members = listOf(rectangle))
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
            EditorDocument(members = listOf(rectangle, ellipse))
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
            EditorDocument(members = listOf(rectangle))
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
            EditorDocument(members = listOf(rectangle))
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
            EditorDocument(members = listOf(rectangle))
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
            EditorDocument(members = listOf(rectangle))
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
                members = listOf(rectangle, ellipse, untouched)
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
            EditorDocument(members = listOf(rectangle))
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


    @Test
    fun `bringing selected nodes to front preserves their order`() {
        val nodes = (0 until 5).map {
            EditorRectangle(
                x = it.toFloat(),
                y = 0f,
                width = 10f,
                height = 10f,
            )
        }

        val selected = setOf(nodes[1].id, nodes[3].id)
        val reordered = nodes.bringToFront(selected)

        assertEquals(
            listOf(nodes[0], nodes[2], nodes[4], nodes[1], nodes[3]),
            reordered,
        )
    }

    @Test
    fun `sending selected nodes to back preserves their order`() {
        val nodes = (0 until 5).map {
            EditorRectangle(
                x = it.toFloat(),
                y = 0f,
                width = 10f,
                height = 10f,
            )
        }

        val selected = setOf(nodes[1].id, nodes[3].id)
        val reordered = nodes.sendToBack(selected)

        assertEquals(
            listOf(nodes[1], nodes[3], nodes[0], nodes[2], nodes[4]),
            reordered,
        )
    }

    @Test
    fun `bringing selected nodes forward skips selected neighbours`() {
        val nodes = (0 until 5).map {
            EditorRectangle(
                x = it.toFloat(),
                y = 0f,
                width = 10f,
                height = 10f,
            )
        }

        val selected = setOf(nodes[1].id, nodes[3].id)
        val reordered = nodes.bringForward(selected)

        assertEquals(
            listOf(nodes[0], nodes[2], nodes[1], nodes[4], nodes[3]),
            reordered,
        )
    }

    @Test
    fun `sending selected nodes backward skips selected neighbours`() {
        val nodes = (0 until 5).map {
            EditorRectangle(
                x = it.toFloat(),
                y = 0f,
                width = 10f,
                height = 10f,
            )
        }

        val selected = setOf(nodes[1].id, nodes[3].id)
        val reordered = nodes.sendBackward(selected)

        assertEquals(
            listOf(nodes[1], nodes[0], nodes[3], nodes[2], nodes[4]),
            reordered,
        )
    }

    @Test
    fun `ordering with empty selection changes nothing`() {
        val nodes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 10f,
                height = 10f,
            ),
            EditorEllipse(
                x = 20f,
                y = 0f,
                width = 10f,
                height = 10f,
            ),
        )

        assertEquals(nodes, nodes.bringToFront(emptySet()))
        assertEquals(nodes, nodes.sendToBack(emptySet()))
        assertEquals(nodes, nodes.bringForward(emptySet()))
        assertEquals(nodes, nodes.sendBackward(emptySet()))
    }


    @Test
    fun `arrangement availability respects stacking boundaries`() {
        val bottom = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 10f,
            height = 10f,
        )

        val middle = EditorRectangle(
            x = 20f,
            y = 0f,
            width = 10f,
            height = 10f,
        )

        val top = EditorRectangle(
            x = 40f,
            y = 0f,
            width = 10f,
            height = 10f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(bottom, middle, top))
        )

        state.selectNode(top.id)

        assertFalse(state.canBringSelectionForward)
        assertFalse(state.canBringSelectionToFront)
        assertTrue(state.canSendSelectionBackward)
        assertTrue(state.canSendSelectionToBack)

        state.selectNode(bottom.id)

        assertTrue(state.canBringSelectionForward)
        assertTrue(state.canBringSelectionToFront)
        assertFalse(state.canSendSelectionBackward)
        assertFalse(state.canSendSelectionToBack)

        state.selectNode(middle.id)

        assertTrue(state.canBringSelectionForward)
        assertTrue(state.canBringSelectionToFront)
        assertTrue(state.canSendSelectionBackward)
        assertTrue(state.canSendSelectionToBack)

        state.clearSelection()

        assertFalse(state.canBringSelectionForward)
        assertFalse(state.canBringSelectionToFront)
        assertFalse(state.canSendSelectionBackward)
        assertFalse(state.canSendSelectionToBack)
    }


    @Test
    fun `select all selects every node without changing history`() {
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

        val state = EditorDocumentState(
            EditorDocument(members = listOf(rectangle, ellipse))
        )

        state.selectNode(rectangle.id)
        state.selectAllNodes()

        assertEquals(
            setOf(rectangle.id, ellipse.id),
            state.selectedNodeIds,
        )

        assertEquals(listOf(rectangle, ellipse), state.nodes)
        assertFalse(state.canUndo)

        state.clearSelection()
        assertTrue(state.selectedNodeIds.isEmpty())
    }

    @Test
    fun `clipboard copies selected nodes in document order`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 50f, height = 50f,
        )
        val b = EditorEllipse(
            x = 100f, y = 0f,
            width = 50f, height = 50f,
        )
        val c = EditorRectangle(
            x = 200f, y = 0f,
            width = 50f, height = 50f,
        )

        val clipboard = EditorNodeClipboard()
        clipboard.copy(
            nodes = listOf(a, b, c),
            selectedIds = setOf(c.id, a.id),
        )

        val pasted = clipboard.paste(Offset.Zero)

        assertEquals(2, pasted.size)
        assertEquals(listOf(a.x, c.x), pasted.map { it.x })
        assertNotEquals(a.id, pasted[0].id)
        assertNotEquals(c.id, pasted[1].id)
    }

    @Test
    fun `clipboard paste generates new IDs each time`() {
        val node = EditorRectangle(
            x = 10f, y = 20f,
            width = 100f, height = 50f,
        )

        val clipboard = EditorNodeClipboard()
        clipboard.copy(listOf(node), setOf(node.id))

        val first = clipboard.paste()
        val second = clipboard.paste()

        assertNotEquals(node.id, first.single().id)
        assertNotEquals(first.single().id, second.single().id)

        assertEquals(30f, first.single().x)
        assertEquals(40f, first.single().y)
    }

    @Test
    fun `clipboard handles empty selections`() {
        val clipboard = EditorNodeClipboard()

        assertFalse(clipboard.hasContent)
        assertTrue(clipboard.paste().isEmpty())

        clipboard.copy(listOf(), emptySet())

        assertFalse(clipboard.hasContent)
    }


    @Test
    fun `copying nodes does not modify document history`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.copySelectedNodes()

        assertTrue(state.clipboard.hasContent)
        assertEquals(listOf(rectangle), state.nodes)
        assertFalse(state.canUndo)
    }

    @Test
    fun `pasting nodes creates one undoable operation`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.copySelectedNodes()
        state.pasteNodes()

        assertEquals(2, state.nodes.size)
        assertEquals(1, state.history.undoStack.size)

        val pasted = state.nodes.last() as EditorRectangle

        assertNotEquals(rectangle.id, pasted.id)
        assertEquals(30f, pasted.x)
        assertEquals(40f, pasted.y)
        assertEquals(setOf(pasted.id), state.selectedNodeIds)

        state.undo()

        assertEquals(listOf(rectangle), state.nodes)
        assertFalse(state.canUndo)

        state.redo()

        assertEquals(2, state.nodes.size)
        assertEquals(pasted, state.nodes.last())
    }

    @Test
    fun `cutting selection is undoable and preserves clipboard`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(rectangle))
        )

        state.selectNode(rectangle.id)
        state.cutSelectedNodes()

        assertTrue(state.nodes.isEmpty())
        assertTrue(state.selectedNodeIds.isEmpty())
        assertTrue(state.clipboard.hasContent)
        assertEquals(1, state.history.undoStack.size)

        state.undo()

        assertEquals(listOf(rectangle), state.nodes)

        // Clipboard contents survive undo.
        assertTrue(state.clipboard.hasContent)

        state.pasteNodes()

        assertEquals(2, state.nodes.size)
    }

    @Test
    fun `pasting empty clipboard changes nothing`() {
        val state = EditorDocumentState()

        state.pasteNodes()

        assertTrue(state.nodes.isEmpty())
        assertFalse(state.canUndo)
    }


    @Test
    fun `repeated pastes advance by incremental offsets`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 100f,
            width = 50f,
            height = 50f,
        )

        val clipboard = EditorNodeClipboard()
        clipboard.copy(
            nodes = listOf(rectangle),
            selectedIds = setOf(rectangle.id),
        )

        val first = clipboard.paste().single()
        val second = clipboard.paste().single()
        val third = clipboard.paste().single()

        assertEquals(120f, first.x)
        assertEquals(140f, second.x)
        assertEquals(160f, third.x)

        assertEquals(120f, first.y)
        assertEquals(140f, second.y)
        assertEquals(160f, third.y)

        assertNotEquals(first.id, second.id)
        assertNotEquals(second.id, third.id)

        clipboard.copy(
            nodes = listOf(rectangle),
            selectedIds = setOf(rectangle.id),
        )

        val afterCopy = clipboard.paste().single()

        assertEquals(120f, afterCopy.x)
        assertEquals(120f, afterCopy.y)
    }


    @Test
    fun `creating a text session starts with empty text`() {
        val session = TextEditSession.Create(
            position = Offset(100f, 200f),
            fontSize = 24f,
        )

        assertEquals(Offset(100f, 200f), session.position)
        assertEquals("", session.initialText)
        assertEquals(24f, session.fontSize)
    }

    @Test
    fun `existing text node produces edit session`() {
        val node = EditorText(
            x = 50f,
            y = 80f,
            text = "Hello, world!",
            fontSize = 32f,
        )

        val session = node.toEditSession()

        assertEquals(node.id, session.nodeId)
        assertEquals(Offset(50f, 80f), session.position)
        assertEquals("Hello, world!", session.initialText)
        assertEquals(32f, session.fontSize)
    }


    @Test
    fun `visible world bounds respect camera pan and zoom`() {
        val camera = EditorCamera(
            position = Offset(100f, 200f),
            zoom = 2f,
        )

        val bounds = camera.visibleWorldBounds(
            viewport = IntSize(800, 600),
        )

        assertEquals(Rect(100f, 200f, 500f, 500f), bounds)
    }

    @Test
    fun `select visible includes partially intersecting nodes`() {
        val inside = EditorRectangle(
            x = 20f,
            y = 20f,
            width = 30f,
            height = 30f,
        )

        val partial = EditorRectangle(
            x = 90f,
            y = 40f,
            width = 40f,
            height = 20f,
        )

        val outside = EditorRectangle(
            x = 150f,
            y = 150f,
            width = 20f,
            height = 20f,
        )

        val selected = listOf(
            inside,
            partial,
            outside,
        ).visibleNodeIds(
            viewportBounds = Rect(0f, 0f, 100f, 100f),
            selectionBounds = { it.selectionBounds },
        )

        assertEquals(
            setOf(inside.id, partial.id),
            selected,
        )
    }

    @Test
    fun `select visible uses supplied node bounds`() {
        val node = EditorRectangle(
            x = 500f,
            y = 500f,
            width = 50f,
            height = 50f,
        )

        val selected = listOf(node).visibleNodeIds(
            viewportBounds = Rect(0f, 0f, 180f, 180f),
            selectionBounds = {
                Rect(80f, 80f, 120f, 120f)
            }
        )

        assertEquals(setOf(node.id), selected)
    }


    @Test
    fun `select visible returns empty set when no nodes intersect`() {
        val node = EditorRectangle(
            x = 200f,
            y = 200f,
            width = 40f,
            height = 40f,
        )

        val selected = listOf(node).visibleNodeIds(
            viewportBounds = Rect(0f, 0f, 100f, 100f),
            selectionBounds = { it.selectionBounds },
        )

        assertTrue(selected.isEmpty())
    }


    @Test
    fun `align left moves selected nodes to the same left edge`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 40f,
            height = 30f,
        )

        val second = EditorRectangle(
            x = 100f,
            y = 80f,
            width = 60f,
            height = 20f,
        )

        val result = listOf(first, second).alignNodes(
            selectedIds = setOf(first.id, second.id),
            alignment = NodeAlignment.LEFT,
            selectionBounds = { it.selectionBounds },
        )

        val alignedFirst = result[0] as EditorRectangle
        val alignedSecond = result[1] as EditorRectangle

        assertEquals(10f, alignedFirst.x)
        assertEquals(10f, alignedSecond.x)

        assertEquals(20f, alignedFirst.y)
        assertEquals(80f, alignedSecond.y)

        assertEquals(first.id, alignedFirst.id)
        assertEquals(second.id, alignedSecond.id)
    }

    @Test
    fun `aligning selected nodes creates one undoable change`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 40f,
            height = 30f,
        )

        val second = EditorRectangle(
            x = 100f,
            y = 80f,
            width = 60f,
            height = 20f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(first, second))
        )

        state.selectNodes(setOf(first.id, second.id))

        state.alignSelectedNodes(
            alignment = NodeAlignment.LEFT,
            selectionBounds = { it.selectionBounds },
        )

        val aligned = state.nodes
            .filterIsInstance<EditorRectangle>()

        assertEquals(10f, aligned[0].x)
        assertEquals(10f, aligned[1].x)

        state.undo()

        assertEquals(listOf(first, second), state.nodes)

        state.redo()

        assertEquals(aligned, state.nodes)
    }


    @Test
    fun `alignment requires at least two selected nodes`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 40f,
            height = 30f,
        )

        val second = EditorRectangle(
            x = 100f,
            y = 80f,
            width = 60f,
            height = 20f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(first, second))
        )

        state.selectNode(first.id)

        assertFalse(state.canAlignSelection)

        state.alignSelectedNodes(
            alignment = NodeAlignment.LEFT,
            selectionBounds = { it.selectionBounds },
        )

        assertEquals(listOf(first, second), state.nodes)
        assertFalse(state.canUndo)
    }


    @Test
    fun `horizontal distribution creates equal gaps`() {
        val first = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val middle = EditorRectangle(
            x = 40f, y = 10f,
            width = 30f, height = 20f,
        )

        val last = EditorRectangle(
            x = 180f, y = 20f,
            width = 20f, height = 20f,
        )

        val result = listOf(first, middle, last).distributeNodes(
            selectedIds = setOf(first.id, middle.id, last.id),
            distribution = NodeDistribution.HORIZONTAL,
            selectionBounds = { it.selectionBounds },
        )

        val rectangles = result.filterIsInstance<EditorRectangle>()

        // Span: 0..200
        // Total widths: 20 + 30 + 20 = 70
        // Each gap: (200 - 70) / 2 = 65

        assertEquals(0f, rectangles[0].x)
        assertEquals(85f, rectangles[1].x)
        assertEquals(180f, rectangles[2].x)

        // Y coordinates remain unchanged.
        assertEquals(10f, rectangles[1].y)
    }


    @Test
    fun `distributing selected nodes creates one undoable change`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 20f,
        )

        val middle = EditorRectangle(
            x = 40f,
            y = 10f,
            width = 30f,
            height = 20f,
        )

        val last = EditorRectangle(
            x = 180f,
            y = 20f,
            width = 20f,
            height = 20f,
        )

        val originals = listOf(first, middle, last)

        val state = EditorDocumentState(
            EditorDocument(members = originals)
        )

        state.selectNodes(originals.mapTo(mutableSetOf()) { it.id })

        assertTrue(state.canDistributeSelection)

        state.distributeSelectedNodes(
            distribution = NodeDistribution.HORIZONTAL,
            selectionBounds = { it.selectionBounds },
        )

        val distributed = state.nodes.filterIsInstance<EditorRectangle>()

        assertEquals(0f, distributed[0].x)
        assertEquals(85f, distributed[1].x)
        assertEquals(180f, distributed[2].x)

        state.undo()

        assertEquals(originals, state.nodes)

        state.redo()

        assertEquals(distributed, state.nodes)
    }


    @Test
    fun `distribution requires at least three selected nodes`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 20f,
        )

        val second = EditorRectangle(
            x = 100f,
            y = 0f,
            width = 20f,
            height = 20f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(first, second))
        )

        state.selectNodes(setOf(first.id, second.id))

        assertFalse(state.canDistributeSelection)

        state.distributeSelectedNodes(
            distribution = NodeDistribution.HORIZONTAL,
            selectionBounds = { it.selectionBounds },
        )

        assertEquals(listOf(first, second), state.nodes)
        assertFalse(state.canUndo)
    }

    @Test
    fun `vertical distribution creates equal gaps`() {
        val first = EditorRectangle(
            x = 10f,
            y = 0f,
            width = 20f,
            height = 20f,
        )

        val middle = EditorRectangle(
            x = 30f,
            y = 40f,
            width = 20f,
            height = 30f,
        )

        val last = EditorRectangle(
            x = 50f,
            y = 180f,
            width = 20f,
            height = 20f,
        )

        val result = listOf(first, middle, last).distributeNodes(
            selectedIds = setOf(first.id, middle.id, last.id),
            distribution = NodeDistribution.VERTICAL,
            selectionBounds = { it.selectionBounds },
        )

        val rectangles = result.filterIsInstance<EditorRectangle>()

        assertEquals(0f, rectangles[0].y)
        assertEquals(85f, rectangles[1].y)
        assertEquals(180f, rectangles[2].y)

        // Horizontal positions must be preserved.
        assertEquals(10f, rectangles[0].x)
        assertEquals(30f, rectangles[1].x)
        assertEquals(50f, rectangles[2].x)
    }


    @Test
    fun `matching widths preserves shape centres`() {
        val small = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 40f,
            height = 30f,
        )

        val large = EditorRectangle(
            x = 100f,
            y = 50f,
            width = 100f,
            height = 60f,
        )

        val result = listOf(small, large).matchShapeSizes(
            selectedIds = setOf(small.id, large.id),
            sizing = NodeSizing.MATCH_WIDTH,
        )

        val first = result[0] as EditorRectangle
        val second = result[1] as EditorRectangle

        assertEquals(100f, first.width)
        assertEquals(100f, second.width)

        assertEquals(30f, first.height)
        assertEquals(60f, second.height)

        assertEquals(small.bounds.center, first.bounds.center)
        assertEquals(large.bounds.center, second.bounds.center)

        assertEquals(small.id, first.id)
        assertEquals(large.id, second.id)
    }


    @Test
    fun `matching selected widths creates one undoable change`() {
        val small = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 40f,
            height = 30f,
        )

        val large = EditorRectangle(
            x = 100f,
            y = 50f,
            width = 100f,
            height = 60f,
        )

        val originals = listOf(small, large)

        val state = EditorDocumentState(
            EditorDocument(members = originals)
        )

        state.selectNodes(setOf(small.id, large.id))

        assertTrue(state.canMatchShapeSizes)

        state.matchSelectedShapeSizes(
            NodeSizing.MATCH_WIDTH
        )

        val resized = state.nodes
            .filterIsInstance<EditorRectangle>()

        assertEquals(100f, resized[0].width)
        assertEquals(100f, resized[1].width)

        // Height and centre are preserved.
        assertEquals(30f, resized[0].height)
        assertEquals(60f, resized[1].height)

        assertEquals(small.bounds.center, resized[0].bounds.center)
        assertEquals(large.bounds.center, resized[1].bounds.center)

        // One undo restores the entire original document.
        state.undo()

        assertEquals(originals, state.nodes)

        // One redo reapplies the size matching.
        state.redo()

        assertEquals(resized, state.nodes)
    }


    @Test
    fun `matching sizes requires two selected shapes`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 40f,
            height = 30f,
        )

        val text = EditorText(
            x = 100f,
            y = 100f,
            text = "Hello",
        )

        val originals = listOf(rectangle, text)

        val state = EditorDocumentState(
            EditorDocument(members = originals)
        )

        state.selectNodes(setOf(rectangle.id, text.id))

        assertFalse(state.canMatchShapeSizes)

        state.matchSelectedShapeSizes(NodeSizing.MATCH_BOTH)

        assertEquals(originals, state.nodes)
        assertFalse(state.canUndo)
    }

    @Test
    fun `absolute positioning moves node to requested coordinates`() {
        val original = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
            rotation = 0.5f,
        )

        val positioned = original.positionedAt(
            Offset(200f, 150f)
        ) as EditorRectangle

        assertEquals(200f, positioned.x)
        assertEquals(150f, positioned.y)

        assertEquals(original.width, positioned.width)
        assertEquals(original.height, positioned.height)
        assertEquals(original.rotation, positioned.rotation)
        assertEquals(original.id, positioned.id)
    }

    @Test
    fun `positioning node at its current position changes nothing`() {
        val node = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
        )

        val result = node.positionedAt(
            Offset(20f, 30f)
        )

        assertEquals(node, result)
    }

    @Test
    fun `absolute positioning rejects non finite coordinates`() {
        val node = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 40f,
            height = 30f,
        )

        assertFailsWith<IllegalArgumentException> {
            node.positionedAt(
                Offset(Float.NaN, 100f)
            )
        }
    }


    @Test
    fun `absolute positioning creates one undoable document change`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(rectangle))
        )

        state.selectNode(rectangle.id)

        state.positionSelectedNodeAt(
            Offset(200f, 150f)
        )

        val moved = state.nodes.single() as EditorRectangle

        assertEquals(200f, moved.x)
        assertEquals(150f, moved.y)
        assertEquals(rectangle.id, moved.id)

        state.undo()

        assertEquals(rectangle, state.nodes.single())

        state.redo()

        assertEquals(moved, state.nodes.single())
    }

    @Test
    fun `absolute positioning ignores multiple selected nodes`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 40f,
            height = 30f,
        )

        val second = EditorRectangle(
            x = 100f,
            y = 100f,
            width = 40f,
            height = 30f,
        )

        val originals = listOf(first, second)

        val state = EditorDocumentState(
            EditorDocument(members = originals)
        )

        state.selectNodes(setOf(first.id, second.id))

        state.positionSelectedNodeAt(
            Offset(200f, 200f)
        )

        assertEquals(originals, state.nodes)
        assertFalse(state.canUndo)
    }

    @Test
    fun `positioning node at existing coordinates does not create history`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(rectangle))
        )

        state.selectNode(rectangle.id)

        state.positionSelectedNodeAt(
            Offset(20f, 30f)
        )

        assertEquals(rectangle, state.nodes.single())
        assertFalse(state.canUndo)
    }


    @Test
    fun `absolute resizing preserves shape position and rotation`() {
        val original = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
            rotation = 0.5f,
        )

        val resized = original.resizedTo(
            width = 120f,
            height = 60f,
        ) as EditorRectangle

        assertEquals(20f, resized.x)
        assertEquals(30f, resized.y)
        assertEquals(120f, resized.width)
        assertEquals(60f, resized.height)
        assertEquals(original.rotation, resized.rotation)
        assertEquals(original.id, resized.id)
    }

    @Test
    fun `absolute resizing rejects invalid dimensions`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 40f,
            height = 30f,
        )

        assertFailsWith<IllegalArgumentException> {
            rectangle.resizedTo(width = -10f)
        }

        assertFailsWith<IllegalArgumentException> {
            rectangle.resizedTo(height = Float.NaN)
        }
    }

    @Test
    fun `resizing to existing dimensions is a no op`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
        )

        assertEquals(
            rectangle,
            rectangle.resizedTo(width = 80f, height = 40f)
        )
    }


    @Test
    fun `absolute rotation preserves shape position and dimensions`() {
        val original = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
            rotation = 0.5f,
        )

        val rotated = original.rotatedTo(1.2f) as EditorRectangle

        assertEquals(20f, rotated.x)
        assertEquals(30f, rotated.y)
        assertEquals(80f, rotated.width)
        assertEquals(40f, rotated.height)
        assertEquals(1.2f, rotated.rotation)
        assertEquals(original.id, rotated.id)
        assertEquals(original.style, rotated.style)
    }

    @Test
    fun `absolute rotation rejects non finite values`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 40f,
            height = 30f,
        )

        assertFailsWith<IllegalArgumentException> {
            rectangle.rotatedTo(Float.NaN)
        }

        assertFailsWith<IllegalArgumentException> {
            rectangle.rotatedTo(Float.POSITIVE_INFINITY)
        }

        assertFailsWith<IllegalArgumentException> {
            rectangle.rotatedTo(Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `rotating to existing angle returns the same instance`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 80f,
            height = 40f,
            rotation = 0.5f,
        )

        assertSame(
            rectangle,
            rectangle.rotatedTo(0.5f)
        )
    }


    @Test
    fun `transform numbers display at most two decimal places`() {
        assertEquals("45", 45f.toTransformString())
        assertEquals("57.3", 57.295776f.toTransformString())
        assertEquals("12.35", 12.3456f.toTransformString())
        assertEquals("100.5", 100.5f.toTransformString())
        assertEquals("-20.68", (-20.678f).toTransformString())
    }

    @Test
    fun `transform formatting handles near integers and zero`() {
        assertEquals("45", 45.000004f.toTransformString())
        assertEquals("90", 89.99999f.toTransformString())
        assertEquals("0", 0f.toTransformString())
        assertEquals("0", (-0.0f).toTransformString())
    }

    @Test
    fun `transform formatting handles non finite values`() {
        assertEquals("NaN", Float.NaN.toTransformString())
        assertEquals("Infinity", Float.POSITIVE_INFINITY.toTransformString())
        assertEquals("-Infinity", Float.NEGATIVE_INFINITY.toTransformString())
    }

    @Test
    fun `absolute group positioning moves selected nodes together`() {
        val first = EditorRectangle(
            x = 10f, y = 20f,
            width = 30f, height = 40f,
        )

        val second = EditorRectangle(
            x = 100f, y = 80f,
            width = 20f, height = 20f,
        )

        val unselected = EditorRectangle(
            x = 200f, y = 200f,
            width = 10f, height = 10f,
        )

        val originals = listOf(first, second, unselected)

        val state = EditorDocumentState(
            EditorDocument(members = originals)
        )

        state.selectNodes(setOf(first.id, second.id))

        state.positionSelectedNodesAt(
            position = Offset(50f, 60f),
            selectionBounds = { it.selectionBounds },
        )

        // Group's original top-left was (10, 20).
        // Therefore every selected node moves by (40, 40).
        assertEquals(50f, state.nodes[0].x)
        assertEquals(60f, state.nodes[0].y)

        assertEquals(140f, state.nodes[1].x)
        assertEquals(120f, state.nodes[1].y)

        assertEquals(unselected, state.nodes[2])

        state.undo()
        assertEquals(originals, state.nodes)

        state.redo()
        assertEquals(50f, state.nodes[0].x)
        assertEquals(140f, state.nodes[1].x)
    }

    @Test
    fun `positioning group at existing bounds does not create history`() {
        val first = EditorRectangle(
            x = 10f, y = 20f,
            width = 30f, height = 40f,
        )

        val second = EditorRectangle(
            x = 100f, y = 80f,
            width = 20f, height = 20f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(first, second))
        )

        state.selectNodes(setOf(first.id, second.id))

        state.positionSelectedNodesAt(
            position = Offset(10f, 20f),
            selectionBounds = { it.selectionBounds },
        )

        assertFalse(state.canUndo)
    }


    @Test
    fun `nested groups flatten nodes in order`() {
        val first = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val second = EditorEllipse(
            x = 50f, y = 0f,
            width = 20f, height = 20f,
        )

        val third = EditorText(
            x = 100f, y = 0f,
            text = "Hello",
        )

        val inner = EditorGroup(
            members = listOf(second, third),
        )

        val outer = EditorGroup(
            members = listOf(first, inner),
        )

        assertEquals(
            listOf(first, second, third),
            outer.flattenedNodes(),
        )
    }

    @Test
    fun `recursive node mapping preserves nested groups`() {
        val rectangle = EditorRectangle(
            x = 10f, y = 20f,
            width = 30f, height = 40f,
        )

        val ellipse = EditorEllipse(
            x = 100f, y = 200f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(
            members = listOf(ellipse),
        )

        val outer = EditorGroup(
            members = listOf(rectangle, inner),
        )

        val updated = outer.mapNodes { node ->
            if (node.id == ellipse.id) {
                node.translatedBy(Offset(10f, 20f))
            } else {
                node
            }
        } as EditorGroup

        val updatedInner = updated.members[1] as EditorGroup
        val updatedEllipse = updatedInner.members[0] as EditorEllipse

        assertEquals(110f, updatedEllipse.x)
        assertEquals(220f, updatedEllipse.y)

        assertEquals(outer.id, updated.id)
        assertEquals(inner.id, updatedInner.id)
        assertEquals(rectangle, updated.members[0])

        // Original tree is unchanged.
        assertEquals(ellipse, inner.members[0])
    }

    @Test
    fun `unchanged recursive mapping preserves group instances`() {
        val rectangle = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(
            members = listOf(rectangle),
        )

        val outer = EditorGroup(
            members = listOf(inner),
        )

        val result = outer.mapNodes { it }

        assertSame(outer, result)
    }


    @Test
    fun `grouping selected nodes creates one group`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorRectangle(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )
        val c = EditorEllipse(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )
        val d = EditorRectangle(
            x = 90f, y = 0f,
            width = 20f, height = 20f,
        )

        val original = EditorDocument(
            members = listOf(a, b, c, d)
        )

        val state = EditorDocumentState(original)
        state.selectNodes(setOf(a.id, c.id))

        assertTrue(state.groupSelectedNodes())

        val members = state.document.members

        assertEquals(3, members.size)
        assertEquals(b, members[0])
        assertEquals(d, members[2])

        val group = members[1] as EditorGroup

        assertEquals(listOf(a, c), group.members)
        assertEquals(listOf(b, a, c, d), state.nodes)
        assertTrue(state.selectedNodeIds.isEmpty())
        assertTrue(state.canUndo)

        state.undo()
        assertEquals(original, state.document)

        state.redo()
        assertEquals(members, state.document.members)
    }

    @Test
    fun `grouping inside nested groups preserves ancestors`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(
            members = listOf(a, b)
        )
        val outer = EditorGroup(
            members = listOf(inner)
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(outer))
        )

        state.selectNodes(setOf(a.id, b.id))

        assertTrue(state.groupSelectedNodes())

        val updatedOuter =
            state.document.members.single() as EditorGroup

        val updatedInner =
            updatedOuter.members.single() as EditorGroup

        val createdGroup =
            updatedInner.members.single() as EditorGroup

        assertEquals(outer.id, updatedOuter.id)
        assertEquals(inner.id, updatedInner.id)
        assertEquals(listOf(a, b), createdGroup.members)
    }

    @Test
    fun `grouping nodes from different parents is rejected`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorRectangle(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val firstGroup = EditorGroup(members = listOf(a))
        val secondGroup = EditorGroup(members = listOf(b))

        val original = EditorDocument(
            members = listOf(firstGroup, secondGroup)
        )

        val state = EditorDocumentState(original)
        state.selectNodes(setOf(a.id, b.id))

        assertFalse(state.groupSelectedNodes())
        assertEquals(original, state.document)
        assertFalse(state.canUndo)
        assertEquals(setOf(a.id, b.id), state.selectedNodeIds)
    }

    @Test
    fun `grouping fewer than two nodes is a no op`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val original = EditorDocument(
            members = listOf(a)
        )

        val state = EditorDocumentState(original)
        state.selectNode(a.id)

        assertFalse(state.groupSelectedNodes())
        assertEquals(original, state.document)
        assertFalse(state.canUndo)
    }


    @Test
    fun `ungrouping restores children at original position`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )
        val c = EditorRectangle(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )
        val d = EditorRectangle(
            x = 90f, y = 0f,
            width = 20f, height = 20f,
        )

        val group = EditorGroup(members = listOf(b, c))
        val original = EditorDocument(
            members = listOf(a, group, d)
        )

        val state = EditorDocumentState(original)

        assertTrue(state.ungroup(group.id))
        assertEquals(listOf(a, b, c, d), state.document.members)
        assertEquals(listOf(a, b, c, d), state.nodes)

        state.undo()
        assertEquals(original, state.document)

        state.redo()
        assertEquals(listOf(a, b, c, d), state.document.members)
    }

    @Test
    fun `ungrouping outer group preserves inner group`() {
        val rectangle = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val ellipse = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(ellipse))
        val outer = EditorGroup(members = listOf(rectangle, inner))

        val state = EditorDocumentState(
            EditorDocument(members = listOf(outer))
        )

        assertTrue(state.ungroup(outer.id))

        assertEquals(
            listOf(rectangle, inner),
            state.document.members,
        )

        assertEquals(
            listOf(rectangle, ellipse),
            state.nodes,
        )

        assertEquals(inner.id, (state.document.members[1] as EditorGroup).id)
    }

    @Test
    fun `ungrouping nested group preserves ancestor`() {
        val rectangle = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val ellipse = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(rectangle, ellipse))
        val outer = EditorGroup(members = listOf(inner))

        val state = EditorDocumentState(
            EditorDocument(members = listOf(outer))
        )

        assertTrue(state.ungroup(inner.id))

        val updatedOuter =
            state.document.members.single() as EditorGroup

        assertEquals(outer.id, updatedOuter.id)
        assertEquals(listOf(rectangle, ellipse), updatedOuter.members)
    }

    @Test
    fun `ungrouping unknown id is a no op`() {
        val rectangle = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val original = EditorDocument(
            members = listOf(rectangle)
        )

        val state = EditorDocumentState(original)

        assertFalse(state.ungroup(Uuid.random()))
        assertFalse(state.ungroup(rectangle.id))
        assertEquals(original, state.document)
        assertFalse(state.canUndo)
    }


    @Test
    fun `member hierarchy resolves nested ancestry`() {
        val node = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(node))
        val outer = EditorGroup(members = listOf(inner))

        val members = listOf(outer)

        assertEquals(
            listOf(outer, inner, node),
            members.pathToMember(node.id),
        )

        assertEquals(
            listOf(outer, inner, node),
            members.flattenedMembers(),
        )
    }

    @Test
    fun `repeated clicks descend through groups`() {
        val node = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(node))
        val outer = EditorGroup(members = listOf(inner))

        val document = EditorDocument(members = listOf(outer))

        val first = document.resolveMemberSelection(
            hitNodeId = node.id,
            selectedMemberIds = emptySet(),
        )

        assertEquals(setOf(outer.id), first)

        val second = document.resolveMemberSelection(
            hitNodeId = node.id,
            selectedMemberIds = first,
        )

        assertEquals(setOf(inner.id), second)

        val third = document.resolveMemberSelection(
            hitNodeId = node.id,
            selectedMemberIds = second,
        )

        assertEquals(setOf(node.id), third)
    }

    @Test
    fun `shift selection promotes members to common parent`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val c = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val e = EditorRectangle(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )

        val d = EditorGroup(members = listOf(e))
        val b = EditorGroup(members = listOf(c, d))

        val document = EditorDocument(
            members = listOf(a, b)
        )

        val insideGroup = document.resolveMemberSelection(
            hitNodeId = c.id,
            selectedMemberIds = setOf(e.id),
            extendSelection = true,
        )

        assertEquals(setOf(c.id, d.id), insideGroup)

        val outsideGroup = document.resolveMemberSelection(
            hitNodeId = a.id,
            selectedMemberIds = setOf(e.id),
            extendSelection = true,
        )

        assertEquals(setOf(a.id, b.id), outsideGroup)
    }

    @Test
    fun `shift clicking inside selected group preserves group selection`() {
        val node = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val group = EditorGroup(members = listOf(node))
        val document = EditorDocument(members = listOf(group))

        val selection = document.resolveMemberSelection(
            hitNodeId = node.id,
            selectedMemberIds = setOf(group.id),
            extendSelection = true,
        )

        assertEquals(setOf(group.id), selection)
    }

    @Test
    fun `shift clicking selected sibling toggles selection`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val b = EditorRectangle(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val document = EditorDocument(members = listOf(a, b))

        val selection = document.resolveMemberSelection(
            hitNodeId = a.id,
            selectedMemberIds = setOf(a.id, b.id),
            extendSelection = true,
        )

        assertEquals(setOf(b.id), selection)
    }


    @Test
    fun `member selection normalization preserves sibling invariant`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val c = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val e = EditorRectangle(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )

        val d = EditorGroup(members = listOf(e))
        val b = EditorGroup(members = listOf(c, d))

        val document = EditorDocument(
            members = listOf(a, b)
        )

        assertEquals(
            setOf(e.id),
            document.normalizeMemberSelection(setOf(e.id)),
        )

        assertEquals(
            setOf(c.id, d.id),
            document.normalizeMemberSelection(setOf(c.id, e.id)),
        )

        assertEquals(
            setOf(a.id, b.id),
            document.normalizeMemberSelection(setOf(a.id, e.id)),
        )

        assertEquals(
            setOf(b.id),
            document.normalizeMemberSelection(setOf(b.id, e.id)),
        )

        assertEquals(
            emptySet(),
            document.normalizeMemberSelection(setOf(Uuid.random())),
        )
    }


    @Test
    fun `selecting a group exposes its descendant nodes`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val b = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val group = EditorGroup(members = listOf(a, b))
        val state = EditorDocumentState(
            EditorDocument(members = listOf(group))
        )

        state.selectMember(group.id)

        assertEquals(setOf(group.id), state.selectedMemberIds)
        assertEquals(listOf(group), state.selectedMembers)
        assertEquals(group, state.selectedMember)

        assertEquals(listOf(a, b), state.selectedNodes)
        assertEquals(setOf(a.id, b.id), state.selectedNodeIds)

        assertNull(state.selectedNode)
        assertNull(state.selectedNodeId)
    }

    @Test
    fun `document state progressively selects nested groups`() {
        val node = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(node))
        val outer = EditorGroup(members = listOf(inner))

        val state = EditorDocumentState(
            EditorDocument(members = listOf(outer))
        )

        state.selectHitNode(node.id)
        assertEquals(setOf(outer.id), state.selectedMemberIds)

        state.selectHitNode(node.id)
        assertEquals(setOf(inner.id), state.selectedMemberIds)

        state.selectHitNode(node.id)
        assertEquals(setOf(node.id), state.selectedMemberIds)

        assertEquals(node, state.selectedNode)
    }

    @Test
    fun `selecting nested members promotes them to siblings`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val c = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val e = EditorRectangle(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )

        val d = EditorGroup(members = listOf(e))
        val b = EditorGroup(members = listOf(c, d))

        val state = EditorDocumentState(
            EditorDocument(members = listOf(a, b))
        )

        state.selectMembers(setOf(c.id, e.id))
        assertEquals(setOf(c.id, d.id), state.selectedMemberIds)

        state.selectMembers(setOf(a.id, e.id))
        assertEquals(setOf(a.id, b.id), state.selectedMemberIds)

        state.selectMembers(setOf(b.id, e.id))
        assertEquals(setOf(b.id), state.selectedMemberIds)
    }

    @Test
    fun `ungrouping selected group invalidates its selection`() {
        val node = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val group = EditorGroup(members = listOf(node))
        val state = EditorDocumentState(
            EditorDocument(members = listOf(group))
        )

        state.selectMember(group.id)

        assertTrue(state.ungroup(group.id))
        assertTrue(state.selectedMemberIds.isEmpty())

        state.undo()

        assertTrue(state.selectedMemberIds.isEmpty())
        assertEquals(
            listOf(group),
            state.document.members,
        )
    }


    @Test
    fun `clipboard preserves nested group hierarchy`() {
        val rectangle = EditorRectangle(
            x = 10f, y = 20f,
            width = 30f, height = 40f,
        )

        val ellipse = EditorEllipse(
            x = 50f, y = 60f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(ellipse))
        val outer = EditorGroup(members = listOf(rectangle, inner))

        val clipboard = EditorNodeClipboard()

        clipboard.copyMembers(
            members = listOf(outer),
            selectedIds = setOf(outer.id),
        )

        val pasted = clipboard.pasteMembers().single() as EditorGroup
        val pastedInner = pasted.members[1] as EditorGroup
        val pastedRectangle = pasted.members[0] as EditorRectangle
        val pastedEllipse = pastedInner.members.single() as EditorEllipse

        assertNotEquals(outer.id, pasted.id)
        assertNotEquals(inner.id, pastedInner.id)
        assertNotEquals(rectangle.id, pastedRectangle.id)
        assertNotEquals(ellipse.id, pastedEllipse.id)

        assertEquals(30f, pastedRectangle.x)
        assertEquals(40f, pastedRectangle.y)
        assertEquals(70f, pastedEllipse.x)
        assertEquals(80f, pastedEllipse.y)

        assertEquals(listOf(rectangle, inner), outer.members)
    }

    @Test
    fun `repeated group pastes generate fresh identities`() {
        val rectangle = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )

        val group = EditorGroup(members = listOf(rectangle))
        val clipboard = EditorNodeClipboard()

        clipboard.copyMembers(
            members = listOf(group),
            selectedIds = setOf(group.id),
        )

        val first = clipboard.pasteMembers().single() as EditorGroup
        val second = clipboard.pasteMembers().single() as EditorGroup

        assertNotEquals(first.id, second.id)
        assertNotEquals(
            first.members.single().id,
            second.members.single().id,
        )

        val firstRectangle = first.members.single() as EditorRectangle
        val secondRectangle = second.members.single() as EditorRectangle

        assertEquals(20f, firstRectangle.x)
        assertEquals(40f, secondRectangle.x)
    }

    @Test
    fun `legacy node clipboard remains compatible`() {
        val rectangle = EditorRectangle(
            x = 10f, y = 10f,
            width = 20f, height = 20f,
        )

        val clipboard = EditorNodeClipboard()

        clipboard.copy(
            nodes = listOf(rectangle),
            selectedIds = setOf(rectangle.id),
        )

        val pasted = clipboard.paste().single()

        assertNotEquals(rectangle.id, pasted.id)
        assertEquals(30f, pasted.x)
        assertEquals(30f, pasted.y)
    }


    @Test
    fun `deleting selected group removes its entire subtree`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )
        val c = EditorRectangle(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(b))
        val outer = EditorGroup(members = listOf(a, inner))

        val original = EditorDocument(members = listOf(outer, c))
        val state = EditorDocumentState(original)

        state.selectMember(outer.id)
        state.deleteSelectedMembers()

        assertEquals(listOf(c), state.document.members)
        assertTrue(state.selectedMemberIds.isEmpty())

        state.undo()
        assertEquals(original, state.document)

        state.redo()
        assertEquals(listOf(c), state.document.members)
    }

    @Test
    fun `duplicating selected group preserves nested hierarchy`() {
        val a = EditorRectangle(
            x = 10f, y = 20f,
            width = 20f, height = 20f,
        )
        val b = EditorEllipse(
            x = 50f, y = 60f,
            width = 20f, height = 20f,
        )

        val inner = EditorGroup(members = listOf(b))
        val outer = EditorGroup(members = listOf(a, inner))

        val state = EditorDocumentState(
            EditorDocument(members = listOf(outer))
        )

        state.selectMember(outer.id)
        state.duplicateSelectedMembers()

        val duplicate = state.document.members[1] as EditorGroup
        val duplicateInner = duplicate.members[1] as EditorGroup
        val duplicateA = duplicate.members[0] as EditorRectangle
        val duplicateB = duplicateInner.members.single() as EditorEllipse

        assertNotEquals(outer.id, duplicate.id)
        assertNotEquals(inner.id, duplicateInner.id)
        assertNotEquals(a.id, duplicateA.id)
        assertNotEquals(b.id, duplicateB.id)

        assertEquals(30f, duplicateA.x)
        assertEquals(40f, duplicateA.y)
        assertEquals(70f, duplicateB.x)
        assertEquals(80f, duplicateB.y)

        assertEquals(setOf(duplicate.id), state.selectedMemberIds)
        assertEquals(listOf(outer), state.document.members.take(1))
    }

    @Test
    fun `grouping selected groups preserves their identities`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )

        val first = EditorGroup(members = listOf(a))
        val second = EditorGroup(members = listOf(b))

        val state = EditorDocumentState(
            EditorDocument(members = listOf(first, second))
        )

        state.selectMembers(setOf(first.id, second.id))

        assertTrue(state.groupSelectedMembers())

        val combined = state.document.members.single() as EditorGroup

        assertEquals(listOf(first, second), combined.members)
        assertEquals(setOf(combined.id), state.selectedMemberIds)
        assertEquals(listOf(a, b), state.nodes)

        state.undo()
        assertEquals(listOf(first, second), state.document.members)
    }

    @Test
    fun `reordering selected group moves it as one member`() {
        val a = EditorRectangle(
            x = 0f, y = 0f,
            width = 20f, height = 20f,
        )
        val b = EditorEllipse(
            x = 30f, y = 0f,
            width = 20f, height = 20f,
        )
        val c = EditorRectangle(
            x = 60f, y = 0f,
            width = 20f, height = 20f,
        )

        val group = EditorGroup(members = listOf(a, b))
        val state = EditorDocumentState(
            EditorDocument(members = listOf(group, c))
        )

        state.selectMember(group.id)

        assertTrue(state.canBringSelectionForward)

        state.bringSelectionForward()

        assertEquals(
            listOf(c, group),
            state.document.members,
        )

        assertEquals(listOf(a, b), group.members)

        state.undo()

        assertEquals(
            listOf(group, c),
            state.document.members,
        )
    }

    @Test
    fun `copying and pasting selected group preserves structure`() {
        val rectangle = EditorRectangle(
            x = 10f, y = 20f,
            width = 30f, height = 40f,
        )

        val group = EditorGroup(members = listOf(rectangle))
        val state = EditorDocumentState(
            EditorDocument(members = listOf(group))
        )

        state.selectMember(group.id)
        state.copySelectedMembers()
        state.pasteMembers()

        val pasted = state.document.members.last() as EditorGroup
        val pastedRectangle = pasted.members.single() as EditorRectangle

        assertNotEquals(group.id, pasted.id)
        assertNotEquals(rectangle.id, pastedRectangle.id)
        assertEquals(30f, pastedRectangle.x)
        assertEquals(40f, pastedRectangle.y)
        assertEquals(setOf(pasted.id), state.selectedMemberIds)
    }


    @Test
    fun `text defaults to zero rotation`() {
        val text = EditorText(
            x = 10f,
            y = 20f,
            text = "Hello",
        )

        assertEquals(0f, text.rotation)
    }

    @Test
    fun `text rotation can be updated immutably`() {
        val text = EditorText(
            x = 10f,
            y = 20f,
            text = "Hello",
        )

        val rotated = text.copy(
            rotation = (PI / 4).toFloat(),
        )

        assertEquals(
            (PI / 4).toFloat(),
            rotated.rotation,
        )
        assertEquals(text.id, rotated.id)
        assertEquals(text.text, rotated.text)
        assertEquals(0f, text.rotation)
    }

    @Test
    fun `text rejects non finite rotation`() {
        assertFailsWith<IllegalArgumentException> {
            EditorText(
                x = 0f,
                y = 0f,
                text = "Hello",
                rotation = Float.NaN,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            EditorText(
                x = 0f,
                y = 0f,
                text = "Hello",
                rotation = Float.POSITIVE_INFINITY,
            )
        }
    }


    @Test
    fun `text rotation survives document DTO round trip`() {
        val original = EditorText(
            x = 10f,
            y = 20f,
            text = "Rotated text",
            rotation = 1.25f,
        )

        val document = EditorDocument(
            members = listOf(original),
        )

        val restored = document.toDto()
            .toDocument()
            .nodes
            .single() as EditorText

        assertEquals(original, restored)
    }

    @Test
    fun `legacy text DTO defaults to zero rotation`() {
        val original = TextDto(
            id = Uuid.random().toString(),
            x = 10f,
            y = 20f,
            style = StyleDto(
                fillColor = null,
                stroke = null,
            ),
            text = "Old document",
            fontSize = 24f,
        )

        val json = Json.encodeToString(original)

        assertFalse("\"rotation\"" in json)

        val restored = Json.decodeFromString<TextDto>(json)

        assertEquals(0f, restored.rotation)
    }


    @Test
    fun `collective rotation preserves node identity and shape orientation`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 0f,
            width = 20f,
            height = 20f,
            rotation = 0.25f,
        )

        val rotated = listOf<EditorNode>(rectangle)
            .rotatedNodesTogether(
                angle = (PI / 2).toFloat(),
                center = Offset.Zero,
                textBounds = { error("No text expected") },
            )
            .single() as EditorRectangle

        assertEquals(rectangle.id, rotated.id)
        assertEquals(-20f, rotated.x, 0.001f)
        assertEquals(20f, rotated.y, 0.001f)
        assertEquals(
            rectangle.rotation + (PI / 2).toFloat(),
            rotated.rotation,
            0.001f,
        )
    }

    @Test
    fun `collective rotation uses measured text centre`() {
        val text = EditorText(
            x = 10f,
            y = 20f,
            text = "Hello",
            rotation = 0.3f,
        )

        val rotated = listOf<EditorNode>(text)
            .rotatedNodesTogether(
                angle = (PI / 2).toFloat(),
                center = Offset.Zero,
                textBounds = {
                    Rect(10f, 20f, 50f, 40f)
                },
            )
            .single() as EditorText

        assertEquals(text.id, rotated.id)
        assertEquals(-50f, rotated.x, 0.001f)
        assertEquals(20f, rotated.y, 0.001f)
        assertEquals(
            text.rotation + (PI / 2).toFloat(),
            rotated.rotation,
            0.001f,
        )
    }

    @Test
    fun `collective rotation transforms freehand path points`() {
        val path = EditorPath(
            x = 10f,
            y = 0f,
            points = listOf(
                Offset.Zero,
                Offset(10f, 0f),
            ),
        )

        val rotated = listOf<EditorNode>(path)
            .rotatedNodesTogether(
                angle = (PI / 2).toFloat(),
                center = Offset.Zero,
                textBounds = { error("No text expected") },
            )
            .single() as EditorPath

        assertEquals(path.id, rotated.id)
        assertEquals(0f, rotated.x, 0.001f)
        assertEquals(10f, rotated.y, 0.001f)
        assertEquals(0f, rotated.points[1].x, 0.001f)
        assertEquals(10f, rotated.points[1].y, 0.001f)
    }

    @Test
    fun `zero collective rotation preserves original nodes`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 10f,
                y = 20f,
                width = 40f,
                height = 30f,
            ),
            EditorText(
                x = 50f,
                y = 60f,
                text = "Hello",
            ),
        )

        val rotated = nodes.rotatedNodesTogether(
            angle = 0f,
            center = Offset.Zero,
            textBounds = { error("Should not measure text") },
        )

        assertEquals(nodes, rotated)
    }

    @Test
    fun `zero rotation preserves rectangle bounds`() {
        val bounds = Rect(
            left = 10f,
            top = 20f,
            right = 50f,
            bottom = 40f,
        )

        assertEquals(bounds, bounds.rotatedVisualBounds(0f))
    }

    @Test
    fun `quarter turn swaps rectangular extents`() {
        val bounds = Rect(
            left = 10f,
            top = 20f,
            right = 50f,
            bottom = 40f,
        )

        val rotated = bounds.rotatedVisualBounds(
            (PI / 2).toFloat()
        )

        assertEquals(20f, rotated.left, 0.001f)
        assertEquals(10f, rotated.top, 0.001f)
        assertEquals(40f, rotated.right, 0.001f)
        assertEquals(50f, rotated.bottom, 0.001f)
    }

    @Test
    fun `rotated visual bounds enclose transformed corners`() {
        val bounds = Rect(10f, 20f, 50f, 40f)
        val angle = (PI / 4).toFloat()

        val visual = bounds.rotatedVisualBounds(angle)

        val corners = listOf(
            bounds.topLeft,
            Offset(bounds.right, bounds.top),
            bounds.bottomRight,
            Offset(bounds.left, bounds.bottom),
        )

        for (corner in corners) {
            val rotated = corner.rotatedAround(
                center = bounds.center,
                angle = angle,
            )

            assertTrue(rotated.x >= visual.left - 0.001f)
            assertTrue(rotated.x <= visual.right + 0.001f)
            assertTrue(rotated.y >= visual.top - 0.001f)
            assertTrue(rotated.y <= visual.bottom + 0.001f)
        }
    }

    @Test
    fun `rotated visual bounds reject non finite angles`() {
        val bounds = Rect(0f, 0f, 20f, 10f)

        assertFailsWith<IllegalArgumentException> {
            bounds.rotatedVisualBounds(Float.NaN)
        }

        assertFailsWith<IllegalArgumentException> {
            bounds.rotatedVisualBounds(Float.POSITIVE_INFINITY)
        }
    }


    @Test
    fun `nodes with identical rotations share an orientation`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0.5f,
            ),
            EditorText(
                x = 30f,
                y = 20f,
                text = "Hello",
                rotation = 0.5f,
            ),
        )

        assertEquals(0.5f, nodes.commonRotationOrNull())
    }

    @Test
    fun `orientations differing by a full turn are equivalent`() {
        val angle = (PI / 6).toFloat()
        val fullTurn = (2 * PI).toFloat()

        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = angle,
            ),
            EditorText(
                x = 30f,
                y = 0f,
                text = "Hello",
                rotation = angle + fullTurn,
            ),
        )

        assertEquals(
            angle,
            nodes.commonRotationOrNull()!!,
            0.0001f,
        )
    }

    @Test
    fun `different orientations have no common rotation`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0f,
            ),
            EditorRectangle(
                x = 30f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = (PI / 2).toFloat(),
            ),
        )

        assertNull(nodes.commonRotationOrNull())
    }

    @Test
    fun `freehand paths have no defined common orientation`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
            ),
            EditorPath(
                x = 30f,
                y = 0f,
                points = listOf(
                    Offset.Zero,
                    Offset(10f, 10f),
                ),
            ),
        )

        assertNull(nodes.commonRotationOrNull())
    }

    @Test
    fun `empty selection has no common orientation`() {
        val nodes = listOf<EditorNode>()

        assertNull(nodes.commonRotationOrNull())
    }

    @Test
    fun `common orientation respects angular tolerance`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 1f,
            ),
            EditorRectangle(
                x = 30f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 1.00005f,
            ),
        )

        assertNotNull(nodes.commonRotationOrNull())

        assertNull(
            nodes.commonRotationOrNull(
                tolerance = 0.00001f,
            )
        )
    }

    @Test
    fun `unrotated group bounds match enclosing node bounds`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 10f,
                y = 20f,
                width = 30f,
                height = 20f,
            ),
            EditorRectangle(
                x = 60f,
                y = 40f,
                width = 20f,
                height = 30f,
            ),
        )

        val bounds = nodes.orientedGroupBoundsOrNull {
            it.bounds
        }

        assertEquals(Rect(10f, 20f, 80f, 70f), bounds)
    }

    @Test
    fun `quarter turned nodes use shared rotated coordinates`() {
        val angle = (PI / 2).toFloat()

        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 10f,
                y = 20f,
                width = 20f,
                height = 10f,
                rotation = angle,
            ),
            EditorRectangle(
                x = 40f,
                y = 20f,
                width = 20f,
                height = 10f,
                rotation = angle,
            ),
        )

        val bounds = nodes.orientedGroupBoundsOrNull {
            it.bounds
        }!!

        assertEquals(15f, bounds.left, 0.001f)
        assertEquals(-55f, bounds.top, 0.001f)
        assertEquals(35f, bounds.right, 0.001f)
        assertEquals(-15f, bounds.bottom, 0.001f)
    }

    @Test
    fun `mixed rotations have no oriented group bounds`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0f,
            ),
            EditorRectangle(
                x = 30f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0.5f,
            ),
        )

        assertNull(
            nodes.orientedGroupBoundsOrNull { it.bounds }
        )
    }

    @Test
    fun `oriented group bounds use supplied text layout bounds`() {
        val nodes: List<EditorNode> = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 10f,
                height = 10f,
            ),
            EditorText(
                x = 20f,
                y = 0f,
                text = "Hello",
            ),
        )

        val bounds = nodes.orientedGroupBoundsOrNull { node ->
            if (node is EditorText) {
                Rect(20f, 0f, 70f, 20f)
            } else {
                node.bounds
            }
        }

        assertEquals(Rect(0f, 0f, 70f, 20f), bounds)
    }


    @Test
    fun `unrotated group resize scales points normally`() {
        val transform = orientedGroupResizeTransform(
            source = Rect(0f, 0f, 100f, 50f),
            rotation = 0f,
            handle = ResizeHandle.SE,
            worldDelta = Offset(50f, 25f),
        )

        assertEquals(1.5f, transform.scaleX, 0.001f)
        assertEquals(1.5f, transform.scaleY, 0.001f)

        val point = transform.transform(Offset(40f, 20f))

        assertEquals(60f, point.x, 0.001f)
        assertEquals(30f, point.y, 0.001f)
    }

    @Test
    fun `quarter turned group uses rotated drag axes`() {
        val angle = (PI / 2).toFloat()

        val transform = orientedGroupResizeTransform(
            source = Rect(0f, 0f, 100f, 50f),
            rotation = angle,
            handle = ResizeHandle.E,
            worldDelta = Offset(0f, 20f),
        )

        assertEquals(120f, transform.target.right, 0.001f)
        assertEquals(50f, transform.target.bottom, 0.001f)

        // Local (100, 25) becomes (120, 25).
        // Rotated into world coordinates: (-25, 120).
        val point = transform.transform(Offset(-25f, 100f))

        assertEquals(-25f, point.x, 0.001f)
        assertEquals(120f, point.y, 0.001f)
    }

    @Test
    fun `oriented group resize preserves fixed opposite edge`() {
        val transform = orientedGroupResizeTransform(
            source = Rect(10f, 20f, 110f, 70f),
            rotation = 0f,
            handle = ResizeHandle.E,
            worldDelta = Offset(30f, 0f),
        )

        val fixedPoint = transform.transform(Offset(10f, 45f))

        assertEquals(10f, fixedPoint.x, 0.001f)
        assertEquals(45f, fixedPoint.y, 0.001f)
    }

    @Test
    fun `zero drag leaves oriented group geometry unchanged`() {
        val angle = (PI / 6).toFloat()

        val transform = orientedGroupResizeTransform(
            source = Rect(10f, 20f, 110f, 70f),
            rotation = angle,
            handle = ResizeHandle.NE,
            worldDelta = Offset.Zero,
        )

        val originalPoint = Offset(47f, 83f)
        val result = transform.transform(originalPoint)

        assertEquals(originalPoint.x, result.x, 0.001f)
        assertEquals(originalPoint.y, result.y, 0.001f)
    }

    @Test
    fun `oriented group resize rejects invalid rotation`() {
        assertFailsWith<IllegalArgumentException> {
            orientedGroupResizeTransform(
                source = Rect(0f, 0f, 100f, 50f),
                rotation = Float.NaN,
                handle = ResizeHandle.SE,
                worldDelta = Offset(10f, 10f),
            )
        }
    }


    @Test
    fun `negative quarter turn maps drag into local axes`() {
        val transform = orientedGroupResizeTransform(
            source = Rect(0f, 0f, 100f, 50f),
            rotation = (-PI / 2).toFloat(),
            handle = ResizeHandle.E,
            worldDelta = Offset(0f, -20f),
        )

        assertEquals(120f, transform.target.right, 0.001f)
        assertEquals(50f, transform.target.bottom, 0.001f)
    }

    @Test
    fun `full turn rotation produces equivalent resize`() {
        val source = Rect(10f, 20f, 110f, 70f)
        val delta = Offset(25f, 15f)

        val first = orientedGroupResizeTransform(
            source = source,
            rotation = 0f,
            handle = ResizeHandle.SE,
            worldDelta = delta,
        )

        val second = orientedGroupResizeTransform(
            source = source,
            rotation = (2 * PI).toFloat(),
            handle = ResizeHandle.SE,
            worldDelta = delta,
        )

        assertEquals(first.target.left, second.target.left, 0.001f)
        assertEquals(first.target.top, second.target.top, 0.001f)
        assertEquals(first.target.right, second.target.right, 0.001f)
        assertEquals(first.target.bottom, second.target.bottom, 0.001f)
    }

    @Test
    fun `proportional group resize preserves aspect ratio`() {
        val transform = orientedGroupResizeTransform(
            source = Rect(0f, 0f, 100f, 50f),
            rotation = 0f,
            handle = ResizeHandle.SE,
            worldDelta = Offset(50f, 25f),
            proportional = true,
        )

        assertEquals(1.5f, transform.scaleX, 0.001f)
        assertEquals(1.5f, transform.scaleY, 0.001f)

        assertEquals(
            transform.source.width / transform.source.height,
            transform.target.width / transform.target.height,
            0.001f,
        )
    }

    @Test
    fun `group resize respects minimum dimensions`() {
        val transform = orientedGroupResizeTransform(
            source = Rect(0f, 0f, 100f, 50f),
            rotation = 0f,
            handle = ResizeHandle.SE,
            worldDelta = Offset(-200f, -200f),
            minimumSize = 10f,
        )

        assertEquals(10f, transform.target.width, 0.001f)
        assertEquals(10f, transform.target.height, 0.001f)
    }

    @Test
    fun `rotated group resize preserves opposite corner`() {
        val angle = (PI / 4).toFloat()
        val source = Rect(10f, 20f, 110f, 70f)

        val transform = orientedGroupResizeTransform(
            source = source,
            rotation = angle,
            handle = ResizeHandle.SE,
            worldDelta = Offset(30f, 20f),
        )

        val fixedCorner = source.topLeft.rotatedAround(
            center = Offset.Zero,
            angle = angle,
        )

        val transformedCorner = transform.transform(fixedCorner)

        assertEquals(
            fixedCorner.x,
            transformedCorner.x,
            0.001f,
        )
        assertEquals(
            fixedCorner.y,
            transformedCorner.y,
            0.001f,
        )
    }


    @Test
    fun `oriented resize scales aligned shapes`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
            ),
            EditorEllipse(
                x = 40f,
                y = 20f,
                width = 20f,
                height = 10f,
            ),
        )

        val transform = orientedGroupResizeTransform(
            source = Rect(0f, 0f, 60f, 30f),
            rotation = 0f,
            handle = ResizeHandle.SE,
            worldDelta = Offset(60f, 30f),
        )

        val result = shapes.resizedWith(transform)

        assertEquals(40f, result[0].width, 0.001f)
        assertEquals(20f, result[0].height, 0.001f)
        assertEquals(80f, result[1].x, 0.001f)
        assertEquals(40f, result[1].y, 0.001f)
        assertEquals(shapes.map { it.id }, result.map { it.id })
    }

    @Test
    fun `oriented resize preserves nonzero common rotation`() {
        val angle = (PI / 4).toFloat()

        val shapes = listOf(
            EditorRectangle(
                x = 10f,
                y = 20f,
                width = 20f,
                height = 10f,
                rotation = angle,
            ),
            EditorEllipse(
                x = 60f,
                y = 20f,
                width = 20f,
                height = 10f,
                rotation = angle,
            ),
        )

        val transform = OrientedGroupResizeTransform(
            source = Rect(-100f, -100f, 100f, 100f),
            target = Rect(-200f, -200f, 200f, 200f),
            rotation = angle,
        )

        val result = shapes.resizedWith(transform)

        assertEquals(40f, result[0].width, 0.001f)
        assertEquals(20f, result[0].height, 0.001f)
        assertEquals(angle, result[0].rotation, 0.001f)
        assertEquals(angle, result[1].rotation, 0.001f)

        for (index in shapes.indices) {
            val expectedCenter = transform.transform(
                shapes[index].bounds.center
            )

            val actualCenter = result[index].bounds.center

            assertEquals(expectedCenter.x, actualCenter.x, 0.001f)
            assertEquals(expectedCenter.y, actualCenter.y, 0.001f)
        }
    }

    @Test
    fun `oriented resize rejects inconsistent rotation`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0f,
            ),
            EditorEllipse(
                x = 30f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0.5f,
            ),
        )

        val transform = OrientedGroupResizeTransform(
            source = Rect(0f, 0f, 50f, 10f),
            target = Rect(0f, 0f, 100f, 20f),
            rotation = 0f,
        )

        assertFailsWith<IllegalStateException> {
            shapes.resizedWith(transform)
        }
    }

    @Test
    fun `oriented resize refuses undersized descendants`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 5f,
                height = 5f,
            ),
        )

        val transform = OrientedGroupResizeTransform(
            source = Rect(0f, 0f, 10f, 10f),
            target = Rect(0f, 0f, 1f, 1f),
            rotation = 0f,
        )

        assertFailsWith<IllegalStateException> {
            shapes.resizedWith(
                transform = transform,
                minimumSize = 1f,
            )
        }
    }


    @Test
    fun `group resize clamps to smallest descendant width`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 100f,
                height = 20f,
            ),
            EditorRectangle(
                x = 120f,
                y = 0f,
                width = 10f,
                height = 20f,
            ),
        )

        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.E,
            worldDelta = Offset(-1000f, 0f),
        )!!

        assertEquals(0.1f, transform.scaleX, 0.001f)
        assertEquals(1f, transform.scaleY, 0.001f)
        assertEquals(
            transform.source.left,
            transform.target.left,
            0.001f,
        )

        val resized = shapes.resizedWith(transform)

        assertEquals(10f, resized[0].width, 0.001f)
        assertEquals(1f, resized[1].width, 0.001f)
    }

    @Test
    fun `left handle preserves opposite edge when clamped`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 10f,
                height = 20f,
            ),
            EditorRectangle(
                x = 30f,
                y = 0f,
                width = 20f,
                height = 20f,
            ),
        )

        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.W,
            worldDelta = Offset(1000f, 0f),
        )!!

        assertEquals(
            transform.source.right,
            transform.target.right,
            0.001f,
        )
        assertEquals(0.1f, transform.scaleX, 0.001f)
    }

    @Test
    fun `proportional group resize respects both minimum scales`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 10f,
                height = 100f,
            ),
            EditorRectangle(
                x = 30f,
                y = 0f,
                width = 100f,
                height = 5f,
            ),
        )

        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.SE,
            worldDelta = Offset(-1000f, -1000f),
            proportional = true,
        )!!

        assertEquals(0.2f, transform.scaleX, 0.001f)
        assertEquals(0.2f, transform.scaleY, 0.001f)

        val resized = shapes.resizedWith(transform)

        assertTrue(resized.all {
            it.width >= 1f - 0.0001f &&
                    it.height >= 1f - 0.0001f
        })
    }

    @Test
    fun `mixed orientation group cannot construct resize transform`() {
        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0f,
            ),
            EditorRectangle(
                x = 40f,
                y = 0f,
                width = 20f,
                height = 10f,
                rotation = 0.5f,
            ),
        )

        assertNull(
            shapes.constrainedOrientedResizeTransform(
                handle = ResizeHandle.SE,
                worldDelta = Offset(10f, 10f),
            )
        )
    }

    @Test
    fun `rotated group resize clamps in local axes`() {
        val angle = (PI / 2).toFloat()

        val shapes = listOf(
            EditorRectangle(
                x = 0f,
                y = 0f,
                width = 10f,
                height = 20f,
                rotation = angle,
            ),
            EditorRectangle(
                x = 30f,
                y = 0f,
                width = 20f,
                height = 20f,
                rotation = angle,
            ),
        )

        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.E,
            worldDelta = Offset(0f, -1000f),
        )!!

        assertEquals(0.1f, transform.scaleX, 0.001f)
        assertEquals(angle, transform.rotation, 0.001f)

        val resized = shapes.resizedWith(transform)

        assertEquals(1f, resized[0].width, 0.001f)
        assertEquals(2f, resized[1].width, 0.001f)
    }


    @Test
    fun `unrotated group resize handles use padded bounds`() {
        val positions = Rect(0f, 0f, 100f, 50f)
            .orientedResizeHandleScreenPositions(
                rotation = 0f,
                camera = EditorCamera(),
                padding = 8f,
            )

        assertEquals(
            Offset(108f, 25f),
            positions[ResizeHandle.E],
        )
        assertEquals(
            Offset(-8f, 25f),
            positions[ResizeHandle.W],
        )
        assertEquals(
            Offset(108f, 58f),
            positions[ResizeHandle.SE],
        )
    }

    @Test
    fun `quarter turned group rotates resize handles`() {
        val positions = Rect(0f, 0f, 100f, 50f)
            .orientedResizeHandleScreenPositions(
                rotation = (PI / 2).toFloat(),
                camera = EditorCamera(),
                padding = 8f,
            )

        val east = positions.getValue(ResizeHandle.E)
        val north = positions.getValue(ResizeHandle.N)

        assertEquals(-25f, east.x, 0.001f)
        assertEquals(108f, east.y, 0.001f)

        assertEquals(8f, north.x, 0.001f)
        assertEquals(50f, north.y, 0.001f)
    }

    @Test
    fun `oriented resize handle padding stays constant with zoom`() {
        val bounds = Rect(0f, 0f, 100f, 50f)

        val first = bounds.orientedResizeHandleScreenPositions(
            rotation = 0f,
            camera = EditorCamera(zoom = 1f),
            padding = 8f,
        )

        val second = bounds.orientedResizeHandleScreenPositions(
            rotation = 0f,
            camera = EditorCamera(zoom = 2f),
            padding = 8f,
        )

        assertEquals(
            8f,
            first.getValue(ResizeHandle.E).x - 100f,
            0.001f,
        )

        assertEquals(
            8f,
            second.getValue(ResizeHandle.E).x - 200f,
            0.001f,
        )
    }

    @Test
    fun `oriented resize handle hit testing finds correct handle`() {
        val bounds = Rect(0f, 0f, 100f, 50f)
        val angle = (PI / 2).toFloat()

        val positions = bounds.orientedResizeHandleScreenPositions(
            rotation = angle,
            camera = EditorCamera(),
            padding = 8f,
        )

        val east = positions.getValue(ResizeHandle.E)

        assertEquals(
            ResizeHandle.E,
            bounds.orientedResizeHandleAt(
                pointer = east + Offset(3f, 2f),
                rotation = angle,
                camera = EditorCamera(),
                padding = 8f,
                radius = 10f,
            ),
        )

        assertNull(
            bounds.orientedResizeHandleAt(
                pointer = Offset(1000f, 1000f),
                rotation = angle,
                camera = EditorCamera(),
                padding = 8f,
                radius = 10f,
            )
        )
    }

    @Test
    fun `oriented resize handle positions respect camera translation`() {
        val camera = EditorCamera(
            position = Offset(10f, 20f),
            zoom = 2f,
        )

        val positions = Rect(0f, 0f, 100f, 50f)
            .orientedResizeHandleScreenPositions(
                rotation = 0f,
                camera = camera,
                padding = 8f,
            )

        val east = positions.getValue(ResizeHandle.E)

        assertEquals(188f, east.x, 0.001f)
        assertEquals(10f, east.y, 0.001f)
    }

    @Test
    fun `oriented rotation handle extends from north resize handle`() {
        val bounds = Rect(0f, 0f, 100f, 50f)
        val angle = (PI / 2).toFloat()
        val camera = EditorCamera()

        val north = bounds.orientedResizeHandleScreenPositions(
            rotation = angle,
            camera = camera,
            padding = 8f,
        ).getValue(ResizeHandle.N)

        val position = bounds.orientedRotationHandlePosition(
            rotation = angle,
            camera = camera,
            padding = 8f,
            offset = 28f,
        )

        assertEquals(north.x + 28f, position.x, 0.001f)
        assertEquals(north.y, position.y, 0.001f)
    }


    @Test
    fun `group resize commits as a single undoable operation`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 40f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val group = EditorGroup(members = listOf(first, second))
        val original = EditorDocument(members = listOf(group))
        val state = EditorDocumentState(original)

        state.selectMember(group.id)

        val shapes = state.selectedNodes.filterIsInstance<EditorShape>()
        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.SE,
            worldDelta = Offset(60f, 10f),
        )!!

        val resized = shapes.resizedWith(transform)
        state.updateNodes(resized)

        val updated = state.document

        assertNotEquals(original, updated)
        assertEquals(setOf(group.id), state.selectedMemberIds)
        assertEquals(group.id, (updated.members.single() as EditorGroup).id)
        assertEquals(listOf(first.id, second.id), updated.nodes.map { it.id })

        state.undo()

        assertEquals(original, state.document)
        assertTrue(state.canRedo)

        state.redo()

        assertEquals(updated, state.document)
        assertEquals(setOf(group.id), state.selectedMemberIds)
    }

    @Test
    fun `nested group resize preserves hierarchy and sibling nodes`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val second = EditorRectangle(
            x = 40f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val sibling = EditorEllipse(
            x = 100f,
            y = 50f,
            width = 30f,
            height = 20f,
        )

        val inner = EditorGroup(members = listOf(first, second))
        val outer = EditorGroup(members = listOf(inner, sibling))
        val original = EditorDocument(members = listOf(outer))
        val state = EditorDocumentState(original)

        state.selectMember(inner.id)

        val shapes = state.selectedNodes.filterIsInstance<EditorShape>()
        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.E,
            worldDelta = Offset(30f, 0f),
        )!!

        state.updateNodes(shapes.resizedWith(transform))

        val updatedOuter = state.document.members.single() as EditorGroup
        val updatedInner = updatedOuter.members.first() as EditorGroup

        assertEquals(outer.id, updatedOuter.id)
        assertEquals(inner.id, updatedInner.id)
        assertEquals(listOf(first.id, second.id), updatedInner.flattenedNodes().map { it.id })
        assertEquals(sibling, updatedOuter.members.last())

        assertNotEquals(first, updatedInner.flattenedNodes().first())
        assertEquals(setOf(inner.id), state.selectedMemberIds)

        state.undo()
        assertEquals(original, state.document)

        state.redo()
        assertEquals(updatedOuter, state.document.members.single())
    }

    @Test
    fun `unchanged group resize does not create history entry`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val second = EditorRectangle(
            x = 40f,
            y = 0f,
            width = 20f,
            height = 10f,
        )

        val group = EditorGroup(members = listOf(first, second))
        val original = EditorDocument(members = listOf(group))
        val state = EditorDocumentState(original)

        state.selectMember(group.id)

        val shapes = state.selectedNodes.filterIsInstance<EditorShape>()
        val transform = shapes.constrainedOrientedResizeTransform(
            handle = ResizeHandle.SE,
            worldDelta = Offset.Zero,
        )!!

        state.updateNodes(shapes.resizedWith(transform))

        assertEquals(original, state.document)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
    }


    @Test
    fun `nested groups survive serialization round trip`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val ellipse = EditorEllipse(
            x = 150f,
            y = 20f,
            width = 50f,
            height = 50f,
        )

        val inner = EditorGroup(
            members = listOf(rectangle),
        )

        val outer = EditorGroup(
            members = listOf(inner, ellipse),
        )

        val original = EditorDocument(
            members = listOf(outer),
        )

        val encoded = DocumentCodec.encode(original)
        val decoded = DocumentCodec.decode(encoded)

        assertEquals(original, decoded)

        val decodedOuter = decoded.members.single() as EditorGroup
        val decodedInner = decodedOuter.members.first() as EditorGroup

        assertEquals(outer.id, decodedOuter.id)
        assertEquals(inner.id, decodedInner.id)
        assertEquals(rectangle, decodedInner.members.single())
        assertEquals(ellipse, decodedOuter.members.last())
    }


    @Test
    fun `version one flat document remains readable`() {
        val original = EditorDocument(
            members = listOf(
                EditorRectangle(
                    id = Uuid.parse(
                        "123e4567-e89b-12d3-a456-426614174000"
                    ),
                    x = 10f,
                    y = 20f,
                    width = 100f,
                    height = 50f,
                    style = NodeStyle(
                        fill = FillStyle.None,
                        stroke = null,
                    ),
                )
            )
        )

        val legacyJson = """
        {
          "version": 1,
          "nodes": [
            {
              "type": "rectangle",
              "id": "123e4567-e89b-12d3-a456-426614174000",
              "x": 10.0,
              "y": 20.0,
              "width": 100.0,
              "height": 50.0,
              "rotation": 0.0,
              "style": {
                "fillColor": null,
                "stroke": null
              }
            }
          ]
        }
    """.trimIndent()

        val decoded = DocumentCodec.decode(legacyJson)

        assertEquals(original, decoded)
    }


    @Test
    fun `document rejects duplicate node IDs across nested groups`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )

        val duplicate = rectangle.copy(
            x = 100f,
        )

        val document = EditorDocument(
            members = listOf(
                EditorGroup(
                    members = listOf(rectangle),
                ),
                EditorGroup(
                    members = listOf(duplicate),
                ),
            ),
        )

        val encoded = DocumentCodec.encode(document)

        assertFailsWith<IllegalArgumentException> {
            DocumentCodec.decode(encoded)
        }
    }

    @Test
    fun `document rejects duplicate group and node IDs`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )

        val group = EditorGroup(
            id = rectangle.id,
            members = listOf(rectangle),
        )

        val document = EditorDocument(
            members = listOf(group),
        )

        assertFailsWith<IllegalArgumentException> {
            DocumentCodec.decode(
                DocumentCodec.encode(document)
            )
        }
    }

    @Test
    fun `document accepts unique IDs throughout nested hierarchy`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )

        val second = EditorEllipse(
            x = 40f,
            y = 0f,
            width = 20f,
            height = 10f,
        )

        val inner = EditorGroup(
            members = listOf(first),
        )

        val outer = EditorGroup(
            members = listOf(inner, second),
        )

        val original = EditorDocument(
            members = listOf(outer),
        )

        val decoded = DocumentCodec.decode(
            DocumentCodec.encode(original)
        )

        assertEquals(original, decoded)
    }


    @Test
    fun `unsupported document versions are rejected`() {
        val unsupported = DocumentDto(
            version = 99,
            members = listOf(),
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            unsupported.toDocument()
        }

        assertTrue(
            exception.message?.contains("Unsupported document version") == true
        )
    }

    @Test
    fun `malformed group UUID is rejected`() {
        val document = DocumentDto(
            version = 2,
            members = listOf(
                GroupDto(
                    id = "not-a-valid-uuid",
                    members = listOf(),
                )
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            document.toDocument()
        }
    }

    @Test
    fun `malformed nested node UUID is rejected`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 50f,
        )

        val document = EditorDocument(
            members = listOf(
                EditorGroup(
                    members = listOf(rectangle),
                )
            )
        )

        val encoded = DocumentCodec.encode(document)

        val corrupted = encoded.replace(
            rectangle.id.toString(),
            "invalid-node-id",
        )

        assertNotEquals(encoded, corrupted)

        assertFailsWith<IllegalArgumentException> {
            DocumentCodec.decode(corrupted)
        }
    }

    @Test
    fun `invalid group member structure is rejected`() {
        val malformedJson = """
        {
          "version": 2,
          "nodes": [
            {
              "type": "group",
              "id": "123e4567-e89b-12d3-a456-426614174000",
              "members": 42
            }
          ]
        }
    """.trimIndent()

        assertFailsWith<SerializationException> {
            DocumentCodec.decode(malformedJson)
        }
    }


    @Test
    fun `aligning groups preserves their internal arrangements`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 20f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 50f,
            y = 40f,
            width = 10f,
            height = 10f,
        )
        val third = EditorRectangle(
            x = 100f,
            y = 60f,
            width = 20f,
            height = 10f,
        )

        val firstGroup = EditorGroup(
            members = listOf(first, second),
        )
        val secondGroup = EditorGroup(
            members = listOf(third),
        )

        val updated = listOf(firstGroup, secondGroup)
            .alignedMemberNodes(
                alignment = NodeAlignment.LEFT,
                selectionBounds = { it.selectionBounds },
            )

        val updatedFirst = updated[0]
        val updatedSecond = updated[1]
        val updatedThird = updated[2]

        assertEquals(10f, updatedFirst.x, 0.001f)
        assertEquals(50f, updatedSecond.x, 0.001f)
        assertEquals(10f, updatedThird.x, 0.001f)

        // Internal relative spacing must be preserved.
        assertEquals(
            second.x - first.x,
            updatedSecond.x - updatedFirst.x,
            0.001f,
        )
    }

    @Test
    fun `aligning group and node treats each as one unit`() {
        val first = EditorRectangle(
            x = 20f,
            y = 10f,
            width = 20f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 60f,
            y = 30f,
            width = 20f,
            height = 10f,
        )
        val standalone = EditorRectangle(
            x = 120f,
            y = 50f,
            width = 20f,
            height = 10f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val updated = listOf(group, standalone)
            .alignedMemberNodes(
                alignment = NodeAlignment.LEFT,
                selectionBounds = { it.selectionBounds },
            )

        assertEquals(20f, updated[0].x, 0.001f)
        assertEquals(60f, updated[1].x, 0.001f)
        assertEquals(20f, updated[2].x, 0.001f)
    }

    @Test
    fun `aligning a single group does not alter its descendants`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 20f,
            height = 10f,
        )
        val second = EditorRectangle(
            x = 50f,
            y = 40f,
            width = 20f,
            height = 10f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val updated = listOf(group).alignedMemberNodes(
            alignment = NodeAlignment.LEFT,
            selectionBounds = { it.selectionBounds },
        )

        assertTrue(updated.isEmpty())
    }


    @Test
    fun `horizontal distribution treats groups as single units`() {
        val first = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val second = EditorRectangle(
            x = 30f,
            y = 0f,
            width = 10f,
            height = 10f,
        )
        val middle = EditorRectangle(
            x = 80f,
            y = 0f,
            width = 20f,
            height = 10f,
        )
        val last = EditorRectangle(
            x = 200f,
            y = 0f,
            width = 50f,
            height = 10f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val updated = listOf(group, middle, last)
            .distributedMemberNodes(
                distribution = NodeDistribution.HORIZONTAL,
                selectionBounds = { it.selectionBounds },
            ).associateBy { it.id }

        // Total span: 250
        // Widths: 40 + 20 + 50 = 110
        // Two gaps of 70 each.
        assertEquals(0f, updated.getValue(first.id).x, 0.001f)
        assertEquals(30f, updated.getValue(second.id).x, 0.001f)
        assertEquals(110f, updated.getValue(middle.id).x, 0.001f)
        assertEquals(200f, updated.getValue(last.id).x, 0.001f)

        assertEquals(
            second.x - first.x,
            updated.getValue(second.id).x -
                    updated.getValue(first.id).x,
            0.001f,
        )
    }

    @Test
    fun `vertical distribution preserves nested group arrangement`() {
        val first = EditorRectangle(
            x = 10f,
            y = 0f,
            width = 20f,
            height = 20f,
        )
        val second = EditorEllipse(
            x = 30f,
            y = 30f,
            width = 10f,
            height = 10f,
        )
        val middle = EditorRectangle(
            x = 50f,
            y = 70f,
            width = 20f,
            height = 20f,
        )
        val last = EditorRectangle(
            x = 80f,
            y = 200f,
            width = 20f,
            height = 40f,
        )

        val inner = EditorGroup(members = listOf(first, second))
        val outer = EditorGroup(members = listOf(inner))

        val updated = listOf(outer, middle, last)
            .distributedMemberNodes(
                distribution = NodeDistribution.VERTICAL,
                selectionBounds = { it.selectionBounds },
            ).associateBy { it.id }

        // Span: 240
        // Heights: 40 + 20 + 40 = 100
        // Gap: 70
        assertEquals(0f, updated.getValue(first.id).y, 0.001f)
        assertEquals(30f, updated.getValue(second.id).y, 0.001f)
        assertEquals(110f, updated.getValue(middle.id).y, 0.001f)
        assertEquals(200f, updated.getValue(last.id).y, 0.001f)

        assertEquals(
            second.y - first.y,
            updated.getValue(second.id).y -
                    updated.getValue(first.id).y,
            0.001f,
        )
    }

    @Test
    fun `distribution requires three selected logical members`() {
        val group = EditorGroup(
            members = listOf(
                EditorRectangle(
                    x = 0f,
                    y = 0f,
                    width = 10f,
                    height = 10f,
                ),
                EditorRectangle(
                    x = 30f,
                    y = 0f,
                    width = 10f,
                    height = 10f,
                ),
                EditorRectangle(
                    x = 60f,
                    y = 0f,
                    width = 10f,
                    height = 10f,
                ),
            ),
        )

        assertTrue(
            listOf(group).distributedMemberNodes(
                distribution = NodeDistribution.HORIZONTAL,
                selectionBounds = { it.selectionBounds },
            ).isEmpty()
        )
    }


    @Test
    fun `oriented selection returns bounds for aligned shapes`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 30f,
            height = 20f,
        )

        val second = EditorEllipse(
            x = 60f,
            y = 40f,
            width = 20f,
            height = 30f,
        )

        val selection = listOf<EditorNode>(first, second)
            .orientedShapeSelectionOrNull()

        assertNotNull(selection)
        assertEquals(0f, selection.rotation, 0.001f)
        assertEquals(10f, selection.bounds.left, 0.001f)
        assertEquals(20f, selection.bounds.top, 0.001f)
        assertEquals(80f, selection.bounds.right, 0.001f)
        assertEquals(70f, selection.bounds.bottom, 0.001f)
    }

    @Test
    fun `oriented selection supports common nonzero rotation`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 30f,
            height = 20f,
            rotation = 0.5f,
        )

        val second = EditorEllipse(
            x = 60f,
            y = 40f,
            width = 20f,
            height = 30f,
            rotation = 0.5f,
        )

        val selection = listOf<EditorNode>(first, second)
            .orientedShapeSelectionOrNull()

        assertNotNull(selection)
        assertEquals(0.5f, selection.rotation, 0.001f)

        val expectedBounds =
            listOf<EditorNode>(first, second)
                .orientedGroupBoundsOrNull {
                    (it as EditorShape).bounds
                }

        assertEquals(expectedBounds, selection.bounds)
    }

    @Test
    fun `oriented selection rejects incompatible rotations`() {
        val first = EditorRectangle(x = 10f, y = 10f, rotation = 0f, width = 10f, height = 10f)
        val second = EditorEllipse(x = 10f, y = 10f, rotation = 0.5f, width = 10f, height = 10f)

        val selection = listOf<EditorNode>(first, second)
            .orientedShapeSelectionOrNull()

        assertNull(selection)
    }

    @Test
    fun `oriented selection rejects empty selections`() {
        val selection = listOf<EditorNode>()
            .orientedShapeSelectionOrNull()

        assertNull(selection)
    }


    @Test
    fun `member bounds include nested descendants`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 30f,
            height = 40f,
        )

        val second = EditorEllipse(
            x = 100f,
            y = 80f,
            width = 20f,
            height = 30f,
        )

        val inner = EditorGroup(members = listOf(first))
        val outer = EditorGroup(members = listOf(inner, second))

        val bounds = outer.selectionBoundsOrNull {
            it.selectionBounds
        }

        assertEquals(
            Rect(
                left = 10f,
                top = 20f,
                right = 120f,
                bottom = 110f,
            ),
            bounds,
        )
    }

    @Test
    fun `empty group has no selection bounds`() {
        val group = EditorGroup(members = listOf())

        assertNull(
            group.selectionBoundsOrNull {
                it.selectionBounds
            }
        )
    }

    @Test
    fun `single node member bounds equal node bounds`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 40f,
            height = 50f,
        )

        assertEquals(
            rectangle.selectionBounds,
            rectangle.selectionBoundsOrNull {
                it.selectionBounds
            },
        )
    }


    @Test
    fun `translating a nested group moves all descendants`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 30f,
            height = 40f,
        )

        val second = EditorEllipse(
            x = 50f,
            y = 60f,
            width = 20f,
            height = 30f,
        )

        val inner = EditorGroup(members = listOf(first))
        val outer = EditorGroup(members = listOf(inner, second))

        val translated = outer.translatedNodesBy(
            Offset(15f, -10f)
        ).associateBy { it.id }

        assertEquals(25f, translated.getValue(first.id).x, 0.001f)
        assertEquals(10f, translated.getValue(first.id).y, 0.001f)

        assertEquals(65f, translated.getValue(second.id).x, 0.001f)
        assertEquals(50f, translated.getValue(second.id).y, 0.001f)
    }

    @Test
    fun `zero translation preserves descendant nodes`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 30f,
            y = 40f,
            width = 10f,
            height = 10f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val translated = group.translatedNodesBy(Offset.Zero)

        assertEquals(listOf(first, second), translated)
    }

    @Test
    fun `member translation preserves relative positions and identities`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorRectangle(
            x = 60f,
            y = 80f,
            width = 10f,
            height = 10f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val translated = group.translatedNodesBy(
            Offset(25f, 15f)
        )

        assertEquals(
            second.x - first.x,
            translated[1].x - translated[0].x,
            0.001f,
        )

        assertEquals(
            second.y - first.y,
            translated[1].y - translated[0].y,
            0.001f,
        )

        assertEquals(first.id, translated[0].id)
        assertEquals(second.id, translated[1].id)
    }


    @Test
    fun `duplicating nested member preserves parent group`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 30f,
            height = 40f,
        )

        val second = EditorEllipse(
            x = 60f,
            y = 70f,
            width = 20f,
            height = 20f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(group),
            ),
        )

        state.selectMember(first.id)
        state.duplicateSelectedMembers()

        // The duplicate must remain inside its original group.
        assertEquals(1, state.document.members.size)

        val updatedGroup =
            state.document.members.single() as EditorGroup

        assertEquals(group.id, updatedGroup.id)
        assertEquals(3, updatedGroup.members.size)

        val duplicate = updatedGroup.members[1] as EditorRectangle

        assertNotEquals(first.id, duplicate.id)
        assertEquals(30f, duplicate.x, 0.001f)
        assertEquals(40f, duplicate.y, 0.001f)

        assertEquals(first, updatedGroup.members[0])
        assertEquals(second, updatedGroup.members[2])

        assertEquals(setOf(duplicate.id), state.selectedMemberIds)

        state.undo()

        assertEquals(
            EditorDocument(members = listOf(group)),
            state.document,
        )
    }


    @Test
    fun `duplicating multiple nested siblings preserves their order`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 50f,
            y = 60f,
            width = 10f,
            height = 10f,
        )
        val third = EditorRectangle(
            x = 100f,
            y = 120f,
            width = 10f,
            height = 10f,
        )

        val inner = EditorGroup(
            members = listOf(first, second, third),
        )
        val outer = EditorGroup(
            members = listOf(inner),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(outer),
            ),
        )

        state.selectMembers(setOf(first.id, third.id))
        state.duplicateSelectedMembers()

        val updatedOuter =
            state.document.members.single() as EditorGroup

        val updatedInner =
            updatedOuter.members.single() as EditorGroup

        assertEquals(5, updatedInner.members.size)

        assertEquals(first, updatedInner.members[0])
        assertEquals(second, updatedInner.members[2])
        assertEquals(third, updatedInner.members[3])

        val firstCopy = updatedInner.members[1] as EditorRectangle
        val thirdCopy = updatedInner.members[4] as EditorRectangle

        assertNotEquals(first.id, firstCopy.id)
        assertNotEquals(third.id, thirdCopy.id)

        assertEquals(30f, firstCopy.x, 0.001f)
        assertEquals(40f, firstCopy.y, 0.001f)

        assertEquals(120f, thirdCopy.x, 0.001f)
        assertEquals(140f, thirdCopy.y, 0.001f)

        assertEquals(
            setOf(firstCopy.id, thirdCopy.id),
            state.selectedMemberIds,
        )

        state.undo()

        assertEquals(
            EditorDocument(members = listOf(outer)),
            state.document,
        )
    }


    @Test
    fun `copying nested member and pasting preserves source hierarchy`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 50f,
            y = 60f,
            width = 10f,
            height = 10f,
        )

        val group = EditorGroup(
            members = listOf(first, second),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(group),
            ),
        )

        state.selectMember(first.id)
        state.copySelectedMembers()
        state.pasteMembers()

        // Paste goes to the document root.
        assertEquals(2, state.document.members.size)

        // The original group is completely unchanged.
        assertEquals(group, state.document.members[0])

        val pasted = state.document.members[1] as EditorRectangle

        assertNotEquals(first.id, pasted.id)
        assertEquals(first.x + 20f, pasted.x, 0.001f)
        assertEquals(first.y + 20f, pasted.y, 0.001f)

        assertEquals(setOf(pasted.id), state.selectedMemberIds)

        state.undo()

        assertEquals(
            EditorDocument(members = listOf(group)),
            state.document,
        )
    }

    @Test
    fun `pasting nested group preserves hierarchy and regenerates IDs`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )

        val ellipse = EditorEllipse(
            x = 50f,
            y = 60f,
            width = 10f,
            height = 10f,
        )

        val inner = EditorGroup(
            members = listOf(rectangle, ellipse),
        )

        val outer = EditorGroup(
            members = listOf(inner),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(outer),
            ),
        )

        state.selectMember(inner.id)
        state.copySelectedMembers()
        state.pasteMembers()

        assertEquals(2, state.document.members.size)
        assertEquals(outer, state.document.members[0])

        val pasted = state.document.members[1] as EditorGroup

        assertNotEquals(inner.id, pasted.id)

        val pastedRectangle = pasted.members[0] as EditorRectangle
        val pastedEllipse = pasted.members[1] as EditorEllipse

        assertNotEquals(rectangle.id, pastedRectangle.id)
        assertNotEquals(ellipse.id, pastedEllipse.id)

        assertEquals(30f, pastedRectangle.x, 0.001f)
        assertEquals(40f, pastedRectangle.y, 0.001f)
        assertEquals(70f, pastedEllipse.x, 0.001f)
        assertEquals(80f, pastedEllipse.y, 0.001f)

        assertEquals(setOf(pasted.id), state.selectedMemberIds)
    }

    @Test
    fun `repeated pastes use incremental offsets and fresh IDs`() {
        val original = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(original),
            ),
        )

        state.selectMember(original.id)
        state.copySelectedMembers()

        state.pasteMembers()
        state.pasteMembers()
        state.pasteMembers()

        assertEquals(4, state.document.members.size)

        val copies = state.document.members
            .drop(1)
            .map { it as EditorRectangle }

        assertEquals(
            listOf(30f, 50f, 70f),
            copies.map { it.x },
        )

        assertEquals(
            listOf(40f, 60f, 80f),
            copies.map { it.y },
        )

        assertEquals(3, copies.map { it.id }.toSet().size)
        assertTrue(copies.none { it.id == original.id })

        state.undo()

        assertEquals(3, state.document.members.size)
    }


    @Test
    fun `selecting nodes from different groups selects their root groups`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )

        val firstGroup = EditorGroup(members = listOf(first))
        val secondGroup = EditorGroup(members = listOf(second))

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(firstGroup, secondGroup),
            ),
        )

        state.selectMembers(setOf(first.id, second.id))

        assertEquals(
            setOf(firstGroup.id, secondGroup.id),
            state.selectedMemberIds,
        )

        assertEquals(
            setOf(first.id, second.id),
            state.selectedNodeIds,
        )
    }

    @Test
    fun `selecting nested siblings preserves their selection depth`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )

        val inner = EditorGroup(
            members = listOf(first, second),
        )
        val outer = EditorGroup(
            members = listOf(inner),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(outer),
            ),
        )

        state.selectMembers(setOf(first.id, second.id))

        assertEquals(
            setOf(first.id, second.id),
            state.selectedMemberIds,
        )
    }


    @Test
    fun `grouping cross hierarchy selection preserves original groups`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f
        )
        val second = EditorEllipse(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )

        val firstGroup = EditorGroup(members = listOf(first))
        val secondGroup = EditorGroup(members = listOf(second))

        val original = EditorDocument(
            members = listOf(firstGroup, secondGroup),
        )

        val state = EditorDocumentState(
            initialDocument = original,
        )

        state.selectMembers(setOf(first.id, second.id))

        assertTrue(state.groupSelectedMembers())

        val combined = state.document.members.single() as EditorGroup

        assertEquals(
            listOf(firstGroup, secondGroup),
            combined.members,
        )

        assertEquals(
            setOf(combined.id),
            state.selectedMemberIds,
        )

        state.undo()

        assertEquals(original, state.document)
    }

    @Test
    fun `nudging a selected nested group preserves hierarchy`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorEllipse(
            x = 50f,
            y = 60f,
            width = 10f,
            height = 10f,
        )

        val inner = EditorGroup(members = listOf(first))
        val outer = EditorGroup(members = listOf(inner, second))

        val original = EditorDocument(
            members = listOf(outer),
        )

        val state = EditorDocumentState(original)

        state.selectMember(outer.id)
        state.beginNudge()
        state.updateNudge(Offset(15f, -10f))

        assertEquals(original, state.document)

        state.finishNudge()

        val updatedOuter = state.document.members.single() as EditorGroup
        val updatedInner = updatedOuter.members[0] as EditorGroup

        assertEquals(outer.id, updatedOuter.id)
        assertEquals(inner.id, updatedInner.id)

        val movedFirst = updatedInner.members.single() as EditorRectangle
        val movedSecond = updatedOuter.members[1] as EditorEllipse

        assertEquals(25f, movedFirst.x, 0.001f)
        assertEquals(10f, movedFirst.y, 0.001f)
        assertEquals(65f, movedSecond.x, 0.001f)
        assertEquals(50f, movedSecond.y, 0.001f)

        state.undo()
        assertEquals(original, state.document)

        state.redo()
        assertEquals(updatedOuter, state.document.members.single())
    }

    @Test
    fun `changing selection cancels an active nudge`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )
        val second = EditorRectangle(
            x = 100f,
            y = 200f,
            width = 10f,
            height = 10f,
        )

        val original = EditorDocument(
            members = listOf(first, second),
        )

        val state = EditorDocumentState(original)

        state.selectMember(first.id)
        state.beginNudge()
        state.updateNudge(Offset(50f, 30f))

        assertNotEquals(state.nodes, state.displayNodes)

        state.selectMember(second.id)

        assertEquals(original, state.document)
        assertEquals(state.nodes, state.displayNodes)
        assertEquals(setOf(second.id), state.selectedMemberIds)

        // A late key-release must not commit the previous preview.
        state.finishNudge()

        assertEquals(original, state.document)
        assertFalse(state.canUndo)
    }

    @Test
    fun `opening document cancels pending nudge`() {
        val first = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 10f,
            height = 10f,
        )

        val state = EditorDocumentState(
            EditorDocument(members = listOf(first)),
        )

        state.selectMember(first.id)
        state.beginNudge()
        state.updateNudge(Offset(100f, 100f))

        val replacement = EditorRectangle(
            x = 500f,
            y = 600f,
            width = 10f,
            height = 10f,
        )

        val replacementDocument = EditorDocument(
            members = listOf(replacement),
        )

        state.open(replacementDocument)

        assertEquals(replacementDocument, state.document)
        assertEquals(state.nodes, state.displayNodes)
        assertTrue(state.selectedMemberIds.isEmpty())

        // Simulate a late key-release from the old document.
        state.finishNudge()

        assertEquals(replacementDocument, state.document)
        assertFalse(state.canUndo)
    }


    @Test
    fun `equal spacing finds midpoint between two targets`() {
        val moving = Rect(100f, 0f, 130f, 30f)

        val targets = listOf(
            Rect(0f, 0f, 50f, 30f),
            Rect(150f, 0f, 200f, 30f),
        )

        val snap = findEqualSpacingSnap(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset(-14f, 0f),
            threshold = 5f,
            axis = SpacingAxis.HORIZONTAL,
        )

        assertNotNull(snap)
        assertEquals(-1f, snap.correction, 0.001f)
        assertEquals(35f, snap.gap, 0.001f)
    }


    @Test
    fun `horizontal equal spacing ignores vertically unrelated targets`() {
        val moving = Rect(100f, 300f, 130f, 330f)

        val targets = listOf(
            Rect(0f, 0f, 50f, 30f),
            Rect(150f, 0f, 200f, 30f),
        )

        val result = findEqualSpacingSnap(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset(-14f, 0f),
            threshold = 5f,
            axis = SpacingAxis.HORIZONTAL,
        )

        assertNull(result)
    }

    @Test
    fun `vertical equal spacing finds midpoint with overlapping targets`() {
        val moving = Rect(10f, 100f, 40f, 130f)

        val targets = listOf(
            Rect(0f, 0f, 50f, 50f),
            Rect(0f, 150f, 50f, 200f),
        )

        val result = findEqualSpacingSnap(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset(0f, -14f),
            threshold = 5f,
            axis = SpacingAxis.VERTICAL,
        )

        assertNotNull(result)
        assertEquals(-1f, result.correction, 0.001f)
        assertEquals(35f, result.gap, 0.001f)
    }


    @Test
    fun `smart snapping selects equal horizontal spacing`() {
        val moving = Rect(100f, 0f, 130f, 30f)

        val targets = listOf(
            Rect(0f, 0f, 50f, 30f),
            Rect(150f, 0f, 200f, 30f),
        )

        val result = snapTranslation(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset(-14f, 0f),
            threshold = 5f,
        )

        println("Snap result: $result")
        println("Horizontal spacing: ${result.horizontalSpacing}")

        assertNotNull(result.horizontalSpacing)
        assertEquals(-15f, result.delta.x, 0.001f)
        assertEquals(35f, result.horizontalSpacing.gap, 0.001f)
        assertNotNull(result.snappedBounds)
        assertEquals(85f, result.snappedBounds.left, 0.001f)
    }


    @Test
    fun `equal spacing snaps before both targets`() {
        val moving = Rect(42f, 0f, 62f, 20f)

        val targets = listOf(
            Rect(100f, 0f, 130f, 20f),
            Rect(170f, 0f, 200f, 20f),
        )

        val result = findEqualSpacingSnap(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset.Zero,
            threshold = 5f,
            axis = SpacingAxis.HORIZONTAL,
        )

        assertNotNull(result)
        assertEquals(SpacingPlacement.BEFORE, result.placement)
        assertEquals(-2f, result.correction, 0.001f)
        assertEquals(40f, result.gap, 0.001f)
    }

    @Test
    fun `equal spacing snaps between both targets`() {
        val moving = Rect(141f, 0f, 161f, 20f)

        val targets = listOf(
            Rect(100f, 0f, 130f, 20f),
            Rect(170f, 0f, 200f, 20f),
        )

        val result = findEqualSpacingSnap(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset.Zero,
            threshold = 5f,
            axis = SpacingAxis.HORIZONTAL,
        )

        assertNotNull(result)
        assertEquals(SpacingPlacement.BETWEEN, result.placement)
        assertEquals(-1f, result.correction, 0.001f)
        assertEquals(10f, result.gap, 0.001f)
    }

    @Test
    fun `equal spacing snaps after both targets`() {
        val moving = Rect(238f, 0f, 258f, 20f)

        val targets = listOf(
            Rect(100f, 0f, 130f, 20f),
            Rect(170f, 0f, 200f, 20f),
        )

        val result = findEqualSpacingSnap(
            movingBounds = moving,
            targetBounds = targets,
            proposedDelta = Offset.Zero,
            threshold = 5f,
            axis = SpacingAxis.HORIZONTAL,
        )

        assertNotNull(result)
        assertEquals(SpacingPlacement.AFTER, result.placement)
        assertEquals(2f, result.correction, 0.001f)
        assertEquals(40f, result.gap, 0.001f)
    }


    @OptIn(InternalComposeUiApi::class)
    @Test
    fun `Ctrl Shift F fits selection`() {
        val event = KeyEvent(
            key = Key.F,
            type = KeyEventType.KeyDown,
            isCtrlPressed = true,
            isShiftPressed = true,
        )

        assertEquals(
            EditorShortcut.FitToSelection,
            event.editorShortcut(),
        )
    }


    @Test
    fun `horizontal connection produces a straight route`() {
        val start = Offset(0f, 0f)
        val end = Offset(100f, 0f)

        assertEquals(
            listOf(start, end),
            routeConnector(start, end).points
        )
    }

    @Test
    fun `diagonal at 45 degrees produces a straight route`() {
        val start = Offset(0f, 0f)
        val end = Offset(100f, 100f)

        assertEquals(
            listOf(start, end),
            routeConnector(start, end).points
        )
    }

    @Test
    fun `non aligned endpoints produce a corner`() {
        val start = Offset(0f, 0f)
        val end = Offset(100f, 25f)

        val route = routeConnector(start, end)

        assertEquals(3, route.points.size)
        assertEquals(start, route.points.first())
        assertEquals(end, route.points.last())

        val corner = route.points[1]

        assertTrue(abs(corner.y) < 0.001f)
        assertTrue(abs((end.x - corner.x) - (end.y - corner.y)) < 0.001f)
    }

    @Test
    fun `coincident endpoints produce a degenerate straight route`() {
        val point = Offset(42f, 17f)

        assertEquals(
            listOf(point, point),
            routeConnector(point, point).points
        )
    }

    @Test
    fun `non finite endpoints are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            routeConnector(
                Offset(Float.NaN, 0f),
                Offset(100f, 100f)
            )
        }
    }

    @Test
    fun `straight route has no rounded corner`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f)
            )
        )

        assertNull(route.roundedCorner(12f))
    }

    @Test
    fun `corner is rounded along adjacent segments`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
                Offset(100f, 100f)
            )
        )

        val corner = assertNotNull(
            route.roundedCorner(20f)
        )

        assertEquals(Offset(80f, 0f), corner.entry)
        assertEquals(Offset(100f, 0f), corner.control)
        assertEquals(Offset(100f, 20f), corner.exit)
    }

    @Test
    fun `radius is limited by shortest segment`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(10f, 0f),
                Offset(10f, 100f)
            )
        )

        val corner = assertNotNull(
            route.roundedCorner(50f)
        )

        assertEquals(Offset(0f, 0f), corner.entry)
        assertEquals(Offset(10f, 10f), corner.exit)
    }

    @Test
    fun `zero radius produces no rounded corner`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
                Offset(100f, 100f)
            )
        )

        assertNull(route.roundedCorner(0f))
    }

    @Test
    fun `coincident points do not produce invalid curves`() {
        val route = ConnectorRoute(
            listOf(
                Offset.Zero,
                Offset.Zero,
                Offset(100f, 0f)
            )
        )

        assertNull(route.roundedCorner(12f))
    }

    @Test
    fun `free endpoint resolves to its own position`() {
        val position = Offset(25f, 50f)
        val endpoint = ConnectorEndpoint.Free(position)

        assertEquals(
            position,
            endpoint.resolve { null }
        )
    }

    @Test
    fun `attached endpoint resolves using callback`() {
        val nodeId = Uuid.random()
        val resolvedPosition = Offset(100f, 200f)

        val endpoint = ConnectorEndpoint.Attached(
            nodeId = nodeId,
            fallbackPosition = Offset.Zero
        )

        assertEquals(
            resolvedPosition,
            endpoint.resolve { id ->
                if (id == nodeId) resolvedPosition else null
            }
        )
    }

    @Test
    fun `attached endpoint falls back when node is missing`() {
        val fallback = Offset(40f, 60f)

        val endpoint = ConnectorEndpoint.Attached(
            nodeId = Uuid.random(),
            fallbackPosition = fallback
        )

        assertEquals(
            fallback,
            endpoint.resolve { null }
        )
    }

    @Test
    fun `free endpoint does not invoke resolver`() {
        val position = Offset(12f, 34f)
        val endpoint = ConnectorEndpoint.Free(position)

        assertEquals(
            position,
            endpoint.resolve {
                error("Resolver should not be called")
            }
        )
    }
    @Test
    fun `free endpoints route between their positions`() {
        val start = Offset(0f, 0f)
        val end = Offset(100f, 0f)

        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(start),
            end = ConnectorEndpoint.Free(end),
        )

        assertEquals(
            listOf(start, end),
            connector.route { null }.points,
        )
    }

    @Test
    fun `attached endpoints use resolved positions`() {
        val startId = Uuid.random()
        val endId = Uuid.random()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = startId,
                fallbackPosition = Offset.Zero,
            ),
            end = ConnectorEndpoint.Attached(
                nodeId = endId,
                fallbackPosition = Offset.Zero,
            ),
        )

        val positions = mapOf(
            startId to Offset(0f, 0f),
            endId to Offset(100f, 0f),
        )

        assertEquals(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
            ),
            connector.route(positions::get).points,
        )
    }

    @Test
    fun `missing attachment uses fallback position`() {
        val fallback = Offset(25f, 50f)
        val freePosition = Offset(125f, 50f)

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = Uuid.random(),
                fallbackPosition = fallback,
            ),
            end = ConnectorEndpoint.Free(freePosition),
        )

        assertEquals(
            listOf(fallback, freePosition),
            connector.route { null }.points,
        )
    }

    @Test
    fun `moving an attached node changes the route`() {
        val nodeId = Uuid.random()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = nodeId,
                fallbackPosition = Offset.Zero,
            ),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        val initial = connector.route {
            Offset(0f, 0f)
        }

        val moved = connector.route {
            Offset(50f, 0f)
        }

        assertEquals(
            Offset(0f, 0f),
            initial.points.first(),
        )

        assertEquals(
            Offset(50f, 0f),
            moved.points.first(),
        )

        assertEquals(
            connector.end.resolve { null },
            moved.points.last(),
        )
    }


    @Test
    fun `adding connector commits document change`() {
        val state = EditorDocumentState()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        state.addConnector(connector)

        assertEquals(listOf(connector), state.connectors)
        assertTrue(state.canUndo)

        state.undo()

        assertTrue(state.connectors.isEmpty())
    }

    @Test
    fun `adding duplicate connector ID is rejected`() {
        val state = EditorDocumentState()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        state.addConnector(connector)

        assertFailsWith<IllegalArgumentException> {
            state.addConnector(connector)
        }

        assertEquals(1, state.connectors.size)
    }

    @Test
    fun `updating connector preserves its identity and supports undo`() {
        val state = EditorDocumentState()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        state.addConnector(connector)

        val updated = connector.copy(
            end = ConnectorEndpoint.Free(Offset(200f, 50f)),
        )

        state.updateConnector(updated)

        assertEquals(listOf(updated), state.connectors)
        assertEquals(connector.id, state.connectors.single().id)

        state.undo()

        assertEquals(listOf(connector), state.connectors)

        state.redo()

        assertEquals(listOf(updated), state.connectors)
    }

    @Test
    fun `deleting connector supports undo and redo`() {
        val state = EditorDocumentState()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        state.addConnector(connector)
        state.deleteConnector(connector.id)

        assertTrue(state.connectors.isEmpty())

        state.undo()

        assertEquals(listOf(connector), state.connectors)

        state.redo()

        assertTrue(state.connectors.isEmpty())
    }

    @Test
    fun `updating missing connector does nothing`() {
        val state = EditorDocumentState()

        val missing = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        state.updateConnector(missing)

        assertTrue(state.connectors.isEmpty())
        assertFalse(state.canUndo)
    }

    @Test
    fun `deleting missing connector does nothing`() {
        val state = EditorDocumentState()

        state.deleteConnector(Uuid.random())

        assertTrue(state.connectors.isEmpty())
        assertFalse(state.canUndo)
    }

    @Test
    fun `updating connector with identical value creates no history entry`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                connectors = listOf(connector),
            ),
        )

        state.updateConnector(connector)

        assertFalse(state.canUndo)
        assertEquals(listOf(connector), state.connectors)
    }


    @Test
    fun `connectors survive document serialization`() {
        val firstId = Uuid.random()
        val secondId = Uuid.random()

        val connectors = listOf(
            EditorConnector(
                start = ConnectorEndpoint.Free(
                    Offset(10f, 20f),
                ),
                end = ConnectorEndpoint.Free(
                    Offset(100f, 200f),
                ),
            ),
            EditorConnector(
                start = ConnectorEndpoint.Attached(
                    nodeId = firstId,
                    fallbackPosition = Offset(30f, 40f),
                ),
                end = ConnectorEndpoint.Attached(
                    nodeId = secondId,
                    fallbackPosition = Offset(300f, 400f),
                ),
            ),
        )

        val document = EditorDocument(
            connectors = connectors,
        )

        val restored = DocumentCodec.decode(
            DocumentCodec.encode(document)
        )

        assertEquals(document, restored)
    }

    @Test
    fun `mixed connector endpoints survive serialization`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = Uuid.random(),
                fallbackPosition = Offset(50f, 60f),
            ),
            end = ConnectorEndpoint.Free(
                Offset(200f, 100f),
            ),
        )

        val restored = DocumentCodec.decode(
            DocumentCodec.encode(
                EditorDocument(connectors = listOf(connector))
            )
        )

        assertEquals(connector, restored.connectors.single())
    }

    @Test
    fun `legacy documents default to no connectors`() {
        for (version in 1..2) {
            val legacy = DocumentDto(
                version = version,
                members = listOf(),
            )

            val restored = legacy.toDocument()

            assertTrue(restored.connectors.isEmpty())
        }
    }

    @Test
    fun `duplicate connector IDs are rejected during decoding`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        val document = EditorDocument(
            connectors = listOf(connector, connector),
        )

        assertFailsWith<IllegalArgumentException> {
            DocumentCodec.decode(
                DocumentCodec.encode(document)
            )
        }
    }

    @Test
    fun `dangling connector attachment survives serialization`() {
        val missingNodeId = Uuid.random()
        val fallback = Offset(75f, 125f)

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = missingNodeId,
                fallbackPosition = fallback,
            ),
            end = ConnectorEndpoint.Free(Offset(200f, 0f)),
        )

        val restored = DocumentCodec.decode(
            DocumentCodec.encode(
                EditorDocument(connectors = listOf(connector))
            )
        )

        val endpoint = restored.connectors.single().start

        assertEquals(
            fallback,
            endpoint.resolve { null },
        )
    }


    @Test
    fun `connector endpoint remains free on empty canvas`() {
        val position = Offset(100f, 200f)

        val endpoint = connectorEndpointAt(
            position = position,
            nodes = listOf(),
            hitTest = { _, _ -> false },
            bounds = { error("No bounds needed") },
        )

        assertEquals(
            ConnectorEndpoint.Free(position),
            endpoint,
        )
    }

    @Test
    fun `connector attachment prefers topmost node`() {
        val bottom = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 100f,
        )

        val top = EditorRectangle(
            x = 20f,
            y = 20f,
            width = 100f,
            height = 100f,
        )

        val endpoint = connectorEndpointAt(
            position = Offset(50f, 50f),
            nodes = listOf(bottom, top),
            hitTest = { node, point ->
                node.bounds.contains(point)
            },
            bounds = { it.bounds },
        )

        assertEquals(
            top.id,
            (endpoint as ConnectorEndpoint.Attached).nodeId,
        )
    }


    @Test
    fun `connector anchors occupy all eight OBB positions`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 200f,
            width = 80f,
            height = 40f,
        )

        val expected = mapOf(
            ConnectorAnchor.NORTH_WEST to Offset(100f, 200f),
            ConnectorAnchor.NORTH to Offset(140f, 200f),
            ConnectorAnchor.NORTH_EAST to Offset(180f, 200f),
            ConnectorAnchor.EAST to Offset(180f, 220f),
            ConnectorAnchor.SOUTH_EAST to Offset(180f, 240f),
            ConnectorAnchor.SOUTH to Offset(140f, 240f),
            ConnectorAnchor.SOUTH_WEST to Offset(100f, 240f),
            ConnectorAnchor.WEST to Offset(100f, 220f),
        )

        for ((anchor, position) in expected) {
            assertEquals(
                position,
                rectangle.anchorPosition(anchor),
            )
        }

        assertEquals(8, ConnectorAnchor.entries.size)
    }

    @Test
    fun `connector anchors follow shape translation`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 60f,
        )

        val moved = rectangle.copy(
            x = 50f,
            y = 90f,
        )

        val anchor = ConnectorAnchor.NORTH_EAST

        assertEquals(
            rectangle.anchorPosition(anchor) + Offset(40f, 70f),
            moved.anchorPosition(anchor),
        )
    }

    @Test
    fun `connector anchors follow shape resizing`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 60f,
        )

        val resized = rectangle.copy(
            width = 200f,
            height = 120f,
        )

        assertEquals(
            Offset(210f, 80f),
            resized.anchorPosition(ConnectorAnchor.EAST),
        )
    }

    @Test
    fun `connector anchors rotate around shape centre`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 60f,
            rotation = (PI / 2).toFloat(),
        )

        val north = rectangle.anchorPosition(
            ConnectorAnchor.NORTH
        )

        assertEquals(80f, north.x, 0.001f)
        assertEquals(30f, north.y, 0.001f)
    }

    @Test
    fun `connector anchor stays fixed relative to shape`() {
        val rectangle = EditorRectangle(
            x = 20f,
            y = 30f,
            width = 120f,
            height = 80f,
            rotation = 0.7f,
        )

        val anchor = ConnectorAnchor.SOUTH_WEST

        val original = rectangle.anchorPosition(anchor)

        val moved = rectangle.copy(
            x = rectangle.x + 50f,
            y = rectangle.y - 25f,
        )

        val translated = moved.anchorPosition(anchor)

        assertEquals(
            original.x + 50f,
            translated.x,
            0.001f,
        )

        assertEquals(
            original.y - 25f,
            translated.y,
            0.001f,
        )
    }


    @Test
    fun `connector anchor survives document serialization`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = Uuid.random(),
                fallbackPosition = Offset(100f, 200f),
                anchor = ConnectorAnchor.NORTH_EAST,
            ),
            end = ConnectorEndpoint.Attached(
                nodeId = Uuid.random(),
                fallbackPosition = Offset(300f, 400f),
                anchor = ConnectorAnchor.SOUTH_WEST,
            ),
        )

        val document = EditorDocument(
            connectors = listOf(connector),
        )

        val restored = DocumentCodec.decode(
            DocumentCodec.encode(document)
        )

        assertEquals(document, restored)
    }

    @Test
    fun `all connector anchors survive serialization`() {
        for (anchor in ConnectorAnchor.entries) {
            val connector = EditorConnector(
                start = ConnectorEndpoint.Attached(
                    nodeId = Uuid.random(),
                    fallbackPosition = Offset(10f, 20f),
                    anchor = anchor,
                ),
                end = ConnectorEndpoint.Free(
                    Offset(200f, 300f)
                ),
            )

            val restored = DocumentCodec.decode(
                DocumentCodec.encode(
                    EditorDocument(
                        connectors = listOf(connector)
                    )
                )
            )

            assertEquals(
                anchor,
                (restored.connectors.single().start
                        as ConnectorEndpoint.Attached).anchor,
            )
        }
    }

    @Test
    fun `legacy centre attachments remain supported`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = Uuid.random(),
                fallbackPosition = Offset(50f, 60f),
            ),
            end = ConnectorEndpoint.Free(
                Offset(200f, 100f)
            ),
        )

        val restored = DocumentCodec.decode(
            DocumentCodec.encode(
                EditorDocument(
                    connectors = listOf(connector)
                )
            )
        )

        val endpoint = restored.connectors.single().start
                as ConnectorEndpoint.Attached

        assertNull(endpoint.anchor)
        assertEquals(Offset(50f, 60f), endpoint.fallbackPosition)
    }

    @Test
    fun `version three attachment without anchor loads`() {
        val nodeId = Uuid.random()
        val connectorId = Uuid.random()

        val json = """
        {
          "version": 3,
          "nodes": [],
          "connectors": [
            {
              "id": "$connectorId",
              "start": {
                "type": "attached",
                "nodeId": "$nodeId",
                "fallbackPosition": {
                  "x": 25.0,
                  "y": 75.0
                }
              },
              "end": {
                "type": "free",
                "position": {
                  "x": 150.0,
                  "y": 200.0
                }
              }
            }
          ]
        }
    """.trimIndent()

        val document = DocumentCodec.decode(json)

        val endpoint = document.connectors.single().start
                as ConnectorEndpoint.Attached

        assertEquals(nodeId, endpoint.nodeId)
        assertNull(endpoint.anchor)
        assertEquals(Offset(25f, 75f), endpoint.fallbackPosition)
    }

    @Test
    fun `new documents use version four`() {
        assertEquals(4, EditorDocument().toDto().version)
    }


    @Test
    fun `anchored connector resolves to requested shape port`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 200f,
            width = 80f,
            height = 40f,
        )

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = rectangle.id,
                fallbackPosition = Offset.Zero,
                anchor = ConnectorAnchor.EAST,
            ),
            end = ConnectorEndpoint.Free(
                Offset(300f, 220f),
            ),
        )

        val route = connector.route { id, anchor ->
            if (id != rectangle.id) {
                null
            } else {
                anchor?.let(rectangle::anchorPosition)
                    ?: rectangle.bounds.center
            }
        }

        assertEquals(
            Offset(180f, 220f),
            route.points.first(),
        )
    }

    @Test
    fun `anchored connector follows shape rotation`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 60f,
            rotation = (PI / 2).toFloat(),
        )

        val endpoint = ConnectorEndpoint.Attached(
            nodeId = rectangle.id,
            fallbackPosition = Offset.Zero,
            anchor = ConnectorAnchor.NORTH,
        )

        val position = endpoint.resolve { id, anchor ->
            if (id == rectangle.id) {
                anchor?.let(rectangle::anchorPosition)
                    ?: rectangle.bounds.center
            } else {
                null
            }
        }

        assertEquals(80f, position.x, 0.001f)
        assertEquals(30f, position.y, 0.001f)
    }

    @Test
    fun `legacy connector attachment still resolves to centre`() {
        val rectangle = EditorRectangle(
            x = 10f,
            y = 20f,
            width = 100f,
            height = 60f,
        )

        val endpoint = ConnectorEndpoint.Attached(
            nodeId = rectangle.id,
            fallbackPosition = Offset.Zero,
        )

        val position = endpoint.resolve { id, anchor ->
            if (id != rectangle.id) {
                null
            } else if (anchor == null) {
                rectangle.bounds.center
            } else {
                rectangle.anchorPosition(anchor)
            }
        }

        assertEquals(
            Offset(60f, 50f),
            position,
        )
    }


    @Test
    fun `connector chooses nearest OBB anchor inside shape`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 100f,
            width = 100f,
            height = 100f,
        )

        val endpoint = connectorEndpointAt(
            position = Offset(195f, 145f),
            nodes = listOf(rectangle),
            hitTest = { node, point ->
                node.bounds.contains(point)
            },
            bounds = { it.bounds },
            snapRadius = 12f,
        ) as ConnectorEndpoint.Attached

        assertEquals(rectangle.id, endpoint.nodeId)
        assertEquals(ConnectorAnchor.EAST, endpoint.anchor)
        assertEquals(Offset(200f, 150f), endpoint.fallbackPosition)
    }

    @Test
    fun `connector snaps to OBB anchor from outside shape`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 100f,
            width = 100f,
            height = 100f,
        )

        val endpoint = connectorEndpointAt(
            position = Offset(205f, 150f),
            nodes = listOf(rectangle),
            hitTest = { node, point ->
                node.bounds.contains(point)
            },
            bounds = { it.bounds },
            snapRadius = 12f,
        ) as ConnectorEndpoint.Attached

        assertEquals(ConnectorAnchor.EAST, endpoint.anchor)
    }

    @Test
    fun `connector stays free outside snapping radius`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 100f,
            width = 100f,
            height = 100f,
        )

        val position = Offset(230f, 150f)

        val endpoint = connectorEndpointAt(
            position = position,
            nodes = listOf(rectangle),
            hitTest = { node, point ->
                node.bounds.contains(point)
            },
            bounds = { it.bounds },
            snapRadius = 12f,
        )

        assertEquals(
            ConnectorEndpoint.Free(position),
            endpoint,
        )
    }

    @Test
    fun `connector chooses rotated OBB anchor`() {
        val rectangle = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 60f,
            rotation = (PI / 2).toFloat(),
        )

        val endpoint = connectorEndpointAt(
            position = Offset(81f, 30f),
            nodes = listOf(rectangle),
            hitTest = { _, _ -> false },
            bounds = { it.bounds },
            snapRadius = 12f,
        ) as ConnectorEndpoint.Attached

        assertEquals(ConnectorAnchor.NORTH, endpoint.anchor)
        assertEquals(80f, endpoint.fallbackPosition.x, 0.001f)
        assertEquals(30f, endpoint.fallbackPosition.y, 0.001f)
    }

    @Test
    fun `connector attaches to topmost overlapping shape`() {
        val bottom = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 100f,
        )

        val top = EditorRectangle(
            x = 20f,
            y = 20f,
            width = 100f,
            height = 100f,
        )

        val endpoint = connectorEndpointAt(
            position = Offset(50f, 50f),
            nodes = listOf(bottom, top),
            hitTest = { node, point ->
                node.bounds.contains(point)
            },
            bounds = { it.bounds },
        ) as ConnectorEndpoint.Attached

        assertEquals(top.id, endpoint.nodeId)
        assertNotNull(endpoint.anchor)
    }


    @Test
    fun `deleting attached node converts endpoint to free`() {
        val nodeId = Uuid.random()
        val position = Offset(100f, 200f)

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = nodeId,
                fallbackPosition = Offset.Zero,
                anchor = ConnectorAnchor.EAST,
            ),
            end = ConnectorEndpoint.Free(Offset(300f, 200f)),
        )

        val result = listOf(connector).detachRemovedNodes(
            removedNodeIds = setOf(nodeId),
            resolvePosition = { position },
        )

        assertEquals(
            ConnectorEndpoint.Free(position),
            result.single().start,
        )

        assertEquals(connector.end, result.single().end)
        assertEquals(connector.id, result.single().id)
    }

    @Test
    fun `deleting unrelated node preserves connector attachments`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = Uuid.random(),
                fallbackPosition = Offset(20f, 30f),
                anchor = ConnectorAnchor.NORTH,
            ),
            end = ConnectorEndpoint.Free(Offset(100f, 100f)),
        )

        val result = listOf(connector).detachRemovedNodes(
            removedNodeIds = setOf(Uuid.random()),
            resolvePosition = {
                error("Unrelated attachment must not be resolved")
            },
        )

        assertEquals(listOf(connector), result)
    }

    @Test
    fun `deleting both attachment targets detaches both endpoints`() {
        val firstId = Uuid.random()
        val secondId = Uuid.random()

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = firstId,
                fallbackPosition = Offset.Zero,
            ),
            end = ConnectorEndpoint.Attached(
                nodeId = secondId,
                fallbackPosition = Offset.Zero,
            ),
        )

        val result = listOf(connector).detachRemovedNodes(
            removedNodeIds = setOf(firstId, secondId),
            resolvePosition = { endpoint ->
                when (endpoint.nodeId) {
                    firstId -> Offset(10f, 20f)
                    secondId -> Offset(200f, 300f)
                    else -> null
                }
            },
        ).single()

        assertEquals(
            ConnectorEndpoint.Free(Offset(10f, 20f)),
            result.start,
        )

        assertEquals(
            ConnectorEndpoint.Free(Offset(200f, 300f)),
            result.end,
        )
    }

    @Test
    fun `missing position resolver uses attachment fallback`() {
        val nodeId = Uuid.random()
        val fallback = Offset(42f, 84f)

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = nodeId,
                fallbackPosition = fallback,
            ),
            end = ConnectorEndpoint.Free(Offset.Zero),
        )

        val result = listOf(connector).detachRemovedNodes(
            removedNodeIds = setOf(nodeId),
            resolvePosition = { null },
        )

        assertEquals(
            ConnectorEndpoint.Free(fallback),
            result.single().start,
        )
    }

    @Test
    fun `deleting a node and detaching connectors is one undo operation`() {
        val rectangle = EditorRectangle(
            x = 100f,
            y = 200f,
            width = 80f,
            height = 40f,
        )

        val connector = EditorConnector(
            start = ConnectorEndpoint.Attached(
                nodeId = rectangle.id,
                fallbackPosition = Offset.Zero,
                anchor = ConnectorAnchor.EAST,
            ),
            end = ConnectorEndpoint.Free(Offset(300f, 220f)),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(rectangle),
                connectors = listOf(connector),
            ),
        )

        state.selectMember(rectangle.id)
        state.deleteSelectedMembers()

        assertTrue(state.document.nodes.isEmpty())

        assertEquals(
            ConnectorEndpoint.Free(Offset(180f, 220f)),
            state.document.connectors.single().start,
        )

        state.undo()

        assertEquals(
            listOf(rectangle),
            state.document.nodes,
        )

        assertEquals(
            connector,
            state.document.connectors.single(),
        )

        state.redo()

        assertTrue(state.document.nodes.isEmpty())

        assertEquals(
            ConnectorEndpoint.Free(Offset(180f, 220f)),
            state.document.connectors.single().start,
        )
    }


    @Test
    fun `distance to horizontal connector segment`() {
        val start = Offset(0f, 0f)
        val end = Offset(100f, 0f)

        assertEquals(
            10f,
            distanceToSegment(
                Offset(50f, 10f),
                start,
                end,
            ),
            0.001f,
        )
    }

    @Test
    fun `distance to segment clamps beyond endpoints`() {
        val distance = distanceToSegment(
            point = Offset(110f, 0f),
            start = Offset(0f, 0f),
            end = Offset(100f, 0f),
        )

        assertEquals(10f, distance, 0.001f)
    }

    @Test
    fun `distance to diagonal connector segment`() {
        val distance = distanceToSegment(
            point = Offset(10f, 0f),
            start = Offset(0f, 0f),
            end = Offset(10f, 10f),
        )

        assertEquals(
            7.0710678f,
            distance,
            0.001f,
        )
    }

    @Test
    fun `connector route hit testing checks all segments`() {
        val route = ConnectorRoute(
            points = listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
                Offset(100f, 100f),
            )
        )

        assertTrue(
            route.contains(
                point = Offset(102f, 50f),
                tolerance = 5f,
            )
        )

        assertFalse(
            route.contains(
                point = Offset(120f, 50f),
                tolerance = 5f,
            )
        )
    }

    @Test
    fun `zero length connector segment can be hit tested`() {
        val point = Offset(20f, 30f)

        assertEquals(
            5f,
            distanceToSegment(
                point = Offset(23f, 34f),
                start = point,
                end = point,
            ),
            0.001f,
        )
    }

    @Test
    fun `connector hit tolerance is inclusive`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
            )
        )

        assertTrue(
            route.contains(
                point = Offset(50f, 5f),
                tolerance = 5f,
            )
        )
    }


    @Test
    fun `connector selection is independent of node selection`() {
        val node = EditorRectangle(
            x = 0f,
            y = 0f,
            width = 100f,
            height = 100f,
        )

        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(200f, 200f)),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                members = listOf(node),
                connectors = listOf(connector),
            ),
        )

        state.selectMember(node.id)
        assertEquals(setOf(node.id), state.selectedMemberIds)

        state.selectConnector(connector.id)

        assertTrue(state.selectedMemberIds.isEmpty())
        assertEquals(connector.id, state.selectedConnectorId)
        assertEquals(connector, state.selectedConnector)

        state.selectMember(node.id)

        assertNull(state.selectedConnectorId)
        assertEquals(setOf(node.id), state.selectedMemberIds)
    }

    @Test
    fun `selecting unknown connector does nothing`() {
        val state = EditorDocumentState()

        state.selectConnector(Uuid.random())

        assertNull(state.selectedConnectorId)
        assertNull(state.selectedConnector)
    }

    @Test
    fun `deleting selected connector clears connector selection`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 100f)),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                connectors = listOf(connector),
            ),
        )

        state.selectConnector(connector.id)
        state.deleteConnector(connector.id)

        assertNull(state.selectedConnectorId)
        assertTrue(state.connectors.isEmpty())

        state.undo()

        assertEquals(listOf(connector), state.connectors)
        assertNull(state.selectedConnectorId)
    }

    @Test
    fun `clearing selection also clears selected connector`() {
        val connector = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        val state = EditorDocumentState(
            initialDocument = EditorDocument(
                connectors = listOf(connector),
            ),
        )

        state.selectConnector(connector.id)
        state.clearSelection()

        assertNull(state.selectedConnector)
    }


    @Test
    fun `connector start handle can be hit`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
            )
        )

        assertEquals(
            ConnectorHandle.START,
            route.handleAt(Offset(4f, 3f), radius = 5f),
        )
    }

    @Test
    fun `connector end handle can be hit`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
            )
        )

        assertEquals(
            ConnectorHandle.END,
            route.handleAt(Offset(97f, 4f), radius = 5f),
        )
    }

    @Test
    fun `connector handle hit testing rejects distant points`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
            )
        )

        assertNull(
            route.handleAt(Offset(50f, 0f), radius = 10f)
        )
    }

    @Test
    fun `connector handle hit testing chooses nearest endpoint`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(10f, 0f),
            )
        )

        assertEquals(
            ConnectorHandle.END,
            route.handleAt(Offset(8f, 0f), radius = 10f),
        )
    }

    @Test
    fun `overlapping connector handles prefer start`() {
        val route = ConnectorRoute(
            listOf(
                Offset(20f, 30f),
                Offset(20f, 30f),
            )
        )

        assertEquals(
            ConnectorHandle.START,
            route.handleAt(Offset(20f, 30f), radius = 5f),
        )
    }

    @Test
    fun `connector handle hit radius is inclusive`() {
        val route = ConnectorRoute(
            listOf(
                Offset(0f, 0f),
                Offset(100f, 0f),
            )
        )

        assertEquals(
            ConnectorHandle.START,
            route.handleAt(Offset(6f, 8f), radius = 10f),
        )
    }

    @Test
    fun `connector handle hit testing rejects invalid radius`() {
        val route = ConnectorRoute(
            listOf(
                Offset.Zero,
                Offset(100f, 0f),
            )
        )

        assertFailsWith<IllegalArgumentException> {
            route.handleAt(Offset.Zero, radius = -1f)
        }

        assertFailsWith<IllegalArgumentException> {
            route.handleAt(Offset.Zero, radius = Float.NaN)
        }
    }


    @Test
    fun `editing connector endpoint preserves identity and opposite endpoint`() {
        val original = EditorConnector(
            start = ConnectorEndpoint.Free(Offset.Zero),
            end = ConnectorEndpoint.Free(Offset(100f, 0f)),
        )

        val newEnd = ConnectorEndpoint.Free(
            Offset(200f, 50f)
        )

        val updated = original.withEndpoint(
            ConnectorHandle.END,
            newEnd,
        )

        assertEquals(original.id, updated.id)
        assertEquals(original.start, updated.start)
        assertEquals(newEnd, updated.end)
    }
}