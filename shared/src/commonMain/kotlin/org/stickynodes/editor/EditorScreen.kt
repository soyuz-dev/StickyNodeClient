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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.stickynodes.data.rememberDocumentStorage
import org.stickynodes.editor.draw.EditorCanvas
import org.stickynodes.editor.draw.EditorTextOverlay
import org.stickynodes.editor.draw.camera.rememberEditorCameraState
import org.stickynodes.editor.draw.model.*
import org.stickynodes.editor.draw.style.*
import org.stickynodes.editor.history.rememberEditorDocumentState
import org.stickynodes.editor.tools.*
import kotlin.uuid.Uuid

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
) {
    val cameraState = rememberEditorCameraState()
    val density = LocalDensity.current

    var selectedTool by remember {
        mutableStateOf(EditorTool.SELECT)
    }

    val documentState = rememberEditorDocumentState()
    val nodes = documentState.nodes
    val selectedNodeIds = documentState.selectedNodeIds
    val displayNodes = documentState.displayNodes

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

    var textStyle by remember {
        mutableStateOf(
            NodeStyle(
                fill = FillStyle.Solid(0xFF0D0D0D.toInt()),
                stroke = null,
            )
        )
    }

    var defaultFontSize by remember {
        mutableStateOf(24f)
    }

    var textEditPosition by remember {
        mutableStateOf<Offset?>(null)
    }

    var textDraft by remember {
        mutableStateOf("")
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
                storage.save(documentState.document)
            } catch (e: Exception) {
                println("Failed to save document: ${e.message}")
            }
        }
    }

    fun open() {
        scope.launch {
            try {
                val document = storage.open() ?: return@launch
                documentState.open(document)
            } catch (e: Exception) {
                println("Failed to open document: ${e.message}")
            }
        }
    }

    fun selectTool(tool: EditorTool) {
        documentState.cancelStylePreview()
        documentState.clearSelection()
        selectedTool = tool
    }


    fun executeShortcut(shortcut: EditorShortcut): Boolean =
        when (shortcut) {
            EditorShortcut.Save -> {
                save()
                true
            }

            EditorShortcut.Open -> {
                open()
                true
            }

            EditorShortcut.Undo -> {
                if (documentState.canUndo) {
                    documentState.undo()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.Redo -> {
                if (documentState.canRedo) {
                    documentState.redo()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.DeleteSelection -> {
                if (selectedNodeIds.isNotEmpty()) {
                    documentState.deleteSelectedNodes()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.DuplicateSelection -> {
                if (selectedNodeIds.isNotEmpty()) {
                    documentState.duplicateSelectedNodes()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.Escape -> {
                when {
                    selectedNodeIds.isNotEmpty() -> {
                        documentState.clearSelection()
                        true
                    }

                    selectedTool != EditorTool.SELECT -> {
                        documentState.cancelStylePreview()
                        selectedTool = EditorTool.SELECT
                        true
                    }

                    else -> false
                }
            }

            is EditorShortcut.SelectTool -> {
                selectTool(shortcut.tool)
                true
            }
        }


    // The existing editing tool determines which style controls appear.
    // SELECT uses the single selected node's type instead.
    val selectedNode = documentState.selectedNode
    val styleToolbarType: StyleToolbarType? = when (selectedTool) {
        EditorTool.RECTANGLE, EditorTool.ELLIPSE -> StyleToolbarType.SHAPE
        EditorTool.PEN -> StyleToolbarType.FREEHAND
        EditorTool.TEXT -> StyleToolbarType.TEXT
        EditorTool.SELECT -> selectedNode?.styleToolbarType()
        EditorTool.PAN -> null
    }

    val editableNode = if (selectedTool == EditorTool.SELECT) selectedNode else null
    val activeStyle: NodeStyle? = when (styleToolbarType) {
        StyleToolbarType.SHAPE ->
            (editableNode as? EditorShape)?.let { documentState.selectedStyle }
                ?: if (selectedTool == EditorTool.ELLIPSE) ellipseStyle else rectangleStyle
        StyleToolbarType.FREEHAND ->
            (editableNode as? EditorPath)?.let { documentState.selectedStyle } ?: penStyle
        StyleToolbarType.TEXT ->
            (editableNode as? EditorText)?.let { documentState.selectedStyle } ?: textStyle
        null -> null
    }

    val activeFontSize = (editableNode as? EditorText)?.fontSize ?: defaultFontSize

    fun updateDefaultStyle(style: NodeStyle) {
        when (styleToolbarType) {
            StyleToolbarType.SHAPE -> {
                if (selectedTool == EditorTool.ELLIPSE) ellipseStyle = style
                else rectangleStyle = style
            }
            StyleToolbarType.FREEHAND -> penStyle = style
            StyleToolbarType.TEXT -> textStyle = style
            null -> Unit
        }
    }

    fun previewStyle(style: NodeStyle) {
        if (editableNode != null) {
            documentState.previewSelectedStyle(style)
        } else {
            updateDefaultStyle(style)
        }
    }

    val onStyleChanged: (NodeStyle) -> Unit = { updated ->
        documentState.cancelStylePreview()
        if (editableNode != null) {
            documentState.commitSelectedStyle(updated)
        } else {
            updateDefaultStyle(updated)
        }
    }

    val onFontSizeChanged: (Float) -> Unit = { size ->
        val textNode = editableNode as? EditorText
        if (textNode != null) {
            documentState.updateNode(textNode.copy(fontSize = size))
        } else {
            defaultFontSize = size
        }
    }

    fun cancelTextEdit() {
        textEditPosition = null
        textDraft = ""
        focusRequester.requestFocus()
    }

    fun commitTextEdit() {
        val position = textEditPosition ?: return

        if (textDraft.isNotBlank()) {
            documentState.addNode(
                EditorText(
                    id = Uuid.random(),
                    x = position.x,
                    y = position.y,
                    text = textDraft,
                    fontSize = defaultFontSize,
                    style = textStyle,
                )
            )
        }

        cancelTextEdit()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {event ->

                if (event.type == KeyEventType.KeyDown) {
                    println(
                        "Key: ${event.key}, Ctrl: ${event.isCtrlPressed}, " +
                                "Shortcut: ${event.editorShortcut()}"
                    )
                }
                if (textEditPosition != null) {
                    if (
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.Escape
                    ) {
                        commitTextEdit()
                        true
                    } else {
                        false
                    }
                } else {
                    event.editorShortcut()?.let(::executeShortcut) ?: false
                }
            }
            .focusable(),
    ) {
        EditorCanvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(cameraState::updateViewportSize)
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
            camera = cameraState.camera,
            rectangleStyle = rectangleStyle,
            ellipseStyle = ellipseStyle,
            penStyle = penStyle,
            onCameraChanged = cameraState::updateCamera,
            onNodeCreated = documentState::addNode,
            onNodeUpdated = documentState::updateNode,
            onNodesUpdated = documentState::updateNodes,
            selectedNodeIds = selectedNodeIds,
            onNodeSelected = documentState::selectNode,
            onNodesSelected = documentState::selectNodes,
            onNodeSelectionToggled = documentState::toggleNodeSelection,
            onTextPlacement = { position ->
                textEditPosition = position
                textDraft = ""
            }
        )

        textEditPosition?.let { position ->
            EditorTextOverlay(
                position = position,
                text = textDraft,
                camera = cameraState.camera,
                fontSize = defaultFontSize,
                onTextChanged = {
                    textDraft = it
                },
                onCommit = ::commitTextEdit,
            )
        }

        EditorActionBar(
            canUndo = documentState.canUndo,
            canRedo = documentState.canRedo,
            zoom = cameraState.camera.zoom,
            onUndo = documentState::undo,
            onRedo = documentState::redo,
            onZoomReset = cameraState::resetZoom,
            onZoomIn = cameraState::zoomIn,
            onZoomOut = cameraState::zoomOut,
            onFitToContent = {
                cameraState.fitToNodes(
                    nodes = documentState.nodes,
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
                canDelete = selectedNodeIds.isNotEmpty(),
                onDelete = {
                    documentState.deleteSelectedNodes()
                    focusRequester.requestFocus()
                },
                onToolSelected = { tool ->
                    selectTool(tool)
                    focusRequester.requestFocus()
                },
                orientation = ToolbarOrientation.VERTICAL,
            )

            AnimatedVisibility(
                visible = styleToolbarType != null,
                enter = fadeIn(animationSpec = tween(180)) +
                        expandVertically(animationSpec = tween(220), expandFrom = Alignment.Top) +
                        slideInVertically(animationSpec = tween(220), initialOffsetY = { -it / 3 }),
                exit = fadeOut(animationSpec = tween(120)) +
                        shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) +
                        slideOutVertically(animationSpec = tween(180), targetOffsetY = { -it / 3 }),
            ) {
                val style = activeStyle
                when (styleToolbarType) {
                    StyleToolbarType.SHAPE -> if (style != null) {
                        ShapeStyleToolbar(
                            style = style,
                            onStyleChanged = onStyleChanged,
                            onStylePreview = ::previewStyle,
                            onStyleChangeFinished = documentState::finishStyleChange,
                            orientation = ToolbarOrientation.VERTICAL,
                        )
                    }
                    StyleToolbarType.FREEHAND -> if (style != null) {
                        FreehandStyleToolbar(
                            style = style,
                            onStyleChanged = onStyleChanged,
                            onStylePreview = ::previewStyle,
                            onStyleChangeFinished = documentState::finishStyleChange,
                            orientation = ToolbarOrientation.VERTICAL,
                        )
                    }
                    StyleToolbarType.TEXT -> if (style != null) {
                        TextStyleToolbar(
                            style = style,
                            fontSize = activeFontSize,
                            onStyleChanged = onStyleChanged,
                            onFontSizeChanged = onFontSizeChanged,
                            orientation = ToolbarOrientation.VERTICAL,
                        )
                    }
                    null -> Unit
                }
            }
        }
    }
}

@Composable
@Preview(widthDp = 800, heightDp = 800)
fun EditorScreenPreview() = EditorScreen()

private fun EditorNode.styleToolbarType(): StyleToolbarType =
    when (this) {
        is EditorShape -> StyleToolbarType.SHAPE
        is EditorText -> StyleToolbarType.TEXT
        is EditorPath -> StyleToolbarType.FREEHAND
    }