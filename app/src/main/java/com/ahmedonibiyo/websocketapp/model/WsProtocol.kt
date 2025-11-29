package com.ahmedonibiyo.websocketapp.model

import org.json.JSONObject
import java.util.UUID

/**
 * 统一的 WebSocket 消息封装/解析工具
 *
 * 协议结构：
 * {
 *   "type": "system | chat | rpc | error",
 *   "event": "<具体事件名>",
 *   "role": "<可选，角色，如 User01 或 bot 名称>",
 *   "payload": { ... },          // 具体数据
 *   "request_id": "字符串，用于关联请求/响应"
 * }
 */

/** 构造一个聊天消息 JSON 字符串（发送时用） */
fun buildChatJson(
    content: String,
    role: String = "User01",
    event: String = "message"
): String {
    val payload = JSONObject().apply {
        put("content", content)
        put("msg_id", (System.currentTimeMillis() - 1764359451000) / 1000).toString()
        put("sender", role)
    }

    val root = JSONObject().apply {
        put("type", "chat")
        put("event", event)
        put("payload", payload)
    }

    return root.toString()
}

/**
 * 解析服务器发来的消息
 * 返回：显示用的 sender（发信人） 和 content（显示内容）
 *  - 优先从 payload.content 读取
 *  - 解析失败时，直接把原始文本当成内容
 */
fun parseIncomingMessage(raw: String): Pair<String, String> {
    return try {
        val json = JSONObject(raw)

        val payload = json.optJSONObject("payload")
        val contentFromPayload = payload?.optString("content", null)

        val displayContent = when {
            contentFromPayload != null -> contentFromPayload
            else -> raw // 没有 payload.content 就直接显示原始字符串
        }

        val role = payload?.optString("sender", null)

        val sender = when {
            role != null -> role
            else -> "Server"
        }

        sender to displayContent
    } catch (e: Exception) {
        "Server" to raw
    }
}