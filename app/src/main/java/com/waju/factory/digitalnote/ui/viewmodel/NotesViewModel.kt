package com.waju.factory.digitalnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.waju.factory.digitalnote.data.repository.NoteRepository
import com.waju.factory.digitalnote.model.NoteItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    val notes: StateFlow<List<NoteItem>> = repository.observeNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )


    suspend fun createNote(title: String, coverColor: Color): Int {
        return repository.createNote(title, coverColor)
    }

    fun updateNoteAppearance(noteId: Int, title: String, coverColor: Color) {
        viewModelScope.launch {
            repository.updateNoteAppearance(noteId = noteId, title = title, coverColor = coverColor)
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }
}

