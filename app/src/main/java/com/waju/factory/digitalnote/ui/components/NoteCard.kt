package com.waju.factory.digitalnote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waju.factory.digitalnote.model.NoteItem
import com.waju.factory.digitalnote.ui.theme.TextSecondary
import com.waju.factory.digitalnote.ui.theme.contentColorForCover

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            val coverTextColor = contentColorForCover(note.coverColor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(note.coverColor)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = coverTextColor
                    )
                    Text(
                        text = note.updatedLabel,
                        color = coverTextColor.copy(alpha = 0.82f),
                        fontSize = 12.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                if (note.hasAttachment) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(note.tones))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = note.excerpt,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.tags.take(3).forEach { tag ->
                        AssistChip(onClick = { }, label = { Text(tag, fontSize = 11.sp) })
                    }
                }
            }
        }
    }
}

