package com.waju.factory.digitalnote.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId"), Index("replyToMessageId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Int,
    val type: String, // TEXT, IMAGE, MARKDOWN, HTML
    val content: String,
    val localImagePath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val replyToMessageId: Long? = null
)
