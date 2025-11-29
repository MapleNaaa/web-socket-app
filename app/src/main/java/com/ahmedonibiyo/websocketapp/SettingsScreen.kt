package com.ahmedonibiyo.websocketapp

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ahmedonibiyo.websocketapp.model.ConnectionState
import com.ahmedonibiyo.websocketapp.model.WebSocketViewModel
import com.ahmedonibiyo.websocketapp.ui.components.ConnectionStatusCard
import com.ahmedonibiyo.websocketapp.ui.theme.FailedRed
import com.ahmedonibiyo.websocketapp.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WebSocketViewModel,
    onBackToChat: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val host by viewModel.host
    val port by viewModel.port
    val autoConnect by viewModel.autoConnect
    val connectionState by viewModel.connectionState
    val themeMode by viewModel.themeMode
    val userName by viewModel.userName


    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "WebSocket 设置",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToChat) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回聊天"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 连接配置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "连接配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "配置 WebSocket 服务端地址和端口。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = host,
                        onValueChange = { viewModel.updateHost(it) },
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = port,
                        onValueChange = { viewModel.updatePort(it) },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { viewModel.updateUserName(it) },
                        label = { Text("用户昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "自动连接",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "启动应用时自动尝试连接 WebSocket。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoConnect,
                            onCheckedChange = { viewModel.updateAutoConnect(it) }
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "主题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "选择应用的外观风格。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 三个选项：跟随系统 / 浅色 / 深色
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ThemeOptionRow(
                            text = "跟随系统",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) }
                        )
                        ThemeOptionRow(
                            text = "浅色模式",
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) }
                        )
                        ThemeOptionRow(
                            text = "深色模式",
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.updateThemeMode(ThemeMode.DARK) }
                        )
                    }
                }
            }
            // 连接状态
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "连接状态",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "查看当前连接状态，手动连接或断开连接。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ConnectionStatusCard(
                        connectionState = connectionState,
                        onConnect = {
                            onStartService()
                        },
                        onDisconnect = {
                            onStopService()
                        },
                        showButtons = true
                    )

                }
            }

            // 聊天记录相关
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "聊天记录",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "导出或清空当前聊天记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 导出聊天记录为 txt 文件
                    Button(
                        onClick = {
                            scope.launch {
                                val exportText = viewModel.buildExportText()

                                val exportDir = File(context.cacheDir, "exports").apply {
                                    if (!exists()) mkdirs()
                                }

                                val fileName = "chat_history_${System.currentTimeMillis()}.txt"
                                val file = File(exportDir, fileName)

                                file.writeText(exportText, Charsets.UTF_8)

                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                val chooser = Intent.createChooser(sendIntent, "导出聊天记录为TXT")
                                context.startActivity(chooser)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "导出聊天记录"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出聊天记录（TXT）")
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearChatHistory() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FailedRed
                        ),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            width = 1.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "清空聊天记录",
                            tint = FailedRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清空聊天记录")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 底部返回按钮（可选，如果觉得顶部返回够用可以删掉这一块）
            TextButton(
                onClick = onBackToChat,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("返回聊天")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
private fun ThemeOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
