package com.waju.factory.digitalnote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.waju.factory.digitalnote.data.local.entity.CanvasImageEntity
import kotlinx.coroutines.flow.Flow

data class NoteImageCountRow(
    val noteId: Int,
    val imageCount: Int
)

@Dao
interface CanvasImageDao {
    @Query("SELECT * FROM canvas_images WHERE noteId = :noteId")
    suspend fun getByNoteId(noteId: Int): List<CanvasImageEntity>

    @Query("DELETE FROM canvas_images WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Int)

    @Query("SELECT noteId, COUNT(*) AS imageCount FROM canvas_images GROUP BY noteId")
    fun observeImageCounts(): Flow<List<NoteImageCountRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<CanvasImageEntity>)

    @Transaction
    suspend fun replaceByNoteId(noteId: Int, images: List<CanvasImageEntity>) {
        deleteByNoteId(noteId)
        if (images.isNotEmpty()) {
            insertAll(images)
        }
    }
}

