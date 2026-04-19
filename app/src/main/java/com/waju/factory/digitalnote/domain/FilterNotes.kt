package com.waju.factory.digitalnote.domain

import com.waju.factory.digitalnote.model.NoteItem

fun filterNotes(
    notes: List<NoteItem>,
    query: String,
    handwrittenOnly: Boolean,
    starredOnly: Boolean,
    attachmentsOnly: Boolean
): List<NoteItem> {
    return notes.filter { note ->
        val queryMatch = if (query.isBlank()) {
            true
        } else {
            val source = "${note.title} ${note.excerpt} ${note.tags.joinToString(" ")}".lowercase()
            source.contains(query.lowercase())
        }

        val handwrittenMatch = !handwrittenOnly || note.handwritten
        val starredMatch = !starredOnly || note.starred
        val attachmentMatch = !attachmentsOnly || note.hasAttachment

        queryMatch && handwrittenMatch && starredMatch && attachmentMatch
    }
}

