package com.ahmedonibiyo.websocketapp.model

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.ahmedonibiyo.websocketapp.WebSocketService
import com.ahmedonibiyo.websocketapp.model.local.AppDatabase
import com.ahmedonibiyo.websocketapp.model.local.DatabaseProvider
import com.ahmedonibiyo.websocketapp.model.local.MessageEntity
import com.ahmedonibiyo.websocketapp.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


// Data class for messages

// Connection states
enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

// ViewModel to handle WebSocket logic
class WebSocketViewModel(application: Application) : AndroidViewModel(application) {

    var themeMode = mutableStateOf(ThemeMode.SYSTEM)
        private set

    // 记录当前 App 是否在前台
    private var isAppInForeground = mutableStateOf(true)

    fun setAppInForeground(isForeground: Boolean) {
        isAppInForeground.value = isForeground
    }

    // WebSocket server URL - using echo server for testing
    var host = mutableStateOf("10.0.2.2")
        private set

    var port = mutableStateOf("12345")
        private set

    var userName = mutableStateOf("You")
        private set

    var autoConnect = mutableStateOf(false)
        private set

    private val prefs = application.getSharedPreferences("ws_prefs", Context.MODE_PRIVATE)

    private val db: AppDatabase = DatabaseProvider.get(application)

    private val messageDao = db.messageDao()

    var messages = mutableStateListOf<Message>()
        private set

    var connectionState = mutableStateOf(ConnectionState.DISCONNECTED)
        private set

    private val stateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WebSocketService.ACTION_STATE) {
                val name = intent.getStringExtra(WebSocketService.EXTRA_STATE) ?: return
                Log.d("WS-VM", "onReceive state broadcast: $name")
                try {
                    val newState = ConnectionState.valueOf(name)
                    connectionState.value = newState
                } catch (e: Exception) {
                    Log.e("WS-VM", "Unknown state: $name", e)
                }
            }
        }
    }


    init {

        viewModelScope.launch(Dispatchers.IO) {
            messageDao.observeAllMessages().collect { list ->
                val uiList = list.map { it.toUiModel() }
                withContext(Dispatchers.Main) {
                    messages.clear()
                    messages.addAll(uiList)
                }
            }
        }

        // 读取本地保存的配置
        val savedHost = prefs.getString("host", "10.0.2.2") ?: "10.0.2.2"
        val savedPort = prefs.getString("port", "12345") ?: "12345"
        val savedAuto = prefs.getBoolean("auto_connect", false)
        val savedThemeModeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        val savedUserName = prefs.getString("user_name", "") ?: ""

        host.value = savedHost
        port.value = savedPort
        autoConnect.value = savedAuto
        userName.value = savedUserName

        themeMode.value = when (savedThemeModeName) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }


        val app = getApplication<Application>()
        val filter = IntentFilter(WebSocketService.ACTION_STATE)
        ContextCompat.registerReceiver(
            app,
            stateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

    }

    private fun setConnectionState(state: ConnectionState) {
        viewModelScope.launch(Dispatchers.Main) {
            connectionState.value = state
        }
    }

    private fun persistSettings() {
        prefs.edit()
            .putString("host", host.value)
            .putString("port", port.value)
            .putBoolean("auto_connect", autoConnect.value)
            .putString("theme_mode", themeMode.value.name)
            .putString("user_name", userName.value)
            .apply()
    }

    fun updateHost(newHost: String) {

        host.value = newHost
        persistSettings()
    }

    fun updatePort(newPort: String) {
        port.value = newPort
        persistSettings()
    }

    fun updateAutoConnect(enabled: Boolean) {
        autoConnect.value = enabled
        persistSettings()
    }


    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val displayName = userName.value.ifBlank { "You" }

        // 1. 先写一条“我发送的消息”到 DB
        viewModelScope.launch(Dispatchers.IO) {
            val entity = MessageEntity(
                sender = displayName,
                content = message,
                timestamp = System.currentTimeMillis(),
                isReceived = false,
                type = "chat",
                event = null,
                requestId = null
            )
            messageDao.insertMessage(entity)
        }

        // 2. 通过 Service 真正发给服务器
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, WebSocketService::class.java).apply {
            action = WebSocketService.ACTION_SEND
            putExtra(WebSocketService.EXTRA_MESSAGE, message)
            putExtra(WebSocketService.EXTRA_USER_NAME, displayName)
            // host/port 可传可不传，Service 内已有缓存
            putExtra(WebSocketService.EXTRA_HOST, host.value)
            putExtra(WebSocketService.EXTRA_PORT, port.value)
        }

        // ✅ 这里只需要普通 startService 就够了，禁止再用 startForegroundService
        ctx.startService(intent)
    }

    private fun addMessage(sender: String, content: String, isReceived: Boolean) {
        messages.add(Message(sender, content, System.currentTimeMillis(), isReceived))
    }


    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        try {
            app.unregisterReceiver(stateReceiver)
        } catch (_: Exception) {
        }
    }


    private fun Message.toEntity(): MessageEntity {
        return MessageEntity(
            sender = sender,
            content = content,
            timestamp = timestamp,
            isReceived = isReceived,
            type = type,
            event = event,
            requestId = requestId
        )
    }

    private fun MessageEntity.toUiModel():Message {
        return Message(
            sender = sender,
            content = content,
            timestamp = timestamp,
            isReceived = isReceived,
            type = type,
            event = event,
            requestId = requestId
        )
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            messageDao.clearAll()
            withContext(Dispatchers.Main) {
                messages.clear()
            }
        }
    }

    suspend fun buildExportText(): String = withContext(Dispatchers.IO) {
        val list = messageDao.getAllMessages()

        // 时间格式：2025-11-28 10:23:45 这种
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        if (list.isEmpty()) {
            sb.append("当前没有聊天记录。")
        } else {
            list.forEach { entity ->
                val timeStr = sdf.format(Date(entity.timestamp))
                sb.append("[")
                    .append(timeStr)
                    .append("] ")
                    .append(entity.sender)
                    .append(": ")
                    .append(entity.content)
                    .append('\n')
            }
        }

        sb.toString()
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        persistSettings()
    }

    fun updateUserName(newName: String) {
        userName.value = newName
        persistSettings()
    }


}