package com.waju.factory.digitalnote.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.waju.factory.digitalnote.ui.canvas.CanvasUiState
import com.waju.factory.digitalnote.ui.canvas.DrawingTool

@Composable
internal fun CanvasTopBarSection(
    uiState: CanvasUiState,
    sidebarOpen: Boolean,
    onToolChanged: (DrawingTool) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onColorChanged: (Int) -> Unit,
    onOpenPalette: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleReadOnly: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarToggleButton(
                selected = uiState.tool == DrawingTool.PEN,
//                label = "ペン",
                icon = Icons.Outlined.Draw,
                onClick = { onToolChanged(DrawingTool.PEN) }
            )
            ToolbarToggleButton(
                selected = uiState.tool == DrawingTool.ERASER,
//                label = "消しゴム",
                icon = Icons.Outlined.AutoFixHigh,
                onClick = { onToolChanged(DrawingTool.ERASER) }
            )
            ToolbarToggleButton(
                selected = uiState.tool == DrawingTool.MARKER,
//                label = "マーカー",
                icon = Icons.Outlined.BorderColor,
                onClick = { onToolChanged(DrawingTool.MARKER) }
            )
            ToolbarToggleButton(
                selected = uiState.tool == DrawingTool.TEXT,
//                label = "付箋",
                icon = Icons.Outlined.TextFields,
                onClick = { onToolChanged(DrawingTool.TEXT) }
            )
            ToolbarToggleButton(
                selected = uiState.tool == DrawingTool.READONLY,
                icon = Icons.Outlined.EditOff,
                onClick = { onToolChanged(DrawingTool.READONLY) }
            )
            FilterChip(
                selected = uiState.tool == DrawingTool.LASER_POINTER,
                onClick = { onToolChanged(DrawingTool.LASER_POINTER) },
                label = {},
                leadingIcon = { Icon(Icons.Outlined.FilterCenterFocus, contentDescription = null) }
            )

            IconButton(onClick = onUndo) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo) {
                Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear")
            }

            uiState.palette.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(if (uiState.selectedColorIndex == index) 30.dp else 24.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (uiState.selectedColorIndex == index) 2.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { onColorChanged(index) }
                )
            }

            IconButton(onClick = onOpenPalette) {
                Icon(Icons.Outlined.Palette, contentDescription = "パレット")
            }
            // ページ送りボタン: PAGEモードかつ2ページ以上のときのみ表示
            if (uiState.mode == com.waju.factory.digitalnote.ui.canvas.CanvasMode.PAGE && uiState.totalPages >= 2) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = uiState.currentPageIndex > 0
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "前のページ")
                }
                Text(
                    text = "${uiState.currentPageIndex + 1}/${uiState.totalPages}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onNextPage,
                    enabled = uiState.currentPageIndex < uiState.totalPages - 1
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "次のページ")
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Tune, contentDescription = "設定")
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .size(width = 52.dp, height = 40.dp)
                .background(
                    color = if (sidebarOpen) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onToggleSidebar),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (sidebarOpen) Icons.Outlined.ChevronRight else Icons.AutoMirrored.Outlined.MenuOpen,
                contentDescription = if (sidebarOpen) "サイドバーを閉じる" else "ページ一覧を開く",
                tint = if (sidebarOpen) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun ToolbarToggleButton(
    selected: Boolean,
    // label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        tonalElevation = if (selected) 1.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
//            Text(
//                text = label,
//                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
//                style = MaterialTheme.typography.labelLarge
//            )
        }
    }
}

