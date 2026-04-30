package com.waju.factory.digitalnote.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waju.factory.digitalnote.data.local.entity.ChatMessageEntity
import com.waju.factory.digitalnote.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChatInputType { TEXT, MARKDOWN, HTML, IMAGE }

data class ChatMessage(
    val id: Long,
    val type: ChatInputType,
    val content: String,
    val localImagePath: String = "",
    val timestamp: Long,
    val replyToMessageId: Long? = null
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val inputType: ChatInputType = ChatInputType.TEXT,
    val editingMessageId: Long? = null,
    val replyingToMessageId: Long? = null,
    val replyingPreview: String? = null
)

class ChatViewModel(
    private val repository: NoteRepository,
    private val noteId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeChatMessages(noteId).collect { entities ->
                _uiState.update { state ->
                    state.copy(messages = entities.map { it.toDomain() })
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onInputTypeChanged(type: ChatInputType) {
        if (type == ChatInputType.IMAGE) return
        _uiState.update { it.copy(inputType = type) }
    }

    fun sendTextMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            val editingId = state.editingMessageId
            if (editingId != null) {
                repository.updateChatMessage(
                    id = editingId,
                    content = text,
                    type = state.inputType.name
                )
            } else {
                repository.sendChatMessage(
                    noteId = noteId,
                    type = state.inputType.name,
                    content = text,
                    replyToMessageId = state.replyingToMessageId
                )
            }
            _uiState.update {
                it.copy(
                    inputText = "",
                    editingMessageId = null,
                    replyingToMessageId = null,
                    replyingPreview = null
                )
            }
        }
    }

    fun sendImageMessage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = repository.importImageForChat(context, uri) ?: return@launch
            repository.sendChatMessage(
                noteId = noteId,
                type = ChatInputType.IMAGE.name,
                content = "",
                localImagePath = path
            )
        }
    }

    fun startEdit(message: ChatMessage) {
        if (message.type == ChatInputType.IMAGE) return
        _uiState.update {
            it.copy(
                inputText = message.content,
                inputType = message.type,
                editingMessageId = message.id,
                replyingToMessageId = null,
                replyingPreview = null
            )
        }
    }

    fun startReply(message: ChatMessage) {
        val previewSource = message.content.ifBlank { "画像" }
        _uiState.update {
            it.copy(
                editingMessageId = null,
                replyingToMessageId = message.id,
                replyingPreview = previewSource.replace("\n", " ").take(50)
            )
        }
    }

    fun cancelComposeMode() {
        _uiState.update {
            it.copy(
                editingMessageId = null,
                replyingToMessageId = null,
                replyingPreview = null,
                inputText = ""
            )
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteChatMessage(id)
        }
    }

    fun sendThreadReply(parentId: Long, type: ChatInputType, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(
                noteId = noteId,
                type = type.name,
                content = trimmed,
                replyToMessageId = parentId
            )
        }
    }

    fun sendThreadReplyImage(parentId: Long, context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = repository.importImageForChat(context, uri) ?: return@launch
            repository.sendChatMessage(
                noteId = noteId,
                type = ChatInputType.IMAGE.name,
                content = "",
                localImagePath = path,
                replyToMessageId = parentId
            )
        }
    }
}

private fun ChatMessageEntity.toDomain(): ChatMessage {
    val inputType = runCatching { ChatInputType.valueOf(type) }.getOrDefault(ChatInputType.TEXT)
    return ChatMessage(
        id = id,
        type = inputType,
        content = content,
        localImagePath = localImagePath,
        timestamp = timestamp,
        replyToMessageId = replyToMessageId
    )
}
