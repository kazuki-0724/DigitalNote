package com.waju.factory.digitalnote.ui.screens

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasDesktopColor
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.CanvasUiState
import com.waju.factory.digitalnote.ui.canvas.DrawStroke
import com.waju.factory.digitalnote.ui.canvas.DrawingTool
import com.waju.factory.digitalnote.ui.canvas.LaserTrail
import com.waju.factory.digitalnote.ui.canvas.StickyNote
import com.waju.factory.digitalnote.ui.canvas.StrokePoint
import com.waju.factory.digitalnote.ui.canvas.WHITEBOARD_PAGE_INDEX
import com.waju.factory.digitalnote.ui.canvas.applyPressureCurve
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun CanvasScreen(
    uiState: CanvasUiState,
    modifier: Modifier = Modifier,
    onToolChanged: (DrawingTool) -> Unit,
    onModeChanged: (CanvasMode) -> Unit,
    onBackgroundStyleChanged: (CanvasBackgroundStyle) -> Unit,
    onInputModeChanged: (CanvasInputMode) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddPage: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onColorChanged: (Int) -> Unit,
    onPaletteColorChanged: (Int, Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onTransform: (zoomChange: Float, panX: Float, panY: Float) -> Unit,
    onResetTransform: () -> Unit,
    onFitToPage: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    onStrokeStart: (x: Float, y: Float, pressure: Float, timestamp: Long) -> Unit,
    onStrokeMove: (x: Float, y: Float, pressure: Float, timestamp: Long) -> Unit,
    onStrokeEnd: () -> Unit,
    onStrokeCancel: () -> Unit,
    onAddStickyNote: (x: Float, y: Float) -> Long,
    onUpdateStickyNoteText: (id: Long, text: String) -> Unit,
    onMoveStickyNote: (id: Long, newX: Float, newY: Float) -> Unit,
    onResizeStickyNote: (id: Long, newWidth: Float, newHeight: Float) -> Unit,
    onUpdateStickyNoteStyle: (id: Long, color: Color, fontSize: Float) -> Unit,
    onDeleteStickyNote: (id: Long) -> Unit,
    onToggleReadOnly: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit
) {
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showPaletteDialog by rememberSaveable { mutableStateOf(false) }
    var inTransformGesture by rememberSaveable { mutableStateOf(false) }
    var lastSpan by rememberSaveable { mutableStateOf(0f) }
    var lastFocusX by rememberSaveable { mutableStateOf(0f) }
    var lastFocusY by rememberSaveable { mutableStateOf(0f) }
    var sidebarOpen by rememberSaveable { mutableStateOf(false) }
    var textTouchStartX by rememberSaveable { mutableStateOf(0f) }
    var textTouchStartY by rememberSaveable { mutableStateOf(0f) }
    var textTouchStartTime by rememberSaveable { mutableStateOf(0L) }
    var textTouchMoved by rememberSaveable { mutableStateOf(false) }
    var textTouchHandledByCanvas by rememberSaveable { mutableStateOf(false) }
    var selectedStickyNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transformStickyNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var styleStickyNoteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.tool) {
        if (uiState.tool != DrawingTool.TEXT) {
            selectedStickyNoteId = null
            transformStickyNoteId = null
            styleStickyNoteId = null
        }
    }

    LaunchedEffect(uiState.stickyNotes) {
        val noteIds = uiState.stickyNotes.map { it.id }.toSet()
        if (selectedStickyNoteId !in noteIds) selectedStickyNoteId = null
        if (transformStickyNoteId !in noteIds) transformStickyNoteId = null
        if (styleStickyNoteId !in noteIds) styleStickyNoteId = null
    }

    // B5 実寸 (182mm × 257mm) をスクリーン画素数へ変換
    // 1mm = 160dp / 25.4; LocalDensity でdp → px
    val density = LocalDensity.current.density
    val b5WidthWorld  = 182f * 160f / 25.4f * density   // px at scale=1.0
    val b5HeightWorld = 257f * 160f / 25.4f * density   // px at scale=1.0

    val visibleStrokes = uiState.strokes.filter {
        if (uiState.mode == CanvasMode.WHITEBOARD) {
            it.pageIndex == WHITEBOARD_PAGE_INDEX
        } else {
            it.pageIndex == uiState.currentPageIndex
        }
    }
    val currentPage = if (uiState.mode == CanvasMode.WHITEBOARD) WHITEBOARD_PAGE_INDEX else uiState.currentPageIndex
    val visibleStickyNotes = uiState.stickyNotes.filter { it.pageIndex == currentPage }
    val nowMillis = System.currentTimeMillis()
    val visibleLaserTrails = uiState.laserTrails.mapNotNull { trail ->
        val age = nowMillis - trail.createdAtMillis
        if (age >= LASER_TRAIL_KEEP_MS) {
            null
        } else {
            // sqrt曲線: 最初は明るさを維持し、末尾に向かってなめらかに消えていく
            val t = age.toFloat() / LASER_TRAIL_KEEP_MS.toFloat()
            val alpha = sqrt(1f - t)
            trail to alpha.coerceIn(0f, 1f)
        }
    }
    val safePaletteIndex = uiState.selectedColorIndex.coerceIn(0, uiState.palette.lastIndex.coerceAtLeast(0))
    val editingColor = uiState.palette.getOrElse(safePaletteIndex) { Color.Black }
    val longPressTimeoutMillis = ViewConfiguration.getLongPressTimeout().toLong()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CanvasTopBarSection(
                uiState = uiState,
                sidebarOpen = sidebarOpen,
                onToolChanged = onToolChanged,
                onUndo = onUndo,
                onRedo = onRedo,
                onClear = onClear,
                onColorChanged = onColorChanged,
                onOpenPalette = { showPaletteDialog = true },
                onOpenSettings = { showSettingsDialog = true },
                onToggleSidebar = { sidebarOpen = !sidebarOpen },
                onToggleReadOnly = onToggleReadOnly,
                onPrevPage = onPrevPage,
                onNextPage = onNextPage
            )

            if (showPaletteDialog) {
                PaletteEditorDialog(
                    palette = uiState.palette,
                    selectedColorIndex = uiState.selectedColorIndex,
                    editingColorIndex = safePaletteIndex,
                    editingColor = editingColor,
                    strokeWidth = uiState.baseStrokeWidth,
                    onDismiss = { showPaletteDialog = false },
                    onSelectColor = onColorChanged,
                    onStrokeWidthChanged = onStrokeWidthChanged,
                    onPaletteColorChanged = { color ->
                        onPaletteColorChanged(safePaletteIndex, color)
                    }
                )
            }

            if (showSettingsDialog) {
                CanvasSettingsDialog(
                    uiState = uiState,
                    onDismiss = { showSettingsDialog = false },
                    onModeChanged = onModeChanged,
                    onBackgroundStyleChanged = onBackgroundStyleChanged,
                    onInputModeChanged = onInputModeChanged,
                    onStrokeWidthChanged = onStrokeWidthChanged,
                    onSensitivityChanged = onSensitivityChanged,
                    onResetTransform = onResetTransform
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (uiState.mode == CanvasMode.PAGE) CanvasDesktopColor
                            else uiState.backgroundColor
                        )
                        .pointerInteropFilter { event ->
                            val isReadOnly = uiState.tool == DrawingTool.READONLY
                            if (isReadOnly && event.pointerCount < 2) return@pointerInteropFilter true

                            when (event.actionMasked) {
                                MotionEvent.ACTION_POINTER_DOWN -> {
                                    if (uiState.tool == DrawingTool.TEXT) {
                                        textTouchHandledByCanvas = false
                                        textTouchMoved = true
                                    }
                                    if (event.pointerCount >= 2) {
                                        inTransformGesture = true
                                        lastSpan = calculateSpan(event)
                                        val focus = calculateFocus(event)
                                        lastFocusX = focus.x
                                        lastFocusY = focus.y
                                        onStrokeCancel()
                                    }
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    if (uiState.tool == DrawingTool.TEXT && event.pointerCount == 1) {
                                        if (!textTouchHandledByCanvas) {
                                            return@pointerInteropFilter false
                                        }
                                        if (!textTouchMoved) {
                                            val dx = abs(event.x - textTouchStartX)
                                            val dy = abs(event.y - textTouchStartY)
                                            if (dx >= 10f || dy >= 10f) {
                                                textTouchMoved = true
                                            }
                                        }
                                        return@pointerInteropFilter true
                                    }

                                    if (event.pointerCount >= 2) {
                                        val newSpan = calculateSpan(event)
                                        val focus = calculateFocus(event)
                                        val zoomChange = if (lastSpan > 0f) newSpan / lastSpan else 1f
                                        val panX = focus.x - lastFocusX
                                        val panY = focus.y - lastFocusY


                                        onTransform(zoomChange, panX, panY)
                                        lastSpan = newSpan
                                        lastFocusX = focus.x
                                        lastFocusY = focus.y
                                        return@pointerInteropFilter true
                                    }

                                    if (!inTransformGesture && canHandleDrawInput(event, uiState.inputMode)) {
                                        val world = toWorldPoint(event.x, event.y, uiState)
                                        for (index in 0 until event.historySize) {
                                            val historical = toWorldPoint(
                                                event.getHistoricalX(index),
                                                event.getHistoricalY(index),
                                                uiState
                                            )
                                            onStrokeMove(
                                                historical.x,
                                                historical.y,
                                                event.getHistoricalPressure(index),
                                                event.getHistoricalEventTime(index)
                                            )
                                        }
                                        onStrokeMove(world.x, world.y, event.getPressure(0), event.eventTime)
                                    }
                                }

                                MotionEvent.ACTION_POINTER_UP -> {
                                    if (event.pointerCount <= 2) {
                                        inTransformGesture = false
                                    }
                                }

                                MotionEvent.ACTION_DOWN -> {
                                    if (uiState.tool == DrawingTool.TEXT) {
                                        if (styleStickyNoteId != null) {
                                            textTouchHandledByCanvas = false
                                            return@pointerInteropFilter false
                                        }
                                        textTouchStartX = event.x
                                        textTouchStartY = event.y
                                        textTouchStartTime = event.eventTime
                                        textTouchMoved = false
                                        val world = toWorldPoint(event.x, event.y, uiState)
                                        val tappedNoteId = findStickyNoteAt(world, visibleStickyNotes)
                                        textTouchHandledByCanvas = tappedNoteId == null
                                        // 既存付箋のタップは子Composableへ渡し、ヘッダーボタンのクリックを優先する。
                                        return@pointerInteropFilter textTouchHandledByCanvas
                                    }

                                    if (!inTransformGesture && canHandleDrawInput(event, uiState.inputMode)) {
                                        val world = toWorldPoint(event.x, event.y, uiState)
                                        onStrokeStart(world.x, world.y, event.getPressure(0), event.eventTime)
                                    }
                                }

                                MotionEvent.ACTION_UP -> {
                                    if (uiState.tool == DrawingTool.TEXT && !textTouchHandledByCanvas) {
                                        return@pointerInteropFilter false
                                    }

                                    if (uiState.tool == DrawingTool.TEXT) {
                                        val world = toWorldPoint(event.x, event.y, uiState)
                                        val tappedNoteId = findStickyNoteAt(world, visibleStickyNotes)
                                        val isSingleFingerLongPress =
                                            event.pointerCount == 1 &&
                                                event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER &&
                                                !textTouchMoved &&
                                                (event.eventTime - textTouchStartTime) >= longPressTimeoutMillis

                                        if (tappedNoteId != null) {
                                            selectedStickyNoteId = tappedNoteId
                                        } else if (isSingleFingerLongPress) {
                                            val createdId = onAddStickyNote(world.x, world.y)
                                            selectedStickyNoteId = createdId
                                            transformStickyNoteId = null
                                            styleStickyNoteId = null
                                        } else {
                                            selectedStickyNoteId = null
                                            transformStickyNoteId = null
                                            styleStickyNoteId = null
                                        }
                                        textTouchHandledByCanvas = false
                                        return@pointerInteropFilter true
                                    }

                                    textTouchHandledByCanvas = false

                                    inTransformGesture = false
                                    onStrokeEnd()
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    textTouchHandledByCanvas = false
                                    inTransformGesture = false
                                    onStrokeCancel()
                                }
                            }
                            true
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (uiState.mode == CanvasMode.PAGE) {
                            // ── PAGEモード: B5サイズのページを描画 ──
                            val pageLeft   = uiState.offsetX
                            val pageTop    = uiState.offsetY
                            val pageW      = b5WidthWorld  * uiState.scale
                            val pageH      = b5HeightWorld * uiState.scale

                            // 影 (drop shadow)
                            drawRect(
                                color    = Color(0x33000000),
                                topLeft  = Offset(pageLeft + 6f, pageTop + 6f),
                                size     = Size(pageW, pageH)
                            )
                            // 用紙白背景
                            drawRect(
                                color   = uiState.backgroundColor,
                                topLeft = Offset(pageLeft, pageTop),
                                size    = Size(pageW, pageH)
                            )
                            // 用紙枠線
                            drawRect(
                                color   = Color(0xFFB0BAC9),
                                topLeft = Offset(pageLeft, pageTop),
                                size    = Size(pageW, pageH),
                                style   = Stroke(width = 1f)
                            )

                            // ページ範囲にクリップして背景パターン・ストロークを描画
                            clipRect(
                                left   = pageLeft,
                                top    = pageTop,
                                right  = pageLeft + pageW,
                                bottom = pageTop + pageH
                            ) {
                                drawBackgroundPattern(uiState)
                                visibleStrokes.forEach { stroke ->
                                    drawStroke(stroke = stroke, uiState = uiState)
                                }
                                visibleLaserTrails.forEach { (trail, alpha) ->
                                    drawLaserTrail(trail = trail, uiState = uiState, alpha = alpha)
                                }
                                if (uiState.activePoints.isNotEmpty()) {
                                    drawActiveStroke(uiState)
                                }
                            }
                        } else {
                            // ── WHITEBOARDモード: 既存の無限キャンバス動作 ──
                            drawBackgroundPattern(uiState)
                            visibleStrokes.forEach { stroke ->
                                drawStroke(stroke = stroke, uiState = uiState)
                            }
                            visibleLaserTrails.forEach { (trail, alpha) ->
                                drawLaserTrail(trail = trail, uiState = uiState, alpha = alpha)
                            }
                            if (uiState.activePoints.isNotEmpty()) {
                                drawActiveStroke(uiState)
                            }
                        }
                    }

                    visibleStickyNotes.forEach { stickyNote ->
                        key(stickyNote.id) {
                            StickyNoteItem(
                                note = stickyNote,
                                uiState = uiState,
                                palette = uiState.palette,
                                isSelected = stickyNote.id == selectedStickyNoteId,
                                isTransformEnabled = stickyNote.id == transformStickyNoteId,
                                isStyleEditorVisible = stickyNote.id == styleStickyNoteId,
                                onSelect = {
                                    selectedStickyNoteId = stickyNote.id
                                    if (transformStickyNoteId != stickyNote.id) {
                                        transformStickyNoteId = null
                                    }
                                },
                                onToggleTransform = {
                                    selectedStickyNoteId = stickyNote.id
                                    transformStickyNoteId = if (transformStickyNoteId == stickyNote.id) null else stickyNote.id
                                },
                                onToggleStyleEditor = {
                                    selectedStickyNoteId = stickyNote.id
                                    styleStickyNoteId = if (styleStickyNoteId == stickyNote.id) null else stickyNote.id
                                },
                                onTextChange = { text -> onUpdateStickyNoteText(stickyNote.id, text) },
                                onMove = { newX, newY -> onMoveStickyNote(stickyNote.id, newX, newY) },
                                onResize = { newWidth, newHeight -> onResizeStickyNote(stickyNote.id, newWidth, newHeight) },
                                onStyleChange = { color, fontSize ->
                                    onUpdateStickyNoteStyle(stickyNote.id, color, fontSize)
                                },
                                onDelete = {
                                    if (selectedStickyNoteId == stickyNote.id) selectedStickyNoteId = null
                                    if (transformStickyNoteId == stickyNote.id) transformStickyNoteId = null
                                    if (styleStickyNoteId == stickyNote.id) styleStickyNoteId = null
                                    onDeleteStickyNote(stickyNote.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (sidebarOpen) {
            CanvasSidebar(
                uiState = uiState,
                onAddPage = onAddPage,
                onGoToPage = onGoToPage,
                onClose = { sidebarOpen = false }
            )
        }
    }
}

@Composable
private fun StickyNoteItem(
    note: StickyNote,
    uiState: CanvasUiState,
    palette: List<Color>,
    isSelected: Boolean,
    isTransformEnabled: Boolean,
    isStyleEditorVisible: Boolean,
    onSelect: () -> Unit,
    onToggleTransform: () -> Unit,
    onToggleStyleEditor: () -> Unit,
    onTextChange: (String) -> Unit,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onStyleChange: (Color, Float) -> Unit,
    onDelete: () -> Unit
) {
    var dragScreenDeltaX by remember(note.id) { mutableStateOf(0f) }
    var dragScreenDeltaY by remember(note.id) { mutableStateOf(0f) }
    var resizeScreenDeltaX by remember(note.id) { mutableStateOf(0f) }
    var resizeScreenDeltaY by remember(note.id) { mutableStateOf(0f) }
    val currentScale by rememberUpdatedState(uiState.scale)
    val capturedX by rememberUpdatedState(note.x)
    val capturedY by rememberUpdatedState(note.y)
    val capturedWidth by rememberUpdatedState(note.width)
    val capturedHeight by rememberUpdatedState(note.height)
    val density = LocalDensity.current
    val noteWidthDp = with(density) { ((note.width * uiState.scale) + resizeScreenDeltaX).coerceAtLeast(120f).toDp() }
    val noteHeightDp = with(density) { ((note.height * uiState.scale) + resizeScreenDeltaY).coerceAtLeast(88f).toDp() }
    val screenPos = toScreenPoint(Offset(note.x, note.y), uiState)
    val focusRequester = remember { FocusRequester() }
    val canEditText = isSelected && !isTransformEnabled
    val textZoomRatio = (uiState.scale / STICKY_NOTE_TEXT_BASE_SCALE).coerceAtLeast(0.1f)
    val effectiveFontSizeSp = note.fontSize * textZoomRatio
    val effectiveLineHeightSp = (note.fontSize + 6f) * textZoomRatio

    LaunchedEffect(canEditText) {
        if (canEditText) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (screenPos.x + dragScreenDeltaX).roundToInt(),
                    y = (screenPos.y + dragScreenDeltaY).roundToInt()
                )
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(width = noteWidthDp, height = noteHeightDp)
            ) {
                var noteModifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFD8C779),
                        shape = RoundedCornerShape(18.dp)
                    )

                noteModifier = if (isTransformEnabled) {
                    noteModifier.pointerInput(note.id, currentScale) {
                        detectDragGestures(
                            onDragStart = {
                                dragScreenDeltaX = 0f
                                dragScreenDeltaY = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragScreenDeltaX += amount.x
                                dragScreenDeltaY += amount.y
                            },
                            onDragEnd = {
                                onMove(
                                    capturedX + dragScreenDeltaX / currentScale,
                                    capturedY + dragScreenDeltaY / currentScale
                                )
                                dragScreenDeltaX = 0f
                                dragScreenDeltaY = 0f
                            },
                            onDragCancel = {
                                dragScreenDeltaX = 0f
                                dragScreenDeltaY = 0f
                            }
                        )
                    }
                } else if (!canEditText) {
                    noteModifier.clickable(onClick = onSelect)
                } else {
                    noteModifier
                }

                Surface(
                    modifier = noteModifier,
                    color = Color(0xFFFFF3A6),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = if (isSelected) 8.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StickyNoteHeaderButton(
                                active = isTransformEnabled,
                                onClick = {
                                    onSelect()
                                    onToggleTransform()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenWith,
                                    contentDescription = "移動とサイズ変更",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isTransformEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            StickyNoteHeaderButton(
                                active = isStyleEditorVisible,
                                onClick = {
                                    onSelect()
                                    onToggleStyleEditor()
                                }
                            ) {
                                Text(
                                    text = "Aa",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isStyleEditorVisible) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            // スペーサー
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            // 削除ボタン
                            StickyNoteHeaderButton(
                                active = false,
                                onClick = onDelete
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "削除",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            if (canEditText) {
                                val scrollState = rememberScrollState()
                                BasicTextField(
                                    value = note.text,
                                    onValueChange = onTextChange,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusRequester(focusRequester)
                                        .verticalScroll(scrollState),
                                    textStyle = TextStyle(
                                        color = note.color,
                                        fontSize = effectiveFontSizeSp.sp,
                                        lineHeight = effectiveLineHeightSp.sp
                                    ),
                                    cursorBrush = SolidColor(note.color),
                                    decorationBox = { innerTextField ->
                                        if (note.text.isBlank()) {
                                            Text(
                                                text = "ここにそのまま入力",
                                                color = note.color.copy(alpha = 0.45f),
                                                fontSize = effectiveFontSizeSp.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            } else {
                                Text(
                                    text = note.text.ifBlank { "ここにそのまま入力" },
                                    color = if (note.text.isBlank()) note.color.copy(alpha = 0.45f) else note.color,
                                    fontSize = effectiveFontSizeSp.sp,
                                    lineHeight = effectiveLineHeightSp.sp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                if (isTransformEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .pointerInput(note.id, currentScale) {
                                detectDragGestures(
                                    onDragStart = {
                                        resizeScreenDeltaX = 0f
                                        resizeScreenDeltaY = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        resizeScreenDeltaX += amount.x
                                        resizeScreenDeltaY += amount.y
                                    },
                                    onDragEnd = {
                                        onResize(
                                            capturedWidth + resizeScreenDeltaX / currentScale,
                                            capturedHeight + resizeScreenDeltaY / currentScale
                                        )
                                        resizeScreenDeltaX = 0f
                                        resizeScreenDeltaY = 0f
                                    },
                                    onDragCancel = {
                                        resizeScreenDeltaX = 0f
                                        resizeScreenDeltaY = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "┘",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (isStyleEditorVisible) {
                StickyNoteStyleEditor(
                    note = note,
                    palette = palette,
                    onStyleChange = onStyleChange
                )
            }
        }
    }
}

@Composable
private fun StickyNoteHeaderButton(
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
private fun StickyNoteStyleEditor(
    note: StickyNote,
    palette: List<Color>,
    onStyleChange: (Color, Float) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 180.dp, max = 220.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("文字スタイル", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (color.value == note.color.value) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable { onStyleChange(color, note.fontSize) }
                    )
                }
            }
            Text(
                text = "文字サイズ: ${note.fontSize.toInt()} sp",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = note.fontSize,
                onValueChange = { onStyleChange(note.color, it) },
                valueRange = 10f..40f,
                steps = 14
            )
        }
    }
}

private const val LASER_TRAIL_KEEP_MS = 2_000L
private const val STICKY_NOTE_TEXT_BASE_SCALE = 0.6f

private fun findStickyNoteAt(world: Offset, stickyNotes: List<StickyNote>): Long? {
    return stickyNotes.lastOrNull { note ->
        world.x in note.x..(note.x + note.width) && world.y in note.y..(note.y + note.height)
    }?.id
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawActiveStroke(uiState: CanvasUiState) {
    val activeStroke = DrawStroke(
        pageIndex = if (uiState.mode == CanvasMode.WHITEBOARD) WHITEBOARD_PAGE_INDEX else uiState.currentPageIndex,
        tool = uiState.tool,
        color = if (uiState.tool == DrawingTool.LASER_POINTER) Color(0xFFFF3B30) else uiState.palette[uiState.selectedColorIndex],
        width = if (uiState.tool == DrawingTool.LASER_POINTER) 5f else resolveActiveWidth(uiState),
        points = uiState.activePoints
    )
    drawStroke(stroke = activeStroke, uiState = uiState)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLaserTrail(
    trail: LaserTrail,
    uiState: CanvasUiState,
    alpha: Float
) {
    if (trail.points.isEmpty()) return
    val stroke = DrawStroke(
        pageIndex = if (uiState.mode == CanvasMode.WHITEBOARD) WHITEBOARD_PAGE_INDEX else uiState.currentPageIndex,
        tool = DrawingTool.LASER_POINTER,
        color = Color(0xFFFF3B30),
        width = 5f,
        points = trail.points
    )
    drawStroke(stroke = stroke, uiState = uiState, alphaMultiplier = alpha)
}

private data class MutableFocus(val x: Float, val y: Float)

private fun canHandleDrawInput(event: MotionEvent, mode: CanvasInputMode): Boolean {
    val toolType = event.getToolType(0)
    return when (mode) {
        CanvasInputMode.PEN_ONLY -> {
            toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
        }

        CanvasInputMode.FINGER_ONLY -> toolType == MotionEvent.TOOL_TYPE_FINGER
    }
}

private fun calculateSpan(event: MotionEvent): Float {
    if (event.pointerCount < 2) return 0f
    val dx = event.getX(0) - event.getX(1)
    val dy = event.getY(0) - event.getY(1)
    return hypot(dx, dy)
}

private fun calculateFocus(event: MotionEvent): MutableFocus {
    val count = event.pointerCount
    var sumX = 0f
    var sumY = 0f
    for (index in 0 until count) {
        sumX += event.getX(index)
        sumY += event.getY(index)
    }
    return MutableFocus(sumX / count, sumY / count)
}

private fun toWorldPoint(screenX: Float, screenY: Float, uiState: CanvasUiState): Offset {
    val worldX = (screenX - uiState.offsetX) / uiState.scale
    val worldY = (screenY - uiState.offsetY) / uiState.scale
    return Offset(worldX, worldY)
}

private fun toScreenPoint(world: Offset, uiState: CanvasUiState): Offset {
    val screenX = (world.x * uiState.scale) + uiState.offsetX
    val screenY = (world.y * uiState.scale) + uiState.offsetY
    return Offset(screenX, screenY)
}

private fun resolveActiveWidth(uiState: CanvasUiState): Float {
    val averagePressure = uiState.activePoints.map { it.pressure }.average().toFloat().coerceIn(0.1f, 1.6f)
    return applyPressureCurve(uiState.baseStrokeWidth, averagePressure, uiState.sensitivity)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackgroundPattern(uiState: CanvasUiState) {
    when (uiState.backgroundStyle) {
        CanvasBackgroundStyle.GRID -> drawGrid(uiState)
        CanvasBackgroundStyle.RULED -> drawRuled(uiState)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(uiState: CanvasUiState) {
    val gridWorldStep = 64f
    val effectiveStep = (gridWorldStep * uiState.scale).coerceAtLeast(20f)

    val startX = ((-uiState.offsetX % effectiveStep) + effectiveStep) % effectiveStep
    var x = startX
    while (x <= size.width) {
        drawLine(
            color = Color(0xFFE6EBF2),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 8f))
        )
        x += effectiveStep
    }

    val startY = ((-uiState.offsetY % effectiveStep) + effectiveStep) % effectiveStep
    var y = startY
    while (y <= size.height) {
        drawLine(
            color = Color(0xFFE6EBF2),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 8f))
        )
        y += effectiveStep
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRuled(uiState: CanvasUiState) {
    val ruledWorldStep = 44.dp.toPx()
    val effectiveStep = (ruledWorldStep * uiState.scale).coerceAtLeast(8f)
    val startY = ((uiState.offsetY % effectiveStep) + effectiveStep) % effectiveStep

    var y = startY
    while (y <= size.height) {
        drawLine(
            color = Color(0xFFBFCBDE),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.2f
        )
        y += effectiveStep
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: DrawStroke,
    uiState: CanvasUiState,
    alphaMultiplier: Float = 1f
) {
    if (stroke.points.isEmpty()) return
    val drawColor = when (stroke.tool) {
        DrawingTool.ERASER -> uiState.backgroundColor
        DrawingTool.LASER_POINTER -> Color(0xFFFF3B30)
        DrawingTool.PEN -> stroke.color
        DrawingTool.MARKER -> stroke.color.copy(alpha = 0.35f)
        DrawingTool.TEXT -> stroke.color
        DrawingTool.READONLY -> stroke.color
    }.copy(alpha = alphaMultiplier)

    val strokeWidth = if (stroke.tool == DrawingTool.MARKER) {
        stroke.width * 1.8f
    } else {
        stroke.width
    }

    if (stroke.points.size == 1) {
        drawCircle(
            color = drawColor,
            radius = (strokeWidth / 2f) * uiState.scale,
            center = toScreenPoint(stroke.points.first().toOffset(), uiState)
        )
        return
    }

    val path = stroke.points.toPath(uiState)
    if (stroke.tool == DrawingTool.LASER_POINTER) {
        val w = strokeWidth * uiState.scale
        val a = alphaMultiplier
        // 外側ハロー（最も広く、極薄）
        drawPath(
            path = path,
            color = Color(0xFFFF3B30).copy(alpha = 0.07f * a),
            style = Stroke(width = w * 9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // 外グロー
        drawPath(
            path = path,
            color = Color(0xFFFF3B30).copy(alpha = 0.15f * a),
            style = Stroke(width = w * 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // 中グロー
        drawPath(
            path = path,
            color = Color(0xFFFF6B35).copy(alpha = 0.38f * a),
            style = Stroke(width = w * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // 内グロー
        drawPath(
            path = path,
            color = Color(0xFFFF3B30).copy(alpha = 0.70f * a),
            style = Stroke(width = w * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
    // コア線
    drawPath(
        path = path,
        color = drawColor,
        style = Stroke(
            width = strokeWidth * uiState.scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    // 白いハイライト中心（レーザーポインタのみ: 熱している感を演出）
    if (stroke.tool == DrawingTool.LASER_POINTER) {
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.80f * alphaMultiplier),
            style = Stroke(
                width = strokeWidth * uiState.scale * 0.35f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun List<StrokePoint>.toPath(uiState: CanvasUiState): Path {
    val path = Path()
    if (isEmpty()) return path
    val first = toScreenPoint(first().toOffset(), uiState)
    path.moveTo(first.x, first.y)
    for (index in 1 until size) {
        val point = toScreenPoint(this[index].toOffset(), uiState)
        path.lineTo(point.x, point.y)
    }
    return path
}

