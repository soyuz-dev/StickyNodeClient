package org.stickynodes.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.stickynodes.data.rememberDocumentStorage
import org.stickynodes.editor.draw.EditorCanvas
import org.stickynodes.editor.draw.camera.EditorCamera
import org.stickynodes.editor.draw.geometry.withStyle
import org.stickynodes.editor.draw.model.EditorNode
import org.stickynodes.editor.draw.style.FillStyle
import org.stickynodes.editor.draw.style.NodeStyle
import org.stickynodes.editor.draw.style.StrokeStyle
import org.stickynodes.editor.history.EditorHistory
import org.stickynodes.editor.tools.EditorActionBar
import org.stickynodes.editor.tools.EditorTool
import org.stickynodes.editor.tools.EditorToolbar
import org.stickynodes.editor.tools.StylingToolbar
import org.stickynodes.editor.tools.ToolbarOrientation
import kotlin.uuid.Uuid

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
) {
    var camera by remember {
        mutableStateOf(EditorCamera())
    }

    var viewportSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val density = LocalDensity.current

    fun viewportCentre() = Offset(
        viewportSize.width / 2f,
        viewportSize.height / 2f,
    )

    var selectedTool by remember {
        mutableStateOf(EditorTool.SELECT)
    }

    var history by remember {
        mutableStateOf(EditorHistory())
    }

    var selectedNodeId by remember {
        mutableStateOf<Uuid?>(null)
    }

    // The pending style applies to this specific node.
    var widthPreview by remember {
        mutableStateOf<Pair<Uuid, NodeStyle>?>(null)
    }

    val nodes = history.document.nodes

    val selectedNode = nodes.firstOrNull {
        it.id == selectedNodeId
    }

    val displayNodes = widthPreview?.let { (id, style) ->
        nodes.map { node ->
            if (node.id == id) node.withStyle(style)
            else node
        }
    } ?: nodes

    var rectangleStyle by remember {
        mutableStateOf(NodeStyle())
    }

    var ellipseStyle by remember {
        mutableStateOf(NodeStyle())
    }

    var penStyle by remember {
        mutableStateOf(
            NodeStyle(
                fill = FillStyle.None,
                stroke = StrokeStyle(
                    color = 0xFF264B72.toInt(),
                    width = 3f,
                ),
            )
        )
    }

    val focusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val scope = rememberCoroutineScope()
    val storage = rememberDocumentStorage()

    fun save() {
        scope.launch {
            try {
                storage.save(history.document)
            } catch (e: Exception) {
                println("Failed to save document: ${e.message}")
            }
        }
    }

    fun open() {
        scope.launch {
            try {
                val document = storage.open() ?: return@launch

                history = EditorHistory(document = document)
                selectedNodeId = null
                widthPreview = null
            } catch (e: Exception) {
                println("Failed to open document: ${e.message}")
            }
        }
    }

    fun commitNodes(updatedNodes: List<EditorNode>) {
        history = history.commit(
            history.document.copy(nodes = updatedNodes)
        )
    }

    fun updateSelection() {
        selectedNodeId = selectedNodeId?.takeIf { id ->
            history.document.nodes.any { it.id == id }
        }
    }

    fun undo() {
        if (widthPreview != null) {
            widthPreview = null
            return
        }

        history = history.undo()
        updateSelection()
    }

    fun redo() {
        widthPreview = null
        history = history.redo()
        updateSelection()
    }

    fun deleteSelectedNode() {
        val id = selectedNodeId ?: return

        widthPreview = null

        commitNodes(
            history.document.nodes.filterNot { it.id == id }
        )

        selectedNodeId = null
    }

    fun updateToolStyle(style: NodeStyle) {
        when (selectedTool) {
            EditorTool.RECTANGLE -> rectangleStyle = style
            EditorTool.ELLIPSE -> ellipseStyle = style
            EditorTool.PEN -> penStyle = style
            else -> Unit
        }
    }

    fun previewStyle(style: NodeStyle) {
        val id = selectedNodeId

        if (id != null && nodes.any { it.id == id }) {
            widthPreview = id to style
        } else {
            updateToolStyle(style)
        }
    }

    fun finishStyleChange() {
        val pending = widthPreview ?: return
        val (id, style) = pending

        widthPreview = null

        commitNodes(
            history.document.nodes.map { node ->
                if (node.id == id) {
                    node.withStyle(style)
                } else {
                    node
                }
            }
        )
    }

    fun selectTool(tool: EditorTool) {
        widthPreview = null
        selectedTool = tool
        selectedNodeId = null
    }

    val activeStyle =
        widthPreview
            ?.takeIf { it.first == selectedNodeId }
            ?.second
            ?: selectedNode?.style
            ?: when (selectedTool) {
                EditorTool.RECTANGLE -> rectangleStyle
                EditorTool.ELLIPSE -> ellipseStyle
                EditorTool.PEN -> penStyle
                else -> null
            }

    val onStyleChanged: (NodeStyle) -> Unit = { updated ->
        widthPreview = null

        val id = selectedNodeId

        if (id != null && history.document.nodes.any { it.id == id }) {
            commitNodes(
                history.document.nodes.map { node ->
                    if (node.id == id) {
                        node.withStyle(updated)
                    } else {
                        node
                    }
                }
            )
        } else {
            updateToolStyle(updated)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    println(
                        "KEY=${event.key}, " +
                                "CTRL=${event.isCtrlPressed}, " +
                                "UNDO=${history.undoStack.size}, " +
                                "REDO=${history.redoStack.size}"
                    )
                }
                event.type == KeyEventType.KeyDown && when {
                    event.isCtrlPressed &&
                            !event.isShiftPressed &&
                            !event.isAltPressed &&
                            event.key == Key.S -> {
                        save()
                        true
                    }

                    event.isCtrlPressed &&
                            !event.isShiftPressed &&
                            !event.isAltPressed &&
                            event.key == Key.O -> {
                        open()
                        true
                    }
                    event.isCtrlPressed &&
                            event.key == Key.Z &&
                            !event.isShiftPressed -> {
                        if (history.canUndo) {
                            undo()
                            true
                        } else {
                            false
                        }
                    }

                    event.isCtrlPressed &&
                            (
                                    event.key == Key.Y ||
                                            (
                                                    event.key == Key.Z &&
                                                            event.isShiftPressed
                                                    )
                                    ) -> {
                        if (history.canRedo) {
                            redo()
                            true
                        } else {
                            false
                        }
                    }

                    event.key == Key.Delete &&
                            !event.isCtrlPressed &&
                            !event.isAltPressed -> {
                        if (selectedNodeId != null) {
                            deleteSelectedNode()
                            true
                        } else {
                            false
                        }
                    }

                    event.key == Key.Escape -> {
                        when {
                            selectedNodeId != null -> {
                                widthPreview = null
                                selectedNodeId = null
                                true
                            }

                            selectedTool != EditorTool.SELECT -> {
                                widthPreview = null
                                selectedTool = EditorTool.SELECT
                                true
                            }

                            else -> false
                        }
                    }

                    !event.isCtrlPressed &&
                            !event.isAltPressed &&
                            !event.isMetaPressed -> {
                        val tool = when (event.key) {
                            Key.S -> EditorTool.SELECT
                            Key.D -> EditorTool.PEN
                            Key.E -> EditorTool.ELLIPSE
                            Key.R -> EditorTool.RECTANGLE
                            Key.T -> EditorTool.TEXT
                            Key.W -> EditorTool.PAN
                            else -> null
                        }

                        if (tool != null && !event.isShiftPressed) {
                            selectTool(tool)
                            true
                        } else false
                    }

                    else -> false
                }
            }
            .focusable(),
    ) {
        EditorCanvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewportSize = it }
                .pointerInput(focusRequester) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(
                                PointerEventPass.Initial
                            )

                            if (event.type == PointerEventType.Press) {
                                focusRequester.requestFocus()
                            }
                        }
                    }
                },
            nodes = displayNodes,
            selectedTool = selectedTool,
            camera = camera,
            rectangleStyle = rectangleStyle,
            ellipseStyle = ellipseStyle,
            penStyle = penStyle,
            onCameraChanged = {
                camera = it
            },
            onNodeCreated = { node ->
                widthPreview = null
                commitNodes(history.document.nodes + node)
            },
            onNodeUpdated = { updated ->
                widthPreview = null

                commitNodes(
                    history.document.nodes.map { node ->
                        if (node.id == updated.id) updated
                        else node
                    }
                )
            },
            selectedNodeId = selectedNodeId,
            onNodeSelected = { id ->
                if (id != selectedNodeId) {
                    widthPreview = null
                }

                selectedNodeId = id
            },
        )

        EditorActionBar(
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            zoom = camera.zoom,
            onUndo = ::undo,
            onRedo = ::redo,
            onZoomReset = {
                camera = camera.zoomAt(viewportCentre(), 1f)
            },
            onZoomIn = {
                camera = camera.zoomAt(viewportCentre(), camera.zoom * 1.25f)
            },
            onZoomOut = {
                camera = camera.zoomAt(viewportCentre(), camera.zoom / 1.25f)
            },
            onFitToContent = {
                camera = camera.fitToNodes(
                    nodes = history.document.nodes,
                    viewport = viewportSize,
                    padding = with(density) { 64.dp.toPx() },
                )
            },
            onOpen = ::open,
            onSave = ::save,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )


        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorToolbar(
                selectedTool = selectedTool,
                onToolSelected = { tool ->
                    selectTool(tool)
                    focusRequester.requestFocus()
                },
                orientation = ToolbarOrientation.VERTICAL,
            )

            AnimatedVisibility(
                visible = activeStyle != null,
                enter = fadeIn(
                    animationSpec = tween(180)
                ) + expandVertically(
                    animationSpec = tween(220),
                    expandFrom = Alignment.Top
                ) + slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { -it/3 }
                ),
                exit = fadeOut(
                    animationSpec = tween(120),
                ) + shrinkVertically(
                    animationSpec = tween(180),
                    shrinkTowards = Alignment.Top,
                ) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { -it / 3 },
                ),
            ) {
                activeStyle?.let { style ->
                    StylingToolbar(
                        style = style,
                        onStyleChanged = onStyleChanged,
                        onStylePreview = ::previewStyle,
                        onStyleChangeFinished = ::finishStyleChange,
                        orientation = ToolbarOrientation.VERTICAL
                    )
                }
            }
        }
    }
}

@Composable
@Preview(widthDp = 800, heightDp = 800)
fun EditorScreenPreview() = EditorScreen()
