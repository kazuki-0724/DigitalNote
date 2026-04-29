package com.waju.factory.digitalnote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waju.factory.digitalnote.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE chat_messages SET content = :content, type = :type WHERE id = :id")
    suspend fun updateMessage(id: Long, content: String, type: String)

    @Query("SELECT * FROM chat_messages WHERE noteId = :noteId ORDER BY timestamp ASC")
    fun observeByNoteId(noteId: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE noteId = :noteId ORDER BY timestamp ASC")
    suspend fun getByNoteId(noteId: Int): List<ChatMessageEntity>
}

