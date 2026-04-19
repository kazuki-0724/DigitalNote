package com.waju.factory.digitalnote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.waju.factory.digitalnote.data.local.entity.StrokeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrokeDao {
    @Query("SELECT * FROM strokes WHERE noteId = :noteId ORDER BY pageIndex ASC, createdAt ASC")
    fun observeByNoteId(noteId: Int): Flow<List<StrokeEntity>>

    @Query("SELECT * FROM strokes WHERE noteId = :noteId ORDER BY pageIndex ASC, createdAt ASC")
    suspend fun getByNoteId(noteId: Int): List<StrokeEntity>

    @Query("DELETE FROM strokes WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(strokes: List<StrokeEntity>)

    @Transaction
    suspend fun replaceByNoteId(noteId: Int, strokes: List<StrokeEntity>) {
        deleteByNoteId(noteId)
        if (strokes.isNotEmpty()) {
            insertAll(strokes)
        }
    }
}

