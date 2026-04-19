package com.waju.factory.digitalnote.model

import androidx.compose.ui.graphics.Color

data class NoteItem(
    val id: Int,
    val title: String,
    val excerpt: String,
    val content: String = "",
    val updatedLabel: String,
    val tags: List<String>,
    val tones: List<Color>,
    val coverColor: Color = Color(0xFFFFFFFF),
    val searchableText: String = "",
    val handwritten: Boolean = false,
    val starred: Boolean = false,
    val hasAttachment: Boolean = false
)

