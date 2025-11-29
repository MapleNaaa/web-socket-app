package com.ahmedonibiyo.websocketapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ahmedonibiyo.websocketapp.model.ConnectionState
import com.ahmedonibiyo.websocketapp.model.WebSocketViewModel
import com.ahmedonibiyo.websocketapp.ui.components.MessageItem
import com.ahmedonibiyo.websocketapp.ui.theme.ConnectedGreen
import com.ahmedonibiyo.websocketapp.ui.theme.ConnectingOrange
import com.ahmedonibiyo.websocketapp.ui.theme.DisconnectedGray
import com.ahmedonibiyo.websocketapp.ui.theme.FailedRed
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: WebSocketViewModel,
    onOpenSettings: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val connectionState by viewModel.connectionState
    val messages = viewModel.messages

    // 新消息自动滚到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // 状态颜色
    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> ConnectedGreen
        ConnectionState.CONNECTING -> ConnectingOrange
        ConnectionState.FAILED -> FailedRed
        ConnectionState.DISCONNECTED -> DisconnectedGray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "ReChat",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusColor, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "已连接"
                                ConnectionState.CONNECTING -> "连接中..."
                                ConnectionState.FAILED -> "连接失败"
                                ConnectionState.DISCONNECTED -> "未连接"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("设置")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 消息区域：独立的大卡片，背景稍微有色，层级感强
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant//.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageItem(message = message)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 输入区域：单独浮在底部的卡片，和消息区明显分开
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("输入消息...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = connectionState == ConnectionState.CONNECTED,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    },
                    enabled = connectionState == ConnectionState.CONNECTED && messageText.isNotBlank(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text("Send")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
