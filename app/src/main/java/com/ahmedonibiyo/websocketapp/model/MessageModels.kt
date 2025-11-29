package com.ahmedonibiyo.websocketapp.model

data class Message(
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isReceived: Boolean,
    val type: String? = null,        // system / chat / rpc / error
    val event: String? = null,       // 事件名
    val requestId: String? = null    // 请求/响应关联用
)