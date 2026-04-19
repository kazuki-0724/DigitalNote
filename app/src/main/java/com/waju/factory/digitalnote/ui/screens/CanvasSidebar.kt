package com.waju.factory.digitalnote.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.CanvasUiState
import com.waju.factory.digitalnote.ui.canvas.DrawStroke
import com.waju.factory.digitalnote.ui.canvas.DrawingTool
import com.waju.factory.digitalnote.ui.theme.TextSecondary

@Composable
internal fun CanvasSidebar(
    uiState: CanvasUiState,
    onAddPage: () -> Unit,
    onGoToPage: (Int) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(160.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ページ",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.mode == CanvasMode.PAGE) {
                    Text(
                        "${uiState.currentPageIndex + 1}/${uiState.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "サイドバーを閉じる",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (uiState.mode == CanvasMode.PAGE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onAddPage)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "ページ追加", modifier = Modifier.size(18.dp))
                        Text("ページ追加", style = MaterialTheme.typography.labelMedium)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((0 until uiState.totalPages).toList()) { pageIndex ->
                        PageThumbnail(
                            pageIndex = pageIndex,
                            strokes = uiState.strokes.filter { it.pageIndex == pageIndex },
                            selected = uiState.currentPageIndex == pageIndex,
                            onClick = { onGoToPage(pageIndex) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "無限キャンバスモード",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    pageIndex: Int,
    strokes: List<DrawStroke>,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, Color(0xFFD1D5DB))
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .width(100.dp)
                    .height(141.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
            ) {
                val lineCount = 8
                val lineStep = size.height / (lineCount + 1)
                for (i in 1..lineCount) {
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(8f, lineStep * i),
                        end = Offset(size.width - 8f, lineStep * i),
                        strokeWidth = 1f
                    )
                }
                drawLine(
                    color = Color(0xFFFFCDD2),
                    start = Offset(20f, 0f),
                    end = Offset(20f, size.height),
                    strokeWidth = 1.2f
                )

                val bounds = computeStrokeBounds(strokes)
                val contentWidth = (bounds.maxX - bounds.minX).coerceAtLeast(1f)
                val contentHeight = (bounds.maxY - bounds.minY).coerceAtLeast(1f)
                val scale = minOf(size.width / contentWidth, size.height / contentHeight) * 0.85f
                val offsetX = (size.width - contentWidth * scale) / 2f
                val offsetY = (size.height - contentHeight * scale) / 2f

                strokes.forEach { stroke ->
                    if (stroke.points.isEmpty()) return@forEach
                    val color = when (stroke.tool) {
                        DrawingTool.ERASER -> Color(0xFFF8FAFC)
                        DrawingTool.LASER_POINTER -> Color(0xFFFF3B30)
                        DrawingTool.PEN -> stroke.color
                    }

                    val path = Path()
                    val first = stroke.points.first()
                    path.moveTo(
                        ((first.x - bounds.minX) * scale) + offsetX,
                        ((first.y - bounds.minY) * scale) + offsetY
                    )
                    for (index in 1 until stroke.points.size) {
                        val point = stroke.points[index]
                        path.lineTo(
                            ((point.x - bounds.minX) * scale) + offsetX,
                            ((point.y - bounds.minY) * scale) + offsetY
                        )
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = (stroke.width * scale).coerceIn(1f, 4f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            Text(
                "P ${pageIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary
            )
        }
    }
}

private data class StrokeBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

private fun computeStrokeBounds(strokes: List<DrawStroke>): StrokeBounds {
    if (strokes.isEmpty()) {
        return StrokeBounds(0f, 0f, 800f, 1000f)
    }

    val points = strokes.flatMap { it.points }
    if (points.isEmpty()) {
        return StrokeBounds(0f, 0f, 800f, 1000f)
    }

    val minX = points.minOf { it.x }
    val minY = points.minOf { it.y }
    val maxX = points.maxOf { it.x }
    val maxY = points.maxOf { it.y }
    val padding = 40f
    return StrokeBounds(
        minX = minX - padding,
        minY = minY - padding,
        maxX = maxX + padding,
        maxY = maxY + padding
    )
}

