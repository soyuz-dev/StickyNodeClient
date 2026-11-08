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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.stickynodes.data.rememberDocumentStorage
import org.stickynodes.editor.draw.EditorCanvas
import org.stickynodes.editor.draw.EditorTextOverlay
import org.stickynodes.editor.draw.camera.rememberEditorCameraState
import org.stickynodes.editor.draw.geometry.enclosingBounds
import org.stickynodes.editor.draw.geometry.text.layoutStyle
import org.stickynodes.editor.draw.geometry.measuredSelectionBounds
import org.stickynodes.editor.draw.geometry.transform.resizedTo
import org.stickynodes.editor.draw.geometry.transform.rotatedTo
import org.stickynodes.editor.draw.geometry.selection.visibleNodeIds
import org.stickynodes.editor.draw.geometry.selection.visibleWorldBounds
import org.stickynodes.editor.draw.model.*
import org.stickynodes.editor.draw.style.*
import org.stickynodes.editor.history.rememberEditorDocumentState
import org.stickynodes.editor.tools.*
import kotlin.uuid.Uuid

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
    val textMeasurer = rememberTextMeasurer()


    var activeNudgeKeys by remember {
        mutableStateOf<Set<Key>>(emptySet())
    }

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

    var textEditSession by remember {
        mutableStateOf<TextEditSession?>(null)
    }

    var textDraft by remember {
        mutableStateOf("")
    }

    val focusRequester = remember {
        FocusRequester()
    }

    var isInspectorEditing by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val scope = rememberCoroutineScope()
    val storage = rememberDocumentStorage()


    val editingTextId =
        (textEditSession as? TextEditSession.Edit)?.nodeId

    val renderedNodes = if (editingTextId != null) {
        displayNodes.filterNot { it.id == editingTextId }
    } else {
        displayNodes
    }


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
                if (documentState.selectedNodeIds.isNotEmpty()) {
                    documentState.deleteSelectedNodes()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.DuplicateSelection -> {
                if (documentState.selectedNodeIds.isNotEmpty()) {
                    documentState.duplicateSelectedNodes()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.SelectAll -> {
                documentState.selectAllNodes()
                true
            }

            EditorShortcut.Escape -> {
                when {
                    documentState.selectedNodeIds.isNotEmpty() -> {
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

            // Nudge needs the physical key for KeyUp tracking.
            // It is handled in onPreviewKeyEvent.
            is EditorShortcut.Nudge -> false

            EditorShortcut.Cut -> {
                if (documentState.selectedNodeIds.isNotEmpty()) {
                    documentState.cutSelectedNodes()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.Copy -> {
                if(documentState.selectedNodeIds.isNotEmpty()) {
                    documentState.copySelectedNodes()
                    true
                } else {
                    false
                }
            }

            EditorShortcut.Paste -> {
                if (documentState.clipboard.hasContent) {
                    documentState.pasteNodes()
                    true
                } else {
                    false
                }
            }
            EditorShortcut.GroupSelection -> {
                documentState.groupSelectedMembers()
            }

            EditorShortcut.UngroupSelection -> {
                documentState.ungroupSelectedMembers()
            }
        }

    // The existing editing tool determines which
    // style controls appear.
    val selectedNode = documentState.selectedNode

    val styleToolbarType: StyleToolbarType? =
        when (selectedTool) {
            EditorTool.RECTANGLE,
            EditorTool.ELLIPSE -> StyleToolbarType.SHAPE

            EditorTool.PEN -> StyleToolbarType.FREEHAND
            EditorTool.TEXT -> StyleToolbarType.TEXT
            EditorTool.SELECT -> selectedNode?.styleToolbarType()
            EditorTool.PAN -> null
        }

    val editableNode =
        if (selectedTool == EditorTool.SELECT) selectedNode
        else null

    val activeStyle: NodeStyle? =
        when (styleToolbarType) {
            StyleToolbarType.SHAPE ->
                (editableNode as? EditorShape)
                    ?.let { documentState.selectedStyle }
                    ?: if (selectedTool == EditorTool.ELLIPSE) {
                        ellipseStyle
                    } else {
                        rectangleStyle
                    }

            StyleToolbarType.FREEHAND ->
                (editableNode as? EditorPath)
                    ?.let { documentState.selectedStyle }
                    ?: penStyle

            StyleToolbarType.TEXT ->
                (editableNode as? EditorText)
                    ?.let { documentState.selectedStyle }
                    ?: textStyle

            null -> null
        }

    val activeFontSize =
        (editableNode as? EditorText)?.fontSize
            ?: defaultFontSize

    fun updateDefaultStyle(style: NodeStyle) {
        when (styleToolbarType) {
            StyleToolbarType.SHAPE -> {
                if (selectedTool == EditorTool.ELLIPSE) {
                    ellipseStyle = style
                } else {
                    rectangleStyle = style
                }
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
            documentState.updateNode(
                textNode.copy(fontSize = size)
            )
        } else {
            defaultFontSize = size
        }
    }

    fun cancelTextEdit() {
        textEditSession = null
        textDraft = ""
        focusRequester.requestFocus()
    }

    fun commitTextEdit() {
        val session = textEditSession ?: return

        when(session) {
            is TextEditSession.Create -> {
                if(textDraft.isNotBlank()) {
                    documentState.addNode(
                        EditorText(
                            id = Uuid.random(),
                            x = session.position.x,
                            y = session.position.y,
                            text = textDraft,
                            fontSize = session.fontSize
                        )
                    )
                }
            }

            is TextEditSession.Edit -> {
                val original = documentState.nodes
                    .filterIsInstance<EditorText>()
                    .firstOrNull { it.id == session.nodeId }

                if(original != null && original.text != textDraft) {
                    documentState.updateNode(
                        original.copy(text = textDraft)
                    )
                }
            }
        }
        cancelTextEdit()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged { focusState ->
                if (
                    !focusState.hasFocus &&
                    activeNudgeKeys.isNotEmpty()
                ) {
                    documentState.finishNudge()
                    activeNudgeKeys = emptySet()
                }
            }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (isInspectorEditing) {
                    false
                } else {
                    val isArrowKey = when (event.key) {
                        Key.DirectionLeft,
                        Key.DirectionRight,
                        Key.DirectionUp,
                        Key.DirectionDown -> true

                        else -> false
                    }

                    when {
                        // Text editing takes priority.
                        textEditSession != null -> {
                            if (activeNudgeKeys.isNotEmpty()) {
                                documentState.finishNudge()
                                activeNudgeKeys = emptySet()
                            }

                            if (
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.Escape
                            ) {
                                commitTextEdit()
                                true
                            } else {
                                false
                            }
                        }

                        // Release an active arrow key.
                        event.type == KeyEventType.KeyUp &&
                                isArrowKey &&
                                event.key in activeNudgeKeys -> {

                            activeNudgeKeys =
                                activeNudgeKeys - event.key

                            if (activeNudgeKeys.isEmpty()) {
                                documentState.finishNudge()
                            }

                            true
                        }

                        event.type == KeyEventType.KeyDown -> {
                            when (
                                val shortcut = event.editorShortcut()
                            ) {
                                is EditorShortcut.Nudge -> {
                                    if (
                                        documentState.selectedNodeIds
                                            .isEmpty()
                                    ) {
                                        false
                                    } else {
                                        if (activeNudgeKeys.isEmpty()) {
                                            documentState.beginNudge()
                                        }

                                        activeNudgeKeys =
                                            activeNudgeKeys + event.key

                                        documentState.updateNudge(
                                            shortcut.delta
                                        )

                                        true
                                    }
                                }

                                null -> false

                                else -> {
                                    // Complete movement before
                                    // executing another command.
                                    if (activeNudgeKeys.isNotEmpty()) {
                                        documentState.finishNudge()
                                        activeNudgeKeys = emptySet()
                                    }

                                    executeShortcut(shortcut)
                                }
                            }
                        }

                        else -> false
                    }
                }
            }
            .focusable(),
    ) {
        EditorCanvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(
                    cameraState::updateViewportSize
                )
                .pointerInput(focusRequester) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(
                                PointerEventPass.Initial
                            )

                            if (
                                event.type == PointerEventType.Press
                            ) {
                                if (activeNudgeKeys.isNotEmpty()) {
                                    documentState.finishNudge()
                                    activeNudgeKeys = emptySet()
                                }

                                focusRequester.requestFocus()
                            }
                        }
                    }
                },
            nodes = renderedNodes,
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
            selectedMembers = documentState.selectedMembers,
            ancestorGroups = documentState.selectedMemberIds
                .mapNotNull { id ->
                    documentState.document.members.pathToMember(id)
                }.let { paths ->
                    if(paths.isEmpty()) listOf()
                    else {
                        paths
                            .first()
                            .dropLast(1)
                            .filterIsInstance<EditorGroup>()
                            .filter { group ->
                                paths.all { path ->
                                    path.any { it.id == group.id }
                                }
                            }
                    }
                },
            onNodeSelected = documentState::selectNode,
            onNodesSelected = documentState::selectNodes,
            onNodeSelectionToggled =
                documentState::toggleNodeSelection,
            onHitSelection = { hitNodeId, extendSelection ->
                documentState.selectHitNode(
                    hitNodeId = hitNodeId,
                    extendSelection = extendSelection,
                )
                documentState.selectedNodeIds
            },
            onTextPlacement = { position ->
                textEditSession = TextEditSession.Create(
                    position = position,
                    fontSize = defaultFontSize
                )
                textDraft = ""
            },
            onTextEdit = { node ->
                textEditSession = node.toEditSession()
                textDraft = node.text
            }
        )

        textEditSession?.let { session ->
            EditorTextOverlay(
                position = session.position,
                text = textDraft,
                camera = cameraState.camera,
                fontSize = session.fontSize,
                onTextChanged = {
                    textDraft = it
                },
                onCommit = ::commitTextEdit,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selectedNodeIds.isNotEmpty(),
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120))
            ) {

                ArrangementBar(
                    canBringToFront = documentState.canBringSelectionToFront,
                    canBringForward = documentState.canBringSelectionForward,
                    canSendBackward = documentState.canSendSelectionBackward,
                    canSendToBack = documentState.canSendSelectionToBack,
                    onBringToFront = documentState::bringSelectionToFront,
                    onBringForward = documentState::bringSelectionForward,
                    onSendBackward = documentState::sendSelectionBackward,
                    onSendToBack = documentState::sendSelectionToBack,
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
                        selectionBounds = { node ->
                            node.measuredSelectionBounds(
                                textMeasurer = textMeasurer,
                                textStyle = { text ->
                                    text.layoutStyle(density)
                                }
                            )
                        }
                    )
                },
                onOpen = ::open,
                onSave = ::save,
                onSelectVisible = {
                    val viewport = cameraState.viewportSize

                    if (viewport.width > 0 && viewport.height > 0) {
                        val visibleBounds =
                            cameraState.camera.visibleWorldBounds(viewport)

                        val visibleIds = documentState.nodes.visibleNodeIds(
                            viewportBounds = visibleBounds,
                            selectionBounds = { node ->
                                node.measuredSelectionBounds(
                                    textMeasurer = textMeasurer,
                                    textStyle = { text ->
                                        text.layoutStyle(density)
                                    },
                                )
                            }
                        )

                        documentState.selectNodes(visibleIds)
                    }
                }
            )
        }



        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
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
                enter = fadeIn(
                    animationSpec = tween(180)
                ) + expandVertically(
                    animationSpec = tween(220),
                    expandFrom = Alignment.Top
                ) + slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { -it / 3 }
                ),
                exit = fadeOut(
                    animationSpec = tween(120)
                ) + shrinkVertically(
                    animationSpec = tween(180),
                    shrinkTowards = Alignment.Top
                ) + slideOutVertically(
                    animationSpec = tween(180),
                    targetOffsetY = { -it / 3 }
                ),
            ) {
                val style = activeStyle

                when (styleToolbarType) {
                    StyleToolbarType.SHAPE -> {
                        if (style != null) {
                            ShapeStyleToolbar(
                                style = style,
                                onStyleChanged = onStyleChanged,
                                onStylePreview = ::previewStyle,
                                onStyleChangeFinished =
                                    documentState::finishStyleChange,
                                orientation = ToolbarOrientation.VERTICAL,
                            )
                        }
                    }

                    StyleToolbarType.FREEHAND -> {
                        if (style != null) {
                            FreehandStyleToolbar(
                                style = style,
                                onStyleChanged = onStyleChanged,
                                onStylePreview = ::previewStyle,
                                onStyleChangeFinished =
                                    documentState::finishStyleChange,
                                orientation = ToolbarOrientation.VERTICAL,
                            )
                        }
                    }

                    StyleToolbarType.TEXT -> {
                        if (style != null) {
                            TextStyleToolbar(
                                style = style,
                                fontSize = activeFontSize,
                                onStyleChanged = onStyleChanged,
                                onFontSizeChanged = onFontSizeChanged,
                                orientation = ToolbarOrientation.VERTICAL,
                            )
                        }
                    }

                    null -> Unit
                }
            }
        }

        AnimatedVisibility(
            visible = documentState.canAlignSelection,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
        ) {
            val selectionBounds: (EditorNode) -> Rect = { node ->
                node.measuredSelectionBounds(
                    textMeasurer = textMeasurer,
                    textStyle = { text ->
                        text.layoutStyle(density)
                    },
                )
            }

            LayoutBar(
                canDistribute = documentState.canDistributeSelection,
                canMatchSize = documentState.canMatchShapeSizes,
                onAlign = { alignment ->
                    documentState.alignSelectedNodes(
                        alignment = alignment,
                        selectionBounds = selectionBounds
                    )
                },
                onDistribute = { distribution ->
                    documentState.distributeSelectedNodes(
                        distribution = distribution,
                        selectionBounds = selectionBounds
                    )
                },
                onMatchSize = { sizing ->
                    documentState.matchSelectedShapeSizes(sizing)
                }
            )
        }

        documentState.selectedNode?.let { node ->
            key(node.id) {
                val shape = node as? EditorShape

                TransformInspector(
                    position = Offset(node.x, node.y),
                    onPositionChanged = { position ->
                        documentState.positionSelectedNodeAt(position)
                    },
                    dimensions = shape?.let {
                        Size(it.bounds.width, it.bounds.height)
                    },
                    onDimensionsChanged = { size ->
                        shape?.let {
                            documentState.updateNode(
                                it.resizedTo(
                                    width = size.width,
                                    height = size.height,
                                )
                            )
                        }
                    },
                    rotation = shape?.rotation,
                    onRotationChanged = { rotation ->
                        shape?.let {
                            documentState.updateNode(
                                it.rotatedTo(
                                    rotation = rotation
                                )
                            )
                        }
                    },
                    onEditingChanged = { isInspectorEditing = it },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                )
            }
        }

        if (selectedNodeIds.size > 1) {
            val selectedNodes = nodes.filter { it.id in selectedNodeIds}

            val selectionBounds: (EditorNode) -> Rect = { node ->
                node.measuredSelectionBounds(
                    textMeasurer = textMeasurer,
                    textStyle = { text -> text.layoutStyle(density)}
                )
            }

            val bounds = selectedNodes.map(selectionBounds)
            val groupBounds = bounds.enclosingBounds()

            val canResizeGroup = selectedNodes.all { node ->
                node is EditorShape && node.rotation == 0f
            }

            val canRotateGroup = selectedNodes.all { node ->
                node is EditorShape
            }

            groupBounds?.let { groupBounds ->
                key(selectedNodeIds) {
                    TransformInspector(
                        position = groupBounds.topLeft,
                        onPositionChanged = { position ->
                            documentState.positionSelectedNodesAt(
                                position = position,
                                selectionBounds = selectionBounds,
                            )
                        },
                        dimensions = groupBounds.size,
                        onDimensionsChanged = documentState::resizeSelectedShapesTo,
                        rotation = if (canRotateGroup) 0f else null,
                        onRotationChanged = documentState::rotateSelectedShapesBy,
                        readOnly = true,
                        positionReadOnly = false,
                        dimensionsReadOnly = !canResizeGroup,
                        rotationReadOnly = false,
                        rotationLabel = "Δ°",
                        onEditingChanged = { isInspectorEditing = it },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
@Preview(widthDp = 1000, heightDp = 1000)
fun EditorScreenPreview() = EditorScreen()

private fun EditorNode.styleToolbarType(): StyleToolbarType =
    when (this) {
        is EditorShape -> StyleToolbarType.SHAPE
        is EditorText -> StyleToolbarType.TEXT
        is EditorPath -> StyleToolbarType.FREEHAND
    }