package com.waju.factory.digitalnote.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

const val WHITEBOARD_PAGE_INDEX = -1
val CanvasPaperColor = Color(0xFFF8FAFC)
/** PAGEモード時のノート外領域（デスクトップ）の背景色 */
val CanvasDesktopColor = Color(0xFFDDE3EC)
val DefaultCanvasPalette = listOf(
    Color(0xFF111111),
    Color(0xFFE53935),
    Color(0xFFFFD600),
    Color(0xFF1E88E5)
)
val LegacyDefaultCanvasPalette = listOf(
    Color(0xFF1454FF),
    Color(0xFF6B6280),
    Color(0xFF1F2937),
    Color(0xFF0EA5E9)
)

enum class DrawingTool {
    PEN,
    ERASER,
    LASER_POINTER
}

enum class CanvasMode {
    PAGE,
    WHITEBOARD
}

enum class CanvasBackgroundStyle {
    GRID,
    RULED
}

enum class CanvasInputMode {
    PEN_ONLY,
    FINGER_ONLY
}

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long
) {
    fun toOffset(): Offset = Offset(x, y)
}

data class DrawStroke(
    val id: Long = 0L,
    val pageIndex: Int = 0,
    val tool: DrawingTool,
    val color: Color,
    val width: Float,
    val points: List<StrokePoint>
)

data class CanvasSettings(
    val mode: CanvasMode = CanvasMode.PAGE,
    val backgroundStyle: CanvasBackgroundStyle = CanvasBackgroundStyle.GRID,
    val inputMode: CanvasInputMode = CanvasInputMode.PEN_ONLY,
    val totalPages: Int = 1,
    val currentPageIndex: Int = 0,
    val palette: List<Color> = DefaultCanvasPalette,
    val selectedColorIndex: Int = 0,
    val baseStrokeWidth: Float = 8f,
    val sensitivity: Float = 0.85f,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class CanvasUiState(
    val strokes: List<DrawStroke> = emptyList(),
    val activePoints: List<StrokePoint> = emptyList(),
    val tool: DrawingTool = DrawingTool.PEN,
    val mode: CanvasMode = CanvasMode.PAGE,
    val backgroundStyle: CanvasBackgroundStyle = CanvasBackgroundStyle.GRID,
    val inputMode: CanvasInputMode = CanvasInputMode.PEN_ONLY,
    val totalPages: Int = 1,
    val currentPageIndex: Int = 0,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val backgroundColor: Color = CanvasPaperColor,
    val palette: List<Color> = DefaultCanvasPalette,
    val selectedColorIndex: Int = 0,
    val baseStrokeWidth: Float = 8f,
    val sensitivity: Float = 0.85f
)
