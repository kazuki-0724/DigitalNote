package com.waju.factory.digitalnote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.waju.factory.digitalnote.data.local.entity.CanvasTextBoxEntity
import kotlinx.coroutines.flow.Flow

data class NoteSearchTextRow(
    val noteId: Int,
    val searchableText: String?
)

@Dao
interface CanvasTextBoxDao {
    @Query("SELECT * FROM text_boxes WHERE noteId = :noteId")
    suspend fun getByNoteId(noteId: Int): List<CanvasTextBoxEntity>

    @Query("DELETE FROM text_boxes WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Int)

    @Query("SELECT noteId, GROUP_CONCAT(text, ' ') AS searchableText FROM text_boxes GROUP BY noteId")
    fun observeSearchableText(): Flow<List<NoteSearchTextRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(textBoxes: List<CanvasTextBoxEntity>)

    @Transaction
    suspend fun replaceByNoteId(noteId: Int, textBoxes: List<CanvasTextBoxEntity>) {
        deleteByNoteId(noteId)
        if (textBoxes.isNotEmpty()) insertAll(textBoxes)
    }
}

