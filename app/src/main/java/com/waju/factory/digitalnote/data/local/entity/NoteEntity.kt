package com.waju.factory.digitalnote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val excerpt: String,
    val content: String,
    val updatedLabel: String,
    val tagsCsv: String,
    val tonesCsv: String,
    val handwritten: Boolean,
    val starred: Boolean,
    val hasAttachment: Boolean,
    val paletteCsv: String,
    val selectedColorIndex: Int,
    val baseStrokeWidth: Float,
    val sensitivity: Float,
    val canvasMode: String,
    val backgroundStyle: String,
    val inputMode: String,
    val pageCount: Int,
    val currentPageIndex: Int,
    val canvasScale: Float,
    val canvasOffsetX: Float,
    val canvasOffsetY: Float
)

