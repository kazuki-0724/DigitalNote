package com.waju.factory.digitalnote.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "canvas_images",
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
data class CanvasImageEntity(
    @PrimaryKey val id: Long,
    val noteId: Int,
    val pageIndex: Int,
    val localPath: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotationDeg: Float,
    val cropLeft: Float,
    val cropTop: Float,
    val cropRight: Float,
    val cropBottom: Float
)

