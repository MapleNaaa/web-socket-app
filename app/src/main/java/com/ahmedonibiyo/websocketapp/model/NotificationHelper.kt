package com.ahmedonibiyo.websocketapp.model

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import com.ahmedonibiyo.websocketapp.MainActivity
import com.ahmedonibiyo.websocketapp.R

object NotificationHelper {

    const val CHANNEL_ID = "chat_message_channel"
    private const val CHANNEL_NAME = "聊天消息"
    private const val CHANNEL_DESC = "后台收到聊天消息时通知"

    const val SUMMARY_CHANNEL_ID = "chat_summary_channel"
    private const val SUMMARY_CHANNEL_NAME = "聊天汇总"
    private const val SUMMARY_CHANNEL_DESC = "聊天消息汇总通知"

    private const val GROUP_KEY_CHAT = "group_chat_message"
    private const val SUMMARY_ID = 0

    // 在应用启动时调用一次，创建通知通道（Android 8+ 必须有通道）
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val msgChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }

            val summaryChannel = NotificationChannel(
                SUMMARY_CHANNEL_ID,
                SUMMARY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = SUMMARY_CHANNEL_DESC
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }

            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(summaryChannel)
        }
    }

    // 显示一条聊天通知
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMessageNotification(
        context: Context,
        sender: String,
        content: String
    ) {
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_avatar
        )

        // 点击通知回到聊天界面
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE
                    else 0
        )

        // 聊天样式
        val me = Person.Builder()
            .setName("你")
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(me)
            .addMessage(
                content,
                System.currentTimeMillis(),
                Person.Builder().setName(sender).build()
            )

        val notificationId = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)   // 换成你自己的图标
            .setStyle(messagingStyle)
            .setContentTitle(sender)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setGroup(GROUP_KEY_CHAT)
            .setLargeIcon(largeIcon)

        val manager = NotificationManagerCompat.from(context)

        // 单条通知
        manager.notify(notificationId, builder.build())

        // 汇总通知
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("聊天消息")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(GROUP_KEY_CHAT)
            .setGroupSummary(true)
            .setLargeIcon(largeIcon)
            .setSilent(true)
            .build()

        manager.notify(SUMMARY_ID, summaryNotification)
    }
}
