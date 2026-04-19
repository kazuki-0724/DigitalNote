package com.waju.factory.digitalnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.waju.factory.digitalnote.data.repository.NoteRepository
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.CanvasSettings
import com.waju.factory.digitalnote.ui.canvas.CanvasUiState
import com.waju.factory.digitalnote.ui.canvas.DrawStroke
import com.waju.factory.digitalnote.ui.canvas.DrawingTool
import com.waju.factory.digitalnote.ui.canvas.StrokePoint
import com.waju.factory.digitalnote.ui.canvas.WHITEBOARD_PAGE_INDEX
import com.waju.factory.digitalnote.ui.canvas.applyPressureCurve
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CanvasViewModel(
    private val repository: NoteRepository,
    private val noteId: Int
) : ViewModel() {
    companion object {
        private const val MIN_SCALE = 0.2f
        private const val MAX_SCALE = 3.0f
        private const val TRANSFORM_SAVE_DEBOUNCE_MS = 200L
    }

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    private val redoStack = mutableListOf<DrawStroke>()
    private var pendingPersist: List<DrawStroke>? = null
    private var isPersisting = false
    private var pendingCanvasSettings: CanvasSettings? = null
    private var isPersistingCanvasSettings = false
    private var transformPersistJob: Job? = null

    init {
        loadCanvasSettings()
        loadInitialStrokes()
    }

    private fun loadCanvasSettings() {
        viewModelScope.launch {
            val settings = repository.getCanvasSettings(noteId) ?: return@launch
            _uiState.update { state ->
                val safePalette = settings.palette.ifEmpty { state.palette }
                val safePageCount = settings.totalPages.coerceAtLeast(1)
                state.copy(
                    mode = settings.mode,
                    backgroundStyle = settings.backgroundStyle,
                    inputMode = settings.inputMode,
                    totalPages = safePageCount,
                    currentPageIndex = settings.currentPageIndex.coerceIn(0, safePageCount - 1),
                    scale = settings.scale.coerceIn(MIN_SCALE, MAX_SCALE),
                    offsetX = settings.offsetX,
                    offsetY = settings.offsetY,
                    palette = safePalette,
                    selectedColorIndex = settings.selectedColorIndex.coerceIn(0, safePalette.lastIndex),
                    baseStrokeWidth = settings.baseStrokeWidth,
                    sensitivity = settings.sensitivity
                )
            }
        }
    }

    private fun loadInitialStrokes() {
        viewModelScope.launch {
            val loaded = repository.getStrokes(noteId)
            val maxPage = loaded
                .filter { it.pageIndex >= 0 }
                .maxOfOrNull { it.pageIndex }
                ?: 0
            _uiState.update {
                val totalPages = maxOf(it.totalPages, maxPage + 1)
                it.copy(
                    strokes = loaded,
                    activePoints = emptyList(),
                    totalPages = totalPages,
                    currentPageIndex = it.currentPageIndex.coerceIn(0, totalPages - 1)
                )
            }
            redoStack.clear()
        }
    }

    fun onToolChanged(tool: DrawingTool) {
        _uiState.update { it.copy(tool = tool) }
    }

    fun onModeChanged(mode: CanvasMode) {
        _uiState.update { it.copy(mode = mode, activePoints = emptyList()) }
        persistCanvasSettings()
    }

    fun onBackgroundStyleChanged(style: CanvasBackgroundStyle) {
        _uiState.update { it.copy(backgroundStyle = style) }
        persistCanvasSettings()
    }

    fun onInputModeChanged(mode: CanvasInputMode) {
        _uiState.update { it.copy(inputMode = mode, activePoints = emptyList()) }
        persistCanvasSettings()
    }

    fun goToPrevPage() {
        val previous = _uiState.value.currentPageIndex
        _uiState.update { state ->
            state.copy(currentPageIndex = (state.currentPageIndex - 1).coerceAtLeast(0), activePoints = emptyList())
        }
        if (_uiState.value.currentPageIndex != previous) {
            persistCanvasSettings()
        }
    }

    fun goToNextPage() {
        val previous = _uiState.value.currentPageIndex
        _uiState.update { state ->
            val next = (state.currentPageIndex + 1).coerceAtMost(state.totalPages - 1)
            state.copy(currentPageIndex = next, activePoints = emptyList())
        }
        if (_uiState.value.currentPageIndex != previous) {
            persistCanvasSettings()
        }
    }

    fun addPage() {
        _uiState.update { state ->
            val newPageCount = state.totalPages + 1
            state.copy(totalPages = newPageCount, currentPageIndex = newPageCount - 1, activePoints = emptyList())
        }
        persistCanvasSettings()
    }

    fun goToPage(index: Int) {
        val previous = _uiState.value.currentPageIndex
        _uiState.update { state ->
            state.copy(
                currentPageIndex = index.coerceIn(0, state.totalPages - 1),
                activePoints = emptyList()
            )
        }
        if (_uiState.value.currentPageIndex != previous) {
            persistCanvasSettings()
        }
    }

    fun onColorChanged(index: Int) {
        _uiState.update { state ->
            state.copy(selectedColorIndex = index.coerceIn(0, state.palette.lastIndex))
        }
        persistCanvasSettings()
    }

    fun onPaletteColorChanged(index: Int, color: Color) {
        _uiState.update { state ->
            if (index !in state.palette.indices) return@update state
            val updatedPalette = state.palette.toMutableList().apply {
                this[index] = color
            }
            state.copy(
                palette = updatedPalette,
                selectedColorIndex = index
            )
        }
        persistCanvasSettings()
    }

    fun onStrokeWidthChanged(width: Float) {
        _uiState.update { it.copy(baseStrokeWidth = width) }
        persistCanvasSettings()
    }

    fun onSensitivityChanged(sensitivity: Float) {
        _uiState.update { it.copy(sensitivity = sensitivity) }
        persistCanvasSettings()
    }

    fun onTransform(zoomChange: Float, panX: Float, panY: Float) {
        _uiState.update { state ->
            val nextScale = (state.scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
            val nextOffsetX = state.offsetX + panX
            val nextOffsetY = state.offsetY + panY
            state.copy(scale = nextScale, offsetX = nextOffsetX, offsetY = nextOffsetY)
        }
        scheduleTransformSettingsPersist()
    }

    fun resetTransform() {
        _uiState.update { it.copy(scale = 1f, offsetX = 0f, offsetY = 0f) }
        persistCanvasSettings()
    }

    /**
     * B5ページがキャンバス内に収まるよう、スケールとオフセットを設定する。
     * [scale], [offsetX], [offsetY] は CanvasScreen 側で計算済みの値を受け取る。
     */
    fun fitToPage(scale: Float, offsetX: Float, offsetY: Float) {
        _uiState.update {
            it.copy(
                scale = scale.coerceIn(MIN_SCALE, MAX_SCALE),
                offsetX = offsetX,
                offsetY = offsetY
            )
        }
        persistCanvasSettings()
    }

    fun startStroke(x: Float, y: Float, pressure: Float, timestamp: Long) {
        if (_uiState.value.tool != DrawingTool.LASER_POINTER) {
            redoStack.clear()
        }
        val point = StrokePoint(x = x, y = y, pressure = pressure, timestamp = timestamp)
        _uiState.update { it.copy(activePoints = listOf(point)) }
    }

    fun extendStroke(x: Float, y: Float, pressure: Float, timestamp: Long) {
        val point = StrokePoint(x = x, y = y, pressure = pressure, timestamp = timestamp)
        _uiState.update { it.copy(activePoints = it.activePoints + point) }
    }

    fun finishStroke() {
        val state = _uiState.value
        if (state.activePoints.isEmpty()) return

        if (state.tool == DrawingTool.LASER_POINTER) {
            _uiState.update { it.copy(activePoints = emptyList()) }
            return
        }

        val averagePressure = state.activePoints.map { it.pressure }.average().toFloat().coerceIn(0.1f, 1.6f)
        val resolvedWidth = applyPressureCurve(
            baseWidth = state.baseStrokeWidth,
            pressure = averagePressure,
            sensitivity = state.sensitivity
        )

        val targetPage = if (state.mode == CanvasMode.WHITEBOARD) {
            WHITEBOARD_PAGE_INDEX
        } else {
            state.currentPageIndex
        }

        val stroke = DrawStroke(
            pageIndex = targetPage,
            tool = state.tool,
            color = state.palette[state.selectedColorIndex],
            width = resolvedWidth,
            points = state.activePoints
        )

        val updatedStrokes = state.strokes + stroke
        _uiState.update { it.copy(strokes = updatedStrokes, activePoints = emptyList()) }
        persist(updatedStrokes)
    }

    fun cancelStroke() {
        _uiState.update { it.copy(activePoints = emptyList()) }
    }

    fun undo() {
        val state = _uiState.value
        val contextPage = currentContextPage(state)
        val targetIndex = state.strokes.indexOfLast { it.pageIndex == contextPage }
        if (targetIndex < 0) return

        val removed = state.strokes[targetIndex]
        redoStack.add(removed)
        val updated = state.strokes.toMutableList().also { it.removeAt(targetIndex) }
        _uiState.update { it.copy(strokes = updated, activePoints = emptyList()) }
        persist(updated)
    }

    fun redo() {
        val state = _uiState.value
        val contextPage = currentContextPage(state)
        val redoIndex = redoStack.indexOfLast { it.pageIndex == contextPage }
        if (redoIndex < 0) return

        val restored = redoStack.removeAt(redoIndex)
        val updated = state.strokes + restored
        _uiState.update { it.copy(strokes = updated) }
        persist(updated)
    }

    fun clear() {
        val state = _uiState.value
        val contextPage = currentContextPage(state)
        val removed = state.strokes.filter { it.pageIndex == contextPage }
        if (removed.isEmpty()) return

        redoStack.addAll(removed)
        val updated = state.strokes.filterNot { it.pageIndex == contextPage }
        _uiState.update { it.copy(strokes = updated, activePoints = emptyList()) }
        persist(updated)
    }

    private fun currentContextPage(state: CanvasUiState): Int {
        return if (state.mode == CanvasMode.WHITEBOARD) WHITEBOARD_PAGE_INDEX else state.currentPageIndex
    }

    private fun persistCanvasSettings() {
        val state = _uiState.value
        pendingCanvasSettings = CanvasSettings(
            mode = state.mode,
            backgroundStyle = state.backgroundStyle,
            inputMode = state.inputMode,
            totalPages = state.totalPages,
            currentPageIndex = state.currentPageIndex,
            palette = state.palette,
            selectedColorIndex = state.selectedColorIndex,
            baseStrokeWidth = state.baseStrokeWidth,
            sensitivity = state.sensitivity,
            scale = state.scale,
            offsetX = state.offsetX,
            offsetY = state.offsetY
        )
        if (isPersistingCanvasSettings) return

        viewModelScope.launch {
            isPersistingCanvasSettings = true
            while (isActive) {
                val next = pendingCanvasSettings ?: break
                pendingCanvasSettings = null
                repository.saveCanvasSettings(noteId = noteId, settings = next)
            }
            isPersistingCanvasSettings = false
        }
    }

    private fun scheduleTransformSettingsPersist() {
        transformPersistJob?.cancel()
        transformPersistJob = viewModelScope.launch {
            delay(TRANSFORM_SAVE_DEBOUNCE_MS)
            persistCanvasSettings()
        }
    }

    override fun onCleared() {
        transformPersistJob?.cancel()
        super.onCleared()
    }

    private fun persist(strokes: List<DrawStroke>) {
        pendingPersist = strokes
        if (isPersisting) return

        viewModelScope.launch {
            isPersisting = true
            while (isActive) {
                val next = pendingPersist ?: break
                pendingPersist = null
                repository.replaceStrokes(noteId = noteId, strokes = next)
            }
            isPersisting = false
        }
    }
}

