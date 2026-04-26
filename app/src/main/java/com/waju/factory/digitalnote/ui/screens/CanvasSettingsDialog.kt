package com.waju.factory.digitalnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waju.factory.digitalnote.ui.canvas.CanvasBackgroundStyle
import com.waju.factory.digitalnote.ui.canvas.CanvasInputMode
import com.waju.factory.digitalnote.ui.canvas.CanvasMode
import com.waju.factory.digitalnote.ui.canvas.CanvasUiState
import com.waju.factory.digitalnote.ui.theme.TextSecondary

@Composable
internal fun CanvasSettingsDialog(
    uiState: CanvasUiState,
    onDismiss: () -> Unit,
    onModeChanged: (CanvasMode) -> Unit,
    onBackgroundStyleChanged: (CanvasBackgroundStyle) -> Unit,
    onInputModeChanged: (CanvasInputMode) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onResetTransform: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("キャンバス設定") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("モード", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.mode == CanvasMode.PAGE,
                        onClick = { onModeChanged(CanvasMode.PAGE) },
                        label = { Text("ノート") }
                    )
                    FilterChip(
                        selected = uiState.mode == CanvasMode.WHITEBOARD,
                        onClick = { onModeChanged(CanvasMode.WHITEBOARD) },
                        label = { Text("無限") }
                    )
                }

                HorizontalDivider()

                Text("背景", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.backgroundStyle == CanvasBackgroundStyle.RULED,
                        onClick = { onBackgroundStyleChanged(CanvasBackgroundStyle.RULED) },
                        label = { Text("罫線") }
                    )
                    FilterChip(
                        selected = uiState.backgroundStyle == CanvasBackgroundStyle.GRID,
                        onClick = { onBackgroundStyleChanged(CanvasBackgroundStyle.GRID) },
                        label = { Text("マス目") }
                    )
                }

                HorizontalDivider()

                Text("入力モード", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.inputMode == CanvasInputMode.FINGER_ONLY,
                        onClick = { onInputModeChanged(CanvasInputMode.FINGER_ONLY) },
                        label = { Text("指") },
                        leadingIcon = { Icon(Icons.Outlined.TouchApp, contentDescription = null) }
                    )
                    FilterChip(
                        selected = uiState.inputMode == CanvasInputMode.PEN_ONLY,
                        onClick = { onInputModeChanged(CanvasInputMode.PEN_ONLY) },
                        label = { Text("ペン") },
                        leadingIcon = { Icon(Icons.Outlined.Draw, contentDescription = null) }
                    )
                }

                HorizontalDivider()

                Text("線の太さ: ${uiState.baseStrokeWidth.toInt()} px", color = TextSecondary)
                Slider(
                    value = uiState.baseStrokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 2f..24f
                )

                Text("筆圧感度: ${(uiState.sensitivity * 100).toInt()}%", color = TextSecondary)
                Slider(
                    value = uiState.sensitivity,
                    onValueChange = onSensitivityChanged,
                    valueRange = 0.3f..1.2f
                )

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "拡大率: ${(uiState.scale * 100).toInt()}%",
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        onResetTransform()
                        onDismiss()
                    }) {
                        Text("1:1 にリセット")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

