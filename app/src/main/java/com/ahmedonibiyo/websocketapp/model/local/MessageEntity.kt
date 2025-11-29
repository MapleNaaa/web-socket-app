package com.ahmedonibiyo.websocketapp.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 数据库里的消息实体，对应 messages 表
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val sender: String,
    val content: String,
    val timestamp: Long,
    val isReceived: Boolean,
    val type: String?,
    val event: String?,
    val requestId: String?
)