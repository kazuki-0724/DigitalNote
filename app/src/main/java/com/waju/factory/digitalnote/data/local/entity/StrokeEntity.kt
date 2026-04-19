package com.waju.factory.digitalnote.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "strokes",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class StrokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Int,
    val pageIndex: Int,
    val toolType: String,
    val colorArgb: Long,
    val width: Float,
    val pointsEncoded: String,
    val createdAt: Long
)

