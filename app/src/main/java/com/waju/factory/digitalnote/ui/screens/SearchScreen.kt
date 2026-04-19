package com.waju.factory.digitalnote.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waju.factory.digitalnote.domain.filterNotes
import com.waju.factory.digitalnote.model.NoteItem
import com.waju.factory.digitalnote.ui.components.NoteCard
import com.waju.factory.digitalnote.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    notes: List<NoteItem>,
    modifier: Modifier = Modifier,
    onOpenNote: (NoteItem) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var handwrittenOnly by rememberSaveable { mutableStateOf(true) }
    var starredOnly by rememberSaveable { mutableStateOf(false) }
    var attachmentsOnly by rememberSaveable { mutableStateOf(false) }

    val filteredNotes = remember(query, handwrittenOnly, starredOnly, attachmentsOnly, notes) {
        filterNotes(
            notes = notes,
            query = query,
            handwrittenOnly = handwrittenOnly,
            starredOnly = starredOnly,
            attachmentsOnly = attachmentsOnly
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("ノート、タグ、内容を検索...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp)
            )
        }
        item {
            Text(
                text = "FILTERS",
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = true, onClick = { }, label = { Text("All Notes") })
                FilterChip(
                    selected = handwrittenOnly,
                    onClick = { handwrittenOnly = !handwrittenOnly },
                    label = { Text("Handwritten") }
                )
                FilterChip(
                    selected = starredOnly,
                    onClick = { starredOnly = !starredOnly },
                    label = { Text("Starred") }
                )
                FilterChip(
                    selected = attachmentsOnly,
                    onClick = { attachmentsOnly = !attachmentsOnly },
                    label = { Text("Attachments") }
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    query = ""
                    handwrittenOnly = false
                    starredOnly = false
                    attachmentsOnly = false
                }) {
                    Text("CLEAR ALL")
                }
            }
        }

        items(filteredNotes) { note ->
            NoteCard(
                note = note,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { onOpenNote(note) }
            )
        }
    }
}

