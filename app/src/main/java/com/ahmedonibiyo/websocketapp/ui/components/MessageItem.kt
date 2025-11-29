package com.ahmedonibiyo.websocketapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedonibiyo.websocketapp.model.Message
import com.ahmedonibiyo.websocketapp.ui.theme.ReceivedMessageBackground
import com.ahmedonibiyo.websocketapp.ui.theme.ReceivedMessageSender
import com.ahmedonibiyo.websocketapp.ui.theme.SentMessageBackground
import com.ahmedonibiyo.websocketapp.ui.theme.SentMessageSender
import com.ahmedonibiyo.websocketapp.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@Composable
fun MessageItem(message: Message) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    val isReceived = message.isReceived

    // 根据收/发消息选择一套颜色（从 MaterialTheme 取，而不是写死常量）
    val bubbleColor = if (isReceived) {
        MaterialTheme.colorScheme.surface   // 接收方：普通气泡，用 surface
    } else {
        MaterialTheme.colorScheme.primaryContainer // 自己发：突出一点，用 primaryContainer
    }

    val senderColor = if (isReceived) {
        MaterialTheme.colorScheme.primary       // 对方昵称：主色
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer // 自己昵称：反差强一点
    }

    val timeColor = if (isReceived) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isReceived) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = bubbleColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.sender,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = senderColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = timeColor
                )
            }
        }
    }
}
