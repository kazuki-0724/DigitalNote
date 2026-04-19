package com.waju.factory.digitalnote.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasDesktopColor
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.CanvasUiState
import com.waju.factory.digitalnote.ui.canvas.DrawStroke
import com.waju.factory.digitalnote.ui.canvas.DrawingTool
import com.waju.factory.digitalnote.ui.canvas.StrokePoint
import com.waju.factory.digitalnote.ui.canvas.WHITEBOARD_PAGE_INDEX
import com.waju.factory.digitalnote.ui.canvas.applyPressureCurve
import kotlin.math.abs
import kotlin.math.hypot

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
    var twoFingerSlideAccumX by rememberSaveable { mutableStateOf(0f) }
    var twoFingerSlideAccumY by rememberSaveable { mutableStateOf(0f) }
    var pageSlideConsumed by rememberSaveable { mutableStateOf(false) }
    var sidebarOpen by rememberSaveable { mutableStateOf(false) }

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
    val safePaletteIndex = uiState.selectedColorIndex.coerceIn(0, uiState.palette.lastIndex.coerceAtLeast(0))
    val editingColor = uiState.palette.getOrElse(safePaletteIndex) { Color.Black }

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
                onToggleSidebar = { sidebarOpen = !sidebarOpen }
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
                            when (event.actionMasked) {
                                MotionEvent.ACTION_POINTER_DOWN -> {
                                    if (event.pointerCount >= 2) {
                                        inTransformGesture = true
                                        lastSpan = calculateSpan(event)
                                        val focus = calculateFocus(event)
                                        lastFocusX = focus.x
                                        lastFocusY = focus.y
                                        twoFingerSlideAccumX = 0f
                                        twoFingerSlideAccumY = 0f
                                        pageSlideConsumed = false
                                        onStrokeCancel()
                                    }
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    if (event.pointerCount >= 2) {
                                        val newSpan = calculateSpan(event)
                                        val focus = calculateFocus(event)
                                        val zoomChange = if (lastSpan > 0f) newSpan / lastSpan else 1f
                                        val panX = focus.x - lastFocusX
                                        val panY = focus.y - lastFocusY

                                        if (uiState.mode == CanvasMode.PAGE && !pageSlideConsumed) {
                                            twoFingerSlideAccumX += panX
                                            twoFingerSlideAccumY += panY

                                            val isLikelyPinch = abs(zoomChange - 1f) > 0.05f
                                            val slideThresholdPx = 100f
                                            val dominantIsHorizontal = abs(twoFingerSlideAccumX) >= abs(twoFingerSlideAccumY)
                                            val dominantSlide = if (dominantIsHorizontal) twoFingerSlideAccumX else twoFingerSlideAccumY

                                            if (!isLikelyPinch && abs(dominantSlide) >= slideThresholdPx) {
                                                if (dominantSlide > 0f) onPrevPage() else onNextPage()
                                                pageSlideConsumed = true
                                                twoFingerSlideAccumX = 0f
                                                twoFingerSlideAccumY = 0f
                                            }
                                        }

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
                                        twoFingerSlideAccumX = 0f
                                        twoFingerSlideAccumY = 0f
                                        pageSlideConsumed = false
                                    }
                                }

                                MotionEvent.ACTION_DOWN -> {
                                    if (!inTransformGesture && canHandleDrawInput(event, uiState.inputMode)) {
                                        val world = toWorldPoint(event.x, event.y, uiState)
                                        onStrokeStart(world.x, world.y, event.getPressure(0), event.eventTime)
                                    }
                                }

                                MotionEvent.ACTION_UP -> {
                                    inTransformGesture = false
                                    twoFingerSlideAccumX = 0f
                                    twoFingerSlideAccumY = 0f
                                    pageSlideConsumed = false
                                    onStrokeEnd()
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    inTransformGesture = false
                                    twoFingerSlideAccumX = 0f
                                    twoFingerSlideAccumY = 0f
                                    pageSlideConsumed = false
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
                                if (uiState.activePoints.isNotEmpty()) {
                                    val activeStroke = DrawStroke(
                                        pageIndex = uiState.currentPageIndex,
                                        tool  = uiState.tool,
                                        color = if (uiState.tool == DrawingTool.LASER_POINTER) Color(0xFFFF3B30)
                                                else uiState.palette[uiState.selectedColorIndex],
                                        width = if (uiState.tool == DrawingTool.LASER_POINTER) 5f
                                                else resolveActiveWidth(uiState),
                                        points = uiState.activePoints
                                    )
                                    drawStroke(stroke = activeStroke, uiState = uiState)
                                }
                            }
                        } else {
                            // ── WHITEBOARDモード: 既存の無限キャンバス動作 ──
                            drawBackgroundPattern(uiState)
                            visibleStrokes.forEach { stroke ->
                                drawStroke(stroke = stroke, uiState = uiState)
                            }
                            if (uiState.activePoints.isNotEmpty()) {
                                val activeStroke = DrawStroke(
                                    pageIndex = WHITEBOARD_PAGE_INDEX,
                                    tool  = uiState.tool,
                                    color = if (uiState.tool == DrawingTool.LASER_POINTER) Color(0xFFFF3B30)
                                            else uiState.palette[uiState.selectedColorIndex],
                                    width = if (uiState.tool == DrawingTool.LASER_POINTER) 5f
                                            else resolveActiveWidth(uiState),
                                    points = uiState.activePoints
                                )
                                drawStroke(stroke = activeStroke, uiState = uiState)
                            }
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
    uiState: CanvasUiState
) {
    if (stroke.points.isEmpty()) return
    val drawColor = when (stroke.tool) {
        DrawingTool.ERASER -> uiState.backgroundColor
        DrawingTool.LASER_POINTER -> Color(0xFFFF3B30)
        DrawingTool.PEN -> stroke.color
    }

    if (stroke.points.size == 1) {
        drawCircle(
            color = drawColor,
            radius = (stroke.width / 2f) * uiState.scale,
            center = toScreenPoint(stroke.points.first().toOffset(), uiState)
        )
        return
    }

    val path = stroke.points.toPath(uiState)
    drawPath(
        path = path,
        color = drawColor,
        style = Stroke(
            width = stroke.width * uiState.scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
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

