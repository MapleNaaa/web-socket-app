package com.ahmedonibiyo.websocketapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.ahmedonibiyo.websocketapp.model.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.getValue
import androidx.room.Room
import com.ahmedonibiyo.websocketapp.model.AppVisibility
import com.ahmedonibiyo.websocketapp.model.local.AppDatabase
import com.ahmedonibiyo.websocketapp.model.local.MessageEntity
import com.ahmedonibiyo.websocketapp.model.ConnectionState
import com.ahmedonibiyo.websocketapp.model.local.DatabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WebSocketService : Service() {

    companion object {
        const val SERVICE_CHANNEL_ID = "chat_connection_channel"
        const val SERVICE_NOTIFICATION_ID = 100

        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_USER_NAME = "extra_user_name"

        // 新增：给 Service 发指令的 action
        const val ACTION_SEND = "com.ahmedonibiyo.websocketapp.WS_SEND"

        const val EXTRA_MESSAGE = "extra_message"

        // 新增：Service → ViewModel 广播连接状态
        const val ACTION_STATE = "com.ahmedonibiyo.websocketapp.WS_STATE"
        const val EXTRA_STATE = "extra_state"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    private var host: String = "10.0.2.2"
    private var port: String = "12345"
    private var userName: String = "You"

    private val db by lazy { DatabaseProvider.get(applicationContext) }

    private val messageDao by lazy { db.messageDao() }


    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        // 1. 创建通知通道 + 启动前台服务
        createServiceChannel()
        startForeground(
            SERVICE_NOTIFICATION_ID,
            buildForegroundNotification("未连接")
        )

        // 2. 申请 CPU 唤醒锁，防止 WebSocket 被系统冷冻
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ReChat::WSLock"
        )
        wakeLock.acquire()

        // 3. 构建 OkHttpClient（pingInterval 对 OPPO/VIVO 机型无效）
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // 4. 启动 WebSocket 心跳（必须，不然网络会被 OPPO 冻结）
        startHeartbeat()
    }

    private fun startHeartbeat() = serviceScope.launch {
        while (true) {
            delay(15000)
            try {
                webSocket?.send("""{"type":"system", "event":"heartbeat", "request_id":"1024"}""")
            } catch (_: Exception) { }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 从 Activity 传进来的基本配置
        host = intent?.getStringExtra(EXTRA_HOST) ?: host
        port = intent?.getStringExtra(EXTRA_PORT) ?: port
        userName = intent?.getStringExtra(EXTRA_USER_NAME) ?: userName

        when (intent?.action) {
            ACTION_SEND -> {
                // 仅发送消息，不重新连
                val msg = intent.getStringExtra(EXTRA_MESSAGE)
                if (!msg.isNullOrBlank()) {
                    webSocket?.send(
                        com.ahmedonibiyo.websocketapp.model.buildChatJson(
                            content = msg,
                            role = userName
                        )
                    )
                }
            }
            else -> {
                // 默认：建立/重建连接
                connectWebSocket()
            }
        }

        return START_STICKY
    }

    private fun broadcastState(state: ConnectionState) {
        val intent = Intent(ACTION_STATE).apply {
            setPackage(packageName)                    // ✅ 限定只发给自己 App
            putExtra(EXTRA_STATE, state.name)
        }
        android.util.Log.d("WS-SERVICE", "broadcastState: $state")
        sendBroadcast(intent)
    }



    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Service destroyed")
        client?.dispatcher?.executorService?.shutdown()

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun createServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "聊天连接",
                NotificationManager.IMPORTANCE_LOW   // 前台服务用低重要级，不打扰用户
            ).apply {
                description = "保持 WebSocket 长连接的前台服务"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(status: String): Notification {
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("聊天服务")
            .setContentText(status)
            .setOngoing(true)
            .build()
    }

    private fun saveIncomingMessage(
        sender: String,
        content: String,
        type: String? = "chat",
        event: String? = null,
        requestId: String? = null
    ) {
        val entity = MessageEntity(
            sender = sender,
            content = content,
            timestamp = System.currentTimeMillis(),
            isReceived = true,
            type = type,
            event = event,
            requestId = requestId
        )
        serviceScope.launch {
            messageDao.insertMessage(entity)
        }
    }


    private fun connectWebSocket() {
        val url = "ws://$host:$port"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket?.cancel()
        broadcastState(ConnectionState.CONNECTING)

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 更新前台通知状态
                val n = buildForegroundNotification("已连接到 $host:$port")
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(SERVICE_NOTIFICATION_ID, n)

                broadcastState(ConnectionState.CONNECTED)
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 解析消息，然后用你之前的 NotificationHelper 发聊天通知
                try {
                    val json = JSONObject(text)
                    val payload = json.optJSONObject("payload")
                    val content = payload?.optString("content", text) ?: text
                    val sender = payload?.optString("sender", "Server") ?: "Server"

                    // 写入 DB
                    saveIncomingMessage(sender, content)

                    // 在后台收到消息时弹出聊天通知
                    if (!AppVisibility.isForeground) {
                        NotificationHelper.showMessageNotification(
                            context = applicationContext,
                            sender = sender,
                            content = content
                        )
                    }
                } catch (e: Exception) {
                    saveIncomingMessage("Server", text)
                    if (!AppVisibility.isForeground) {
                        NotificationHelper.showMessageNotification(
                            context = applicationContext,
                            sender = "Server",
                            content = text
                        )
                    }
                }
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val content = "收到二进制消息(${bytes.size} bytes)"
                saveIncomingMessage("Server", content, type = "chat", event = "binary")

                if (!AppVisibility.isForeground) {   // ✅ 同样只在后台弹
                    NotificationHelper.showMessageNotification(
                        context = applicationContext,
                        sender = "Server",
                        content = content
                    )
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                val n = buildForegroundNotification("连接关闭：$reason")
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(SERVICE_NOTIFICATION_ID, n)

                broadcastState(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val n = buildForegroundNotification("连接失败：${t.message}")
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(SERVICE_NOTIFICATION_ID, n)
                // 这里可以按需做自动重连

                broadcastState(ConnectionState.FAILED)

            }
        })
    }
}
