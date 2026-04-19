package com.waju.factory.digitalnote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waju.factory.digitalnote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val title: String = "",
    val content: String = ""
)

class EditorViewModel(
    private val repository: NoteRepository,
    private val noteId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeNote(noteId).collect { note ->
                if (note != null) {
                    _uiState.update {
                        it.copy(
                            title = note.title,
                            content = when {
                                note.content.isNotBlank() -> note.content
                                note.excerpt.isNotBlank() -> note.excerpt
                                else -> note.title
                            }
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onContentChanged(value: String) {
        _uiState.update { it.copy(content = value) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveEditorContent(
                noteId = noteId,
                title = state.title,
                content = state.content
            )
        }
    }
}

