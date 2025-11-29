package com.ahmedonibiyo.websocketapp.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun observeAllMessages(): kotlinx.coroutines.flow.Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
