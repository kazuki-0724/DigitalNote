package com.waju.factory.digitalnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waju.factory.digitalnote.data.repository.NoteRepository
import com.waju.factory.digitalnote.model.NoteItem
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


    suspend fun createNote(): Int {
        return repository.createNote()
    }
}

