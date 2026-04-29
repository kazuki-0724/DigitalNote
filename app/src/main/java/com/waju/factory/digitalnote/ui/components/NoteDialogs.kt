package com.waju.factory.digitalnote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waju.factory.digitalnote.model.NoteType
import com.waju.factory.digitalnote.ui.theme.NoteCoverColors
import com.waju.factory.digitalnote.ui.theme.contentColorForCover

@Composable
fun NoteUpsertDialog(
    title: String,
    noteTitle: String,
    selectedCoverColorIndex: Int,
    onTitleChange: (String) -> Unit,
    onCoverColorChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Color) -> Unit,
    onDelete: (() -> Unit)? = null,
    confirmText: String,
    hintText: String = "ノート名"
) {
    val coverColor = NoteCoverColors.getOrElse(selectedCoverColorIndex) { NoteCoverColors.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    label = { Text(hintText) },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "カバー色",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NoteCoverColors.forEachIndexed { index, color ->
                            val selected = index == selectedCoverColorIndex
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onCoverColorChange(index) },
                                shape = CircleShape,
                                color = color,
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                if (selected) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = contentColorForCover(color),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = coverColor,
                        modifier = Modifier.width(140.dp)
                    ) {
                        Text(
                            text = "プレビュー",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = contentColorForCover(coverColor)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(noteTitle, coverColor) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        }
    )
}

// ── ノート種別選択ダイアログ（新規作成統合版） ────────────────────────────────

@Composable
fun NoteTypePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, coverColor: Color, noteType: NoteType) -> Unit
) {
    var noteTitle by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedType by remember { mutableStateOf(NoteType.CANVAS) }
    val coverColor = NoteCoverColors.getOrElse(selectedColorIndex) { NoteCoverColors.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新規ノートを作成") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ① ノート名
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    singleLine = true,
                    label = { Text("ノート名") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ② ノートの種別
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "種別",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == NoteType.CANVAS,
                            onClick = { selectedType = NoteType.CANVAS },
                            label = { Text("✏️ キャンバス", fontSize = 13.sp) }
                        )
//                        FilterChip(
//                            selected = selectedType == NoteType.EDITOR,
//                            onClick = { selectedType = NoteType.EDITOR },
//                            label = { Text("📝 テキスト", fontSize = 13.sp) }
//                        )
                        FilterChip(
                            selected = selectedType == NoteType.CHAT,
                            onClick = { selectedType = NoteType.CHAT },
                            label = { Text("💬 チャット", fontSize = 13.sp) }
                        )
                    }
                }

                // ③ カバー色
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "カバー色",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NoteCoverColors.forEachIndexed { index, color ->
                            val selected = index == selectedColorIndex
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { selectedColorIndex = index },
                                shape = CircleShape,
                                color = color,
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                if (selected) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = contentColorForCover(color),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = coverColor,
                        modifier = Modifier.width(140.dp)
                    ) {
                        Text(
                            text = "プレビュー",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = contentColorForCover(coverColor)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeTitle = noteTitle.trim().ifBlank { "新しいノート" }
                    onConfirm(safeTitle, coverColor, selectedType)
                }
            ) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

